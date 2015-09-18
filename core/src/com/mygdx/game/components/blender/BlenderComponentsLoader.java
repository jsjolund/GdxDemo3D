package com.mygdx.game.components.blender;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Json;
import com.mygdx.game.components.LightComponent;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.MotionStateComponent;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.systems.PhysicsSystem;
import com.mygdx.game.utilities.ModelFactory;

import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Created by user on 8/3/15.
 */
public class BlenderComponentsLoader {

	public static final String tag = "BlenderComponentsLoader";
	public final ArrayList<Entity> entities = new ArrayList<Entity>();
	public final Vector3 sunDirection = new Vector3();
	private AssetManager assets;

	public Vector3 gridTileHalfExt = new Vector3();
	public Vector3 gridOrigin = new Vector3();

	public BlenderComponentsLoader(AssetManager assets, String modelsJsonPath, String emptiesJsonPath, String
			lightsJsonPath) {
		this.assets = assets;

		MipMapGenerator.setUseHardwareMipMap(true);
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;

		ArrayList<BlenderModelComponent> models = loadModels(modelsJsonPath);
		for (BlenderModelComponent cmp : models) {
			cmp.model_file_name = String.format("models/g3db/%s.g3db", cmp.model_file_name);
//			Gdx.app.debug(tag, "Loading " + cmp.model_file_name);
			assets.load(cmp.model_file_name, Model.class, param);
		}

		ArrayList<BlenderEmptyComponent> empties = loadEmpties(emptiesJsonPath);
		ArrayList<BlenderLightComponent> lights = loadLights(lightsJsonPath);


		for (BlenderComponent cmp : empties) {
			blenderToGdxCoordinates(cmp);
			if (cmp.name.equals("grid_unit")) {
				gridTileHalfExt.set(cmp.scale);
				gridOrigin.set(cmp.position);
			}
		}
		for (BlenderModelComponent cmp : models) {
			blenderToGdxCoordinates(cmp);
			Entity entity = createModelEntity(cmp, empties);
			entities.add(entity);
		}
		for (BlenderLightComponent cmp : lights) {
			blenderToGdxCoordinates(cmp);
			Entity entity = createLightEntity(cmp);
			entities.add(entity);
		}
	}

	public static void blenderToGdxCoordinates(BlenderComponent cmp) {
		blenderToGdxCoordinates(cmp.position, cmp.rotation, cmp.scale);
	}

	private static void blenderToGdxCoordinates(Vector3... vs) {
		for (Vector3 v : vs) {
			blenderToGdxCoordinates(v);
		}
	}

	private static Vector3 blenderToGdxCoordinates(Vector3 v) {
		return v.set(v.x, v.z, -v.y);
	}

	private Entity createModelEntity(BlenderModelComponent cmp, ArrayList<BlenderEmptyComponent> empties) {

		Entity entity = new Entity();

		assets.finishLoadingAsset(cmp.model_file_name);
		Model model = assets.get(cmp.model_file_name, Model.class);
		entity.add(new ModelComponent(model, cmp.name, cmp.position, cmp.rotation, cmp.scale));
		ModelInstance instance = entity.getComponent(ModelComponent.class).modelInstance;

		btCollisionShape shape = loadCollisionShape(cmp.name, empties, model);

		if (shape == null) {
			// No shape defined. Load as static object.
//			Gdx.app.debug(tag, String.format("Created static object %s.", cmp.name));
			shape = Bullet.obtainStaticNodeShape(instance.nodes);

			PhysicsComponent phyCmp = new PhysicsComponent(
					shape, null, 0,
					PhysicsSystem.GROUND_FLAG,
					PhysicsSystem.ALL_FLAG,
					false, false);
			entity.add(phyCmp);
			phyCmp.body.setWorldTransform(instance.transform);

		} else {
			// Load as dynamic object with mass and motion state.
			float mass = loadMass(cmp, empties);
			Gdx.app.debug(tag, String.format("Created active model entity %s with %.2f mass.", cmp.name, mass));

			MotionStateComponent motionStateCmp = new MotionStateComponent(instance.transform);
			entity.add(motionStateCmp);
			entity.add(new PhysicsComponent(
					shape, motionStateCmp.motionState, mass,
					PhysicsSystem.OBJECT_FLAG,
					PhysicsSystem.ALL_FLAG,
					true, false));
		}
		return entity;
	}

	private Entity createLightEntity(BlenderLightComponent cmp) {
		Entity entity = null;

		if (cmp.type.equals("PointLamp")) {
			entity = new Entity();
			entity.add(new LightComponent(
					new PointLight().set(cmp.lamp_color.r, cmp.lamp_color.g, cmp.lamp_color.b,
							cmp.position.setZero(), cmp.lamp_energy * cmp.lamp_distance * 1000)));


		} else if (cmp.type.equals("SpotLamp")) {
			entity = new Entity();

			Vector3 direction = new Vector3(Vector3.Y).scl(-1);
			direction.rotate(Vector3.X, cmp.rotation.x);
			direction.rotate(Vector3.Z, cmp.rotation.z);
			direction.rotate(Vector3.Y, cmp.rotation.y);

			float intensity = cmp.lamp_energy;
			float cutoffAngle = cmp.lamp_falloff;
			float exponent = 1;

			entity.add(new LightComponent(
					new SpotLight().set(cmp.lamp_color, cmp.position,
							direction, intensity, cutoffAngle, exponent)));


		} else if (cmp.type.equals("SunLamp")) {

			entity = new Entity();

			Vector3 direction = new Vector3(Vector3.Y).scl(-1);
			direction.rotate(Vector3.X, cmp.rotation.x);
			direction.rotate(Vector3.Z, cmp.rotation.z);
			direction.rotate(Vector3.Y, cmp.rotation.y);

			float s = cmp.lamp_energy;
//			float s = 0.01f;
			entity.add(new LightComponent(
					new DirectionalLight().set(
							s * cmp.lamp_color.r,
							s * cmp.lamp_color.g,
							s * cmp.lamp_color.b,
							direction.x,
							direction.y,
							direction.z)));

			sunDirection.set(direction);
		}
		return entity;
	}


	private float loadMass(BlenderModelComponent model, ArrayList<BlenderEmptyComponent>
			empties) {
		for (BlenderEmptyComponent empty : empties) {
			if (empty.name.equals(model.name)
					&& (empty.custom_properties.containsKey("mass"))) {
				return Float.parseFloat(empty.custom_properties.get("mass"));
			}
		}
		return 0;
	}

	private btCollisionShape loadCollisionShape(String modelName, ArrayList<BlenderEmptyComponent>
			empties, Model convexHullModel) {
		btCollisionShape shape = null;
		for (BlenderEmptyComponent empty : empties) {

			if (empty.name.equals(modelName)
					&& (empty.custom_properties.containsKey("collision_shape"))) {

				String shapeType = empty.custom_properties.get("collision_shape");

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
					Mesh m = convexHullModel.meshes.get(0);
					FloatBuffer buf = ModelFactory.createBlenderToGdxFloatBuffer(m);
					shape = new btConvexHullShape(buf, m.getNumVertices(), m.getVertexSize());
				}
			}
		}
		return shape;
	}

	private ArrayList<BlenderModelComponent> loadModels(String path) {
		return (path == null) ? new ArrayList<BlenderModelComponent>() :
				new Json().fromJson(ArrayList.class, BlenderModelComponent.class, Gdx.files.local(path));
	}

	private ArrayList<BlenderEmptyComponent> loadEmpties(String path) {
		return (path == null) ? new ArrayList<BlenderEmptyComponent>() :
				new Json().fromJson(ArrayList.class, BlenderEmptyComponent.class, Gdx.files.local(path));
	}


	private ArrayList<BlenderLightComponent> loadLights(String path) {
		return (path == null) ? new ArrayList<BlenderLightComponent>() :
				new Json().fromJson(ArrayList.class, BlenderLightComponent.class, Gdx.files.local(path));
	}

}
