package com.mygdx.game.blender;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Json;
import com.mygdx.game.GameEngine;
import com.mygdx.game.objects.GameModel;
import com.mygdx.game.objects.GameModelBody;
import com.mygdx.game.objects.InvisibleBody;
import com.mygdx.game.pathfinding.NavMesh;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;

import java.nio.FloatBuffer;
import java.util.Comparator;

/**
 * Created by Johannes Sjolund on 10/19/15.
 */
public class BlenderScene {

	public static final String tag = "BlenderScene";

	public Array<BaseLight> lights = new Array<BaseLight>();
	public Array<GameModelBody> entities = new Array<GameModelBody>();
	public Array<InvisibleBody> ghosts = new Array<InvisibleBody>();
	public Vector3 shadowCameraDirection = new Vector3();
	public NavMesh navMesh;

	public BoundingBox worldBounds = new BoundingBox();
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
		Array<BlenderObject.BModel> blenderModels = deserializeModels(modelsJsonPath);
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

		// The export script uses Blender's coordinate system (z-up), so convert them.
		// This could also be done in the script of course
		blenderToGdxCoordinates(blenderModels);
		blenderToGdxCoordinates(blenderEmpties);
		blenderToGdxCoordinates(blenderLights);
		blenderToGdxCoordinates(blenderCameras);

		loadCollisionData(blenderEmpties);
		createLights(blenderLights);
		createGameObjects(blenderModels);

		// Not sure if multiple cameras will ever be supported
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

	public void dispose() {
		for (btCollisionShape shape : blenderDefinedShapesMap.values) {
			shape.dispose();
		}
		for (btCollisionShape shape : staticGeneratedShapesMap.values) {
			shape.dispose();
		}
		for (GameModelBody g : entities) {
			g.dispose();
		}
		for (InvisibleBody g : ghosts) {
			g.dispose();
		}
		modelAssets.dispose();
		navMesh.dispose();
	}

	public void setToSceneCamera(GhostCamera camera) {
		Vector3 direction = new Vector3(Vector3.Y).scl(-1);
		direction.rotate(Vector3.X, sceneCamera.rotation.x);
		direction.rotate(Vector3.Z, sceneCamera.rotation.z);
		direction.rotate(Vector3.Y, sceneCamera.rotation.y);
		camera.position.set(sceneCamera.position);
		camera.direction.set(direction);
		camera.fieldOfView = sceneCamera.fov;
		camera.update();
	}

	private void createGameObjects(Array<BlenderObject.BModel> models) {

		for (BlenderObject.BModel bModel : models) {

			GameModelBody entity;

			modelAssets.finishLoadingAsset(bModel.model_file_name);
			Model model = modelAssets.get(bModel.model_file_name, Model.class);

			btCollisionShape shape;
			float mass = 0;
			short belongsToFlag;
			short collidesWithFlag;
			boolean callback = false;
			boolean noDeactivate = false;

			// The navmesh should be treated differently than the rest of the models
			if (bModel.name.equals("navmesh")) {
				// We need to set the node transforms before calculating the navmesh shape
				GameModel gameModel = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
				ModelFactory.setBlenderToGdxFloatBuffer(gameModel.modelInstance.model.meshes.first());

				Array<NodePart> nodes = gameModel.modelInstance.model.getNode("navmesh").parts;
				// Sort the model meshParts array according to material name
				nodes.sort(new NavMeshNodeSorter());

				// The navmesh should be handled differently than other entities.
				// Its model should not be rendered.
				// Its vertices need to be rotated correctly for the shape to be oriented correctly.
				navMesh = new NavMesh(gameModel.modelInstance.model);
				shape = navMesh.getShape();
				staticGeneratedShapesMap.put("navmesh", shape);
				belongsToFlag = GameEngine.NAVMESH_FLAG;
				collidesWithFlag = GameEngine.NAVMESH_FLAG;

				InvisibleBody invisibleBody = new InvisibleBody(
						shape, mass, gameModel.modelInstance.transform, belongsToFlag, collidesWithFlag, callback, noDeactivate);
				worldBounds.set(gameModel.bounds);
				ghosts.add(invisibleBody);
				// Finished with the navmesh
				continue;
			}

			// Check if shape and mass is defined in the Blender scene. Otherwise let Bullet calculate a shape.
			if (blenderDefinedShapesMap.containsKey(bModel.name) && massMap.containsKey(bModel.name)) {
				// The model has a shape and mass predefined.
				shape = blenderDefinedShapesMap.get(bModel.name);
				mass = massMap.get(bModel.name);
				belongsToFlag = GameEngine.OBJECT_FLAG;
				collidesWithFlag = (short) (GameEngine.GROUND_FLAG
						| GameEngine.OBJECT_FLAG
						| GameEngine.PC_FLAG);

			} else {
				// Check if we have already calculated a shape previously
				if (staticGeneratedShapesMap.containsKey(bModel.name)) {
					shape = staticGeneratedShapesMap.get(bModel.name);
				} else {
					// We need to set the node transforms before calculating the static Bullet shape
					GameModel gameModel = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
					shape = Bullet.obtainStaticNodeShape(gameModel.modelInstance.nodes);
					staticGeneratedShapesMap.put(bModel.name, shape);
				}
				belongsToFlag = GameEngine.GROUND_FLAG;
				collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);
			}

			entity = new GameModelBody(model, bModel.name, bModel.position, bModel.rotation, bModel.scale,
					shape, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate);
			for (int i = 0; i < bModel.layers.length; i++) {
				if (bModel.layers[i]) {
					entity.layers.set(i);
				}
			}

			// TODO: Add constraint support in blender scene somehow
			if (bModel.name.startsWith("door")) {
				entity.constraints.add(new btHingeConstraint(entity.body, new Vector3(0, 0, -0.6f), Vector3.Y));
			}

			entities.add(entity);

			// TODO:
			// If models have zero mass (static terrain) and are on the same layer, perhaps merge them?
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

	private class NavMeshNodeSorter implements Comparator<NodePart> {
		@Override
		public int compare(NodePart a, NodePart b) {
			return a.material.id.compareTo(b.material.id);
		}
	}
}
