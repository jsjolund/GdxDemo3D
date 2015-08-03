//package com.mygdx.game;
//
//import com.badlogic.ashley.core.Entity;
//import com.badlogic.gdx.assets.AssetManager;
//import com.badlogic.gdx.math.Vector3;
//import com.badlogic.gdx.utils.Json;
//import com.mygdx.game.components.blender.BlenderComponent;
//import com.mygdx.game.components.blender.BlenderModelComponent;
//
//import java.util.ArrayList;
//
///**
// * Created by user on 7/31/15.
// */
//public class BlenderLoader {
//
//	public ArrayList<Entity> entities = new ArrayList<Entity>();
//	AssetManager assets;
//
//	public BlenderLoader(String[] paths) {
//
//		assets = new AssetManager();
//
//		ArrayList<BlenderComponent> cmps = loadJson(path);
//
//		for (BlenderComponent cmp : cmps) {
//			blenderToGdxCoords(cmp.position);
//			blenderToGdxCoords(cmp.rotation);
//			blenderToGdxCoords(cmp.scale);
//		}
//
//		loadModelAssets(cmps);
//
//		ArrayMap<String, String> modelRigidBody = loadRigidBodyDefs(cmps);
//
//		for (BlenderComponent cmp : cmps) {
//
//			if (cmp.type.equals("Mesh")) {
//				Entity entity = loadMeshObject(cmp, modelRigidBody);
//				entities.add(entity);
//
//			} else if (cmp.type.equals("NoneType")) {
//
//
//			} else if (cmp.type.equals("PointLamp") || cmp.type.equals("SpotLamp") || cmp.type.equals("SunLamp")) {
//				Entity entity = loadLamp(cmp);
//				entities.add(entity);
//			}
//		}
//	}
//
//	private static Vector3 blenderToGdxCoords(Vector3 v) {
//		return v.set(v.x, v.z, -v.y);
//	}
//
//	private ArrayList<BlenderComponent> loadJson(String path) {
//
//		return new Json().fromJson(ArrayList.class, BlenderComponent.class, Gdx.files.local(path));
//	}
//
//	private ArrayMap<String, String> loadRigidBodyDefs(ArrayList<BlenderComponent> cmps) {
//		ArrayMap<String, String> modelRigidBody = new ArrayMap<String, String>();
//		for (BlenderComponent cmp : cmps) {
//			if (cmp.type.equals("NoneType")) {
//				System.out.println("Checking " + cmp.name);
//				String[] nameParts = cmp.name.split(";");
//
//				for (String part : nameParts) {
//					if (part.startsWith("body")) {
//						String[] bodyPart = part.split("=");
//						modelRigidBody.put(nameParts[0], bodyPart[1]);
//					}
//				}
//			}
//		}
//		return modelRigidBody;
//	}
//
//	private void loadModelAssets(ArrayList<BlenderComponent> cmps) {
//		for (BlenderComponent cmp : cmps) {
//			if (cmp.type.equals("Mesh")) {
//				System.out.println("loading " + cmp.name);
//				cmp.modelFileName = String.format("models/%s.g3db", cmp.name);
//				assets.load(cmp.modelFileName, Model.class);
//			}
//		}
//	}
//
//	private void setInstanceTransform(ModelInstance instance, Vector3 location, Vector3 rotation, Vector3 scale) {
//		for (Node node : instance.nodes) {
//			node.scale.set(Math.abs(scale.x), Math.abs(scale.y), Math.abs(scale.z));
//		}
//		instance.transform.rotate(Vector3.Y, rotation.y);
//		instance.transform.rotate(Vector3.X, rotation.x);
//		instance.transform.rotate(Vector3.Z, rotation.z);
//		instance.transform.setTranslation(location);
//		instance.calculateTransforms();
//	}
//
//	private btCollisionShape loadCollisionShape(BlenderComponent cmp, ArrayMap<String, String> modelRigidBody) {
//		btCollisionShape shape = null;
//		if (modelRigidBody.containsKey(cmp.name)) {
//
//			String bodyType = modelRigidBody.get(cmp.name);
//
//			if (bodyType.equals("sphere")) {
//				float radius = Math.max(
//						Math.max(Math.abs(cmp.scale.x), Math.abs(cmp.scale.y)),
//						Math.abs(cmp.scale.z));
//				shape = new btSphereShape(radius);
//
//			} else if (bodyType.equals("capsule")) {
//				float radius = Math.max(Math.abs(cmp.scale.x), Math.abs(cmp.scale.z));
//				shape = new btCapsuleShape(radius, cmp.scale.y * 2.5f);
//			}
//		}
//		return shape;
//	}
//
//	private Entity loadMeshObject(BlenderComponent cmp, ArrayMap<String, String> modelRigidBody) {
//		Entity entity = new Entity();
//
//		assets.finishLoadingAsset(cmp.modelFileName);
//		Model model = assets.get(cmp.modelFileName, Model.class);
//		entity.add(new ModelComponent(model, cmp.name));
//
//		ModelInstance instance = entity.getComponent(ModelComponent.class).modelInstance;
//		setInstanceTransform(instance, cmp.position, cmp.rotation, cmp.scale);
//
//		btCollisionShape shape = loadCollisionShape(cmp, modelRigidBody);
//		boolean isActiveObject = true;
//
//		if (shape == null) {
//			shape = Bullet.obtainStaticNodeShape(model.nodes);
//			isActiveObject = false;
//		}
//
//		MotionStateComponent motionState = new MotionStateComponent();
//		motionState.transform = instance.transform;
//
//		entity.add(new TransformComponent(instance.transform));
//
//		if (isActiveObject) {
//			entity.add(new PhysicsComponent(
//					shape, motionState, 1,
//					PhysicsSystem.OBJECT_FLAG,
//					PhysicsSystem.ALL_FLAG,
//					true, false));
//
//		} else {
//			entity.add(new PhysicsComponent(
//					shape, motionState, 0,
//					PhysicsSystem.GROUND_FLAG,
//					PhysicsSystem.ALL_FLAG,
//					false, false));
//		}
//
//		return entity;
//	}
//
//	private Entity loadLamp(BlenderComponent cmp) {
//		Entity entity = null;
//		Matrix4 transform = new Matrix4();
//
//		if (cmp.type.equals("PointLamp")) {
//			entity = new Entity();
//
//			transform.translate(cmp.position);
//			entity.add(new TransformComponent(transform));
//			entity.add(new LightComponent(
//					new PointLight().set(cmp.lamp_color.r, cmp.lamp_color.g, cmp.lamp_color.b,
//							cmp.position, cmp.lamp_energy * cmp.lamp_distance / 2)));
//
//
//		} else if (cmp.type.equals("SpotLamp")) {
//			entity = new Entity();
//
//			Vector3 direction = new Vector3(Vector3.Y).scl(-1);
//			transform.rotate(Vector3.X, cmp.rotation.x);
//			transform.rotate(Vector3.Z, cmp.rotation.z);
//			direction.rot(transform);
//
//			transform.translate(cmp.position);
//
//			float intensity = cmp.lamp_energy;
//			float cutoffAngle = cmp.lamp_falloff;
//			float exponent = 1;
//
//			entity.add(new TransformComponent(transform));
//			entity.add(new LightComponent(
//					new SpotLight().set(cmp.lamp_color, cmp.position,
//							direction, intensity, cutoffAngle, exponent)));
//
//
//		} else if (cmp.type.equals("SunLamp")) {
//			entity = new Entity();
//
//			Vector3 direction = new Vector3(Vector3.Y).scl(-1);
//			transform.rotate(Vector3.X, -cmp.rotation.x);
//			transform.rotate(Vector3.Z, -cmp.rotation.z);
//			direction.rot(transform);
//
//			transform.translate(cmp.position);
//			entity.add(new TransformComponent(transform));
//			entity.add(new LightComponent(
//					new DirectionalLight().set(cmp.lamp_color.r, cmp.lamp_color.g,
//							cmp.lamp_color.b, direction.x, direction.y, direction.z)));
//		}
//
//		return entity;
//
//	}
//
//
//}