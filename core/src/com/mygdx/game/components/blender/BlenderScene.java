package com.mygdx.game.components.blender;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.MotionStateComponent;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.navmesh.NavMesh;
import com.mygdx.game.systems.PhysicsSystem;
import com.mygdx.game.utilities.ModelFactory;

import java.nio.FloatBuffer;


/**
 * Created by Johannes Sjolund on 10/2/15.
 */
public class BlenderScene implements Disposable {

	public Array<BaseLight> lights = new Array<BaseLight>();
	public Array<Entity> entities = new Array<Entity>();
	public Vector3 shadowCameraDirection = new Vector3();
	public NavMesh navMesh;
	public BoundingBox worldBox = new BoundingBox();
	private ArrayMap<String, btCollisionShape> blenderDefinedShapesMap = new ArrayMap<String, btCollisionShape>();
	private ArrayMap<String, btCollisionShape> staticGeneratedShapesMap = new ArrayMap<String, btCollisionShape>();
	private ArrayMap<String, Float> massMap = new ArrayMap<String, Float>();
	private ArrayMap<String, String> nameModelMap = new ArrayMap<String, String>();
	private AssetManager modelAssets = new AssetManager();
	private BlenderObject.BCamera sceneCamera;

	public BlenderScene(String modelsJsonPath,
						String emptiesJsonPath,
						String lightsJsonPath,
						String cameraJsonPath) {

		// Deserialize the list of model objects from the json and start loading them in asset manager.
		// Use automatic mipmap generation.
		Array<BlenderObject.BModel> blenderModels = deserializeModels(modelsJsonPath);
		MipMapGenerator.setUseHardwareMipMap(true);
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		for (BlenderObject.BModel obj : blenderModels) {
			obj.model_file_name = String.format("models/g3db/%s.g3db", obj.model_file_name);
			modelAssets.load(obj.model_file_name, Model.class, param);
			nameModelMap.put(obj.name, obj.model_file_name);
		}
		Array<BlenderObject.BEmpty> blenderEmpties = deserializeEmpties(emptiesJsonPath);
		Array<BlenderObject.BLight> blenderLights = deserializeLights(lightsJsonPath);
		Array<BlenderObject.BCamera> blenderCameras = deserializeCameras(cameraJsonPath);

		blenderToGdxCoordinates(blenderModels);
		blenderToGdxCoordinates(blenderEmpties);
		blenderToGdxCoordinates(blenderLights);
		blenderToGdxCoordinates(blenderCameras);

		loadCollisionData(blenderEmpties);
		createLights(blenderLights);
		createEntities(blenderModels);

		if (blenderCameras.size != 0) {
			sceneCamera = blenderCameras.get(0);
		}
	}

	private static void blenderToGdxCoordinates(Array<? extends BlenderObject> objs) {
		for (BlenderObject obj : objs) {
			blenderToGdxCoordinates(obj);
		}
	}

	public static void blenderToGdxCoordinates(BlenderObject obj) {
		blenderToGdxCoordinates(obj.position, obj.rotation, obj.scale);
	}

	private static void blenderToGdxCoordinates(Vector3... vectors) {
		for (Vector3 v : vectors) {
			blenderToGdxCoordinates(v);
		}
	}

	private static Vector3 blenderToGdxCoordinates(Vector3 vector) {
		return vector.set(vector.x, vector.z, -vector.y);
	}

	@Override
	public void dispose() {
		for (btCollisionShape shape : blenderDefinedShapesMap.values) {
			shape.dispose();
		}
		for (btCollisionShape shape : staticGeneratedShapesMap.values) {
			shape.dispose();
		}
		modelAssets.dispose();
		navMesh.dispose();
	}

	public void setToSceneCamera(PerspectiveCamera camera) {
		Vector3 direction = new Vector3(Vector3.Y).scl(-1);
		direction.rotate(Vector3.X, sceneCamera.rotation.x);
		direction.rotate(Vector3.Z, sceneCamera.rotation.z);
		direction.rotate(Vector3.Y, sceneCamera.rotation.y);
		camera.position.set(sceneCamera.position);
		camera.direction.set(direction);
		camera.fieldOfView = sceneCamera.fov;
		camera.update();
	}

	private void createEntities(Array<BlenderObject.BModel> models) {

		for (BlenderObject.BModel cmp : models) {

			Entity entity = new Entity();
			entities.add(entity);

			modelAssets.finishLoadingAsset(cmp.model_file_name);
			Model model = modelAssets.get(cmp.model_file_name, Model.class);
			ModelComponent mdlCmp = new ModelComponent(model, cmp.name,
					cmp.position, cmp.rotation, cmp.scale);
			ModelInstance instance = mdlCmp.modelInstance;

			if (cmp.name.equals("navmesh")) {
				// The navmesh should be handled differently than other entities.
				// Its vertices need to be rotated correctly for the shape to work.
				ModelFactory.setBlenderToGdxFloatBuffer(instance.model.meshes.first());
				btTriangleIndexVertexArray vertexArray =
						new btTriangleIndexVertexArray(instance.model.meshParts);
				btBvhTriangleMeshShape shape = new btBvhTriangleMeshShape(vertexArray, true);
				staticGeneratedShapesMap.put("navmesh", shape);
				PhysicsComponent phyCmp = new PhysicsComponent(
						shape, null, 0,
						PhysicsSystem.NAVMESH_FLAG,
						PhysicsSystem.NAVMESH_FLAG,
						false, false);
				entity.add(phyCmp);
				phyCmp.body.setWorldTransform(instance.transform);
				navMesh = new NavMesh(instance.model.meshes.first(), shape);
				continue;
			}

			entity.add(mdlCmp);

			if (blenderDefinedShapesMap.containsKey(cmp.name) && massMap.containsKey(cmp.name)) {
				// The model has a shape and mass predefined.
				btCollisionShape shape = blenderDefinedShapesMap.get(cmp.name);
				float mass = massMap.get(cmp.name);

				MotionStateComponent motionStateCmp = new MotionStateComponent(instance.transform);
				entity.add(motionStateCmp);
				entity.add(new PhysicsComponent(
						shape, motionStateCmp.motionState, mass,
						PhysicsSystem.OBJECT_FLAG,
						(short) (PhysicsSystem.GROUND_FLAG
								| PhysicsSystem.OBJECT_FLAG
								| PhysicsSystem.PC_FLAG),
						true, false));

			} else {
				btCollisionShape shape;
				if (staticGeneratedShapesMap.containsKey(cmp.name)) {
					shape = staticGeneratedShapesMap.get(cmp.name);
				} else {
					shape = Bullet.obtainStaticNodeShape(instance.nodes);
					staticGeneratedShapesMap.put(cmp.name, shape);
				}
				PhysicsComponent phyCmp = new PhysicsComponent(
						shape, null, 0,
						PhysicsSystem.GROUND_FLAG,
						(short) (PhysicsSystem.OBJECT_FLAG | PhysicsSystem.PC_FLAG),
						false, false);
				phyCmp.body.setWorldTransform(instance.transform);
				entity.add(phyCmp);
			}

//			mdlCmp.bounds.
		}
	}

	private void createLights(Array<BlenderObject.BLight> bLights) {

		for (BlenderObject.BLight cmp : bLights) {
			Vector3 direction = new Vector3(Vector3.Y).scl(-1);
			direction.rotate(Vector3.X, cmp.rotation.x);
			direction.rotate(Vector3.Z, cmp.rotation.z);
			direction.rotate(Vector3.Y, cmp.rotation.y);

			float intensity = cmp.lamp_energy;
			float cutoffAngle = cmp.lamp_falloff;
			float exponent = 1;

			if (cmp.type.equals("PointLamp")) {
				BaseLight light = new PointLight().set(
						cmp.lamp_color.r, cmp.lamp_color.g, cmp.lamp_color.b,
						cmp.position, cmp.lamp_energy);
				lights.add(light);

			} else if (cmp.type.equals("SpotLamp")) {
				BaseLight light = new SpotLight().set(cmp.lamp_color, cmp.position,
						direction, intensity, cutoffAngle, exponent);
				lights.add(light);

			} else if (cmp.type.equals("SunLamp")) {
				BaseLight light = new DirectionalLight().set(
						cmp.lamp_color.r,
						cmp.lamp_color.g,
						cmp.lamp_color.b,
						direction.x, direction.y, direction.z);
				lights.add(light);

				shadowCameraDirection.set(direction);
			}
		}

	}

	private void loadCollisionData(Array<BlenderObject.BEmpty> empties) {

		for (BlenderObject.BEmpty empty : empties) {
			if (empty.custom_properties.containsKey("mass")) {
				massMap.put(empty.name, Float.parseFloat(empty.custom_properties.get("mass")));
			}
			if (empty.custom_properties.containsKey("collision_shape")) {

				String shapeType = empty.custom_properties.get("collision_shape");
				btCollisionShape shape = null;

				if (shapeType.equals("capsule")) {
					float radius = Math.max(Math.abs(empty.scale.x), Math.abs(empty.scale.z));
					shape = new btCapsuleShape(radius, empty.scale.y);

				} else if (shapeType.equals("sphere")) {
					float radius = Math.max(
							Math.max(Math.abs(empty.scale.x), Math.abs(empty.scale.y)),
							Math.abs(empty.scale.z));
					shape = new btSphereShape(radius);

				} else if (shapeType.equals("box")) {
					Vector3 halfExtents = new Vector3(empty.scale);
					halfExtents.x = Math.abs(halfExtents.x);
					halfExtents.y = Math.abs(halfExtents.y);
					halfExtents.z = Math.abs(halfExtents.z);
					shape = new btBoxShape(halfExtents);

				} else if (shapeType.equals("convex_hull")) {
					// Since this depends on the model, load it first.
					String modelName = nameModelMap.get(empty.name);
					modelAssets.finishLoadingAsset(modelName);
					Model convexHullModel = modelAssets.get(modelName, Model.class);
					Mesh m = convexHullModel.meshes.get(0);
					FloatBuffer buf = ModelFactory.createBlenderToGdxFloatBuffer(m);
					shape = new btConvexHullShape(buf, m.getNumVertices(), m.getVertexSize());
				}
				blenderDefinedShapesMap.put(empty.name, shape);
			}
		}
	}

	private Array<BlenderObject.BModel> deserializeModels(String path) {
		return (path == null) ? new Array<BlenderObject.BModel>() :
				new Json().fromJson(Array.class, BlenderObject.BModel.class, Gdx.files.local(path));
	}

	private Array<BlenderObject.BEmpty> deserializeEmpties(String path) {
		return (path == null) ? new Array<BlenderObject.BEmpty>() :
				new Json().fromJson(Array.class, BlenderObject.BEmpty.class, Gdx.files.local(path));
	}

	private Array<BlenderObject.BLight> deserializeLights(String path) {
		return (path == null) ? new Array<BlenderObject.BLight>() :
				new Json().fromJson(Array.class, BlenderObject.BLight.class, Gdx.files.local(path));
	}

	private Array<BlenderObject.BCamera> deserializeCameras(String path) {
		return (path == null) ? new Array<BlenderObject.BCamera>() :
				new Json().fromJson(Array.class, BlenderObject.BCamera.class, Gdx.files.local(path));
	}


}
