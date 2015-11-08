/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.blender;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.utils.*;
import com.mygdx.game.GameEngine;
import com.mygdx.game.objects.*;
import com.mygdx.game.pathfinding.NavMesh;
import com.mygdx.game.utilities.GhostCamera;
import com.mygdx.game.utilities.ModelFactory;

import java.nio.FloatBuffer;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author jsjolund
 */
public class BlenderScene implements Disposable {

	/**
	 * Used internally to map a model to a collision shape. A shape can be reused between game objects.
	 */
	private class CollisionShapeDef {
		String shapeTypeName;
		float mass = 0;
		btCollisionShape shape;

		CollisionShapeDef() {
		}

		CollisionShapeDef(String shapeTypeName, float mass, btCollisionShape shape) {
			this.shapeTypeName = shapeTypeName;
			this.mass = mass;
			this.shape = shape;
		}

		/**
		 * Generate a collision shape from this model instance. If mass > 0, an optimized convex hull shape will be
		 * generated. Otherwise a static node shape will be used.
		 *
		 * @param shapeTypeName
		 * @param mass
		 * @param modelInstance
		 */
		CollisionShapeDef(String shapeTypeName, float mass, ModelInstance modelInstance) {
			this.shapeTypeName = shapeTypeName;
			this.mass = mass;
			if (mass > 0) {
				// The global node transform must be applied in order for the convex hull shape
				// to have correct orientation.
				Matrix4 transform = new Matrix4(modelInstance.nodes.get(0).globalTransform);
				Mesh mesh = modelInstance.model.meshes.get(0);
				FloatBuffer workBuffer = BufferUtils.newFloatBuffer(mesh.getVerticesBuffer().capacity());
				BufferUtils.copy(mesh.getVerticesBuffer(), workBuffer, mesh.getNumVertices() * mesh.getVertexSize() / 4);
				BufferUtils.transform(workBuffer, 3, mesh.getVertexSize(), mesh.getNumVertices(), transform);

				// First create a shape using all the vertices, then use the built in tool to reduce
				// the number of vertices to a manageable amount.
				btConvexShape convexShape = new btConvexHullShape(workBuffer, mesh.getNumVertices(), mesh.getVertexSize());
				btShapeHull hull = new btShapeHull(convexShape);
				hull.buildHull(convexShape.getMargin());
				shape = new btConvexHullShape(hull);
				convexShape.dispose();
				hull.dispose();

			} else {
				this.shape = Bullet.obtainStaticNodeShape(modelInstance.nodes);
			}
		}

		CollisionShapeDef(String shapeTypeName, ModelInstance modelInstance) {
			this.shapeTypeName = shapeTypeName;
			this.mass = 0;
			this.shape = Bullet.obtainStaticNodeShape(modelInstance.nodes);
		}

	}

	/**
	 * For now, this is used to order the MeshParts of the navmesh in layers
	 */
	private class NavMeshNodeSorter implements Comparator<NodePart> {
		@Override
		public int compare(NodePart a, NodePart b) {
			return a.material.id.compareTo(b.material.id);
		}
	}
	private final Array<GameObject> gameObjects = new Array<GameObject>();
	private final ArrayMap<String, Array<GameModel>> modelIdMap = new ArrayMap<String, Array<GameModel>>();
	private final AssetManager modelAssets = new AssetManager();
	private final ArrayMap<String, CollisionShapeDef> shapesMap = new ArrayMap<String, CollisionShapeDef>();
	private final ModelLoader.ModelParameters param;
	// Navmesh stuff
	public NavMesh navMesh;
	public Entity navmeshEntity;
	public BoundingBox worldBounds = new BoundingBox();
	public Array<BaseLight> lights = new Array<BaseLight>();
	public Vector3 shadowCameraDirection = new Vector3(Vector3.Y).scl(-1);
	private BlenderObject.BCamera sceneCamera;

	/**
	 * Loads a Blender scene from the JSON generated by the export script.
	 *
	 * @param modelsJsonPath
	 * @param emptiesJsonPath
	 * @param lightsJsonPath
	 * @param cameraJsonPath
	 */
	public BlenderScene(String modelsJsonPath,
						String emptiesJsonPath,
						String lightsJsonPath,
						String cameraJsonPath) {

		param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;

		Array<BlenderObject.BModel> blenderModels = deserializeModels(modelsJsonPath);
		for (BlenderObject.BModel obj : blenderModels) {
			obj.model_file_name = String.format("models/g3db/%s.g3db", obj.model_file_name);
			modelAssets.load(obj.model_file_name, Model.class, param);
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


		for (BlenderObject.BEmpty empty : blenderEmpties) {
			addEmpty(empty);
		}

		for (BlenderObject.BModel bModel : blenderModels) {
			if (navMesh == null && bModel.name.equals("navmesh")) {
				addNavmesh(bModel);
			} else {
				addModel(bModel);
			}
		}

		for (BlenderObject.BLight light : blenderLights) {
			addLight(light);
		}

		for (BlenderObject.BCamera camera : blenderCameras) {
			addCameras(camera);
		}
	}

	@SuppressWarnings("unchecked")
	private static Array<BlenderObject.BModel> deserializeModels(String path) {
		return (path == null) ? new Array<BlenderObject.BModel>() :
				new Json().fromJson(Array.class, BlenderObject.BModel.class, Gdx.files.local(path));
	}

	@SuppressWarnings("unchecked")
	private static Array<BlenderObject.BEmpty> deserializeEmpties(String path) {
		return (path == null) ? new Array<BlenderObject.BEmpty>() :
				new Json().fromJson(Array.class, BlenderObject.BEmpty.class, Gdx.files.local(path));
	}

	@SuppressWarnings("unchecked")
	private static Array<BlenderObject.BLight> deserializeLights(String path) {
		return (path == null) ? new Array<BlenderObject.BLight>() :
				new Json().fromJson(Array.class, BlenderObject.BLight.class, Gdx.files.local(path));
	}

	@SuppressWarnings("unchecked")
	private static Array<BlenderObject.BCamera> deserializeCameras(String path) {
		return (path == null) ? new Array<BlenderObject.BCamera>() :
				new Json().fromJson(Array.class, BlenderObject.BCamera.class, Gdx.files.local(path));
	}

	public static void blenderToGdxCoordinates(Array<? extends BlenderObject> objs) {
		for (BlenderObject obj : objs) {
			blenderToGdxCoordinates(obj);
		}
	}

	public static void blenderToGdxCoordinates(BlenderObject obj) {
		blenderToGdxCoordinates(obj.position, obj.rotation, obj.scale);
	}

	public static void blenderToGdxCoordinates(Vector3... vectors) {
		for (Vector3 v : vectors) {
			blenderToGdxCoordinates(v);
		}
	}

	public static Vector3 blenderToGdxCoordinates(Vector3 vector) {
		return vector.set(vector.x, vector.z, -vector.y);
	}

	public Iterator<GameObject> getGameObjects() {
		return gameObjects.iterator();
	}

	/**
	 * Get all game models which have this id.
	 *
	 * @param id  The id of the models.
	 * @param out Models will be added to this output array.
	 * @return The output array for chaining.
	 */
	public Array<GameModel> getGameModelById(String id, Array<GameModel> out) {
		if (modelIdMap.containsKey(id)) {
			out.addAll(modelIdMap.get(id));
		}
		return out;
	}

	/**
	 * Spawn a dog. If called after {@link GameEngine#setScene(BlenderScene)} the returned object
	 * must be added to the engine with {@link GameEngine#addEntity(Entity)}.
	 *
	 * @param initialPosition
	 * @return
	 */
	public DogCharacter spawnDog(Vector3 initialPosition) {
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		String modelFile = "models/g3db/dog_model.g3db";
		modelAssets.load(modelFile, Model.class, param);
		modelAssets.finishLoading();
		Model model = modelAssets.get(modelFile);

		btCollisionShape shape = new btCapsuleShape(0.4f, 0.5f);
		float mass = 1;
		boolean callback = false;
		boolean noDeactivate = true;
		short belongsToFlag = GameEngine.PC_FLAG;
		short collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);

		float scl = 0.3f;
		DogCharacter dog = new DogCharacter(model, "dog",
				initialPosition, new Vector3(0, 0, 0), new Vector3(scl, scl, scl),
				shape, mass, belongsToFlag, collidesWithFlag,
				callback, noDeactivate);

		Ray posGroundRay = new Ray(initialPosition, new Vector3(0, -1, 0));
		dog.currentTriangle = navMesh.rayTest(posGroundRay, 100, null);
		dog.layers.set(dog.currentTriangle.meshPartIndex);

		add(dog);

		return dog;
	}

	/**
	 * Spawn a human. If called after {@link GameEngine#setScene(BlenderScene)} the returned object
	 * must be added to the engine with {@link GameEngine#addEntity(Entity)}.
	 *
	 * @param initialPosition
	 * @return
	 */
	public HumanCharacter spawnHuman(Vector3 initialPosition) {

		short belongsToFlag = GameEngine.PC_FLAG;
		short collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.GROUND_FLAG);

		// Model
		ModelLoader.ModelParameters param = new ModelLoader.ModelParameters();
		param.textureParameter.genMipMaps = true;
		param.textureParameter.minFilter = Texture.TextureFilter.MipMap;
		param.textureParameter.magFilter = Texture.TextureFilter.Linear;
		String modelFile = "models/g3db/character_male_base.g3db";
		modelAssets.load(modelFile, Model.class, param);
		modelAssets.finishLoading();
		Model model = modelAssets.get(modelFile);
		btCollisionShape shape = new btCapsuleShape(0.4f, 1.1f);
		float mass = 1;
		boolean callback = false;
		boolean noDeactivate = true;
		String ragdollJson = "models/json/character_empty.json";
		String armatureNodeId = "armature";

		HumanCharacter human = new HumanCharacter(
				model, "character",
				initialPosition, new Vector3(0, 0, 0), new Vector3(1, 1, 1),
				shape, mass, belongsToFlag, collidesWithFlag,
				callback, noDeactivate, ragdollJson, armatureNodeId);
		Ray posGroundRay = new Ray(initialPosition, new Vector3(0, -1, 0));
		human.currentTriangle = navMesh.rayTest(posGroundRay, 100, null);
		human.layers.set(human.currentTriangle.meshPartIndex);

		add(human);

		return human;
	}

	/**
	 * Spawn a selection marker billboard. If called after {@link GameEngine#setScene(BlenderScene)}
	 * the returned object must be added to the engine with {@link GameEngine#addEntity(Entity)}.
	 *
	 * @param camera The camera which the billboard should face.
	 * @return
	 */
	public Billboard spawnSelectionBillboard(Camera camera) {
		// Selection billboard
		TextureLoader.TextureParameter param = new TextureLoader.TextureParameter();
		param.genMipMaps = true;
		param.minFilter = Texture.TextureFilter.MipMap;
		param.magFilter = Texture.TextureFilter.Linear;
		modelAssets.load("images/marker.png", Texture.class, param);
		modelAssets.finishLoading();
		Texture billboardPixmap = modelAssets.get("images/marker.png", Texture.class);
		// TODO: dispose or use an asset manager model
		Model billboardModel = ModelFactory.buildBillboardModel(billboardPixmap, 1, 1);
		Billboard markerBillboard = new Billboard(billboardModel, "marker",
				camera, true, new Matrix4(), new Vector3());

		add(markerBillboard);

		return markerBillboard;
	}

	/**
	 * Add object to scene. If called after {@link GameEngine#setScene(BlenderScene)}
	 * the object must be added to the engine with {@link GameEngine#addEntity(Entity)}.
	 *
	 * @param obj
	 */
	public void add(GameObject obj) {
		gameObjects.add(obj);
		if (obj instanceof GameModel) {
			GameModel gameModel = (GameModel) obj;
			if (!modelIdMap.containsKey(gameModel.id)) {
				modelIdMap.put(gameModel.id, new Array<GameModel>());
			}
			Array<GameModel> models = modelIdMap.get(gameModel.id);
			models.add(gameModel);
		}
	}

	public void dispose() {
		navMesh.dispose();
		modelAssets.dispose();
		modelIdMap.clear();
		for (GameObject obj : gameObjects) {
			obj.dispose();
		}
		for (CollisionShapeDef def : shapesMap.values()) {
			def.shape.dispose();
		}
	}

	/**
	 * Creates and adds the navmesh to this scene.
	 *
	 * @param bModel
	 */
	private void addNavmesh(BlenderObject.BModel bModel) {
		modelAssets.finishLoadingAsset(bModel.model_file_name);
		Model model = modelAssets.get(bModel.model_file_name, Model.class);

		// We need to set the node transforms before calculating the navmesh shape
		GameModel gameModel = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
		Array<NodePart> nodes = gameModel.modelInstance.model.getNode("navmesh").parts;
		// Sort the model meshParts array according to material name
		nodes.sort(new NavMeshNodeSorter());

		// The model transform must be applied to the meshparts for shape generation to work correctly.
		gameModel.modelInstance.calculateTransforms();
		Matrix4 transform = new Matrix4();
		for (Node node : gameModel.modelInstance.nodes) {
			transform.set(node.globalTransform).inv();
			for (NodePart nodePart : node.parts) {
				nodePart.meshPart.mesh.transform(transform);
			}
		}
		navMesh = new NavMesh(gameModel.modelInstance.model);
		btCollisionShape shape = navMesh.getShape();
		shapesMap.put("navmesh", new CollisionShapeDef(null, 0, shape));

		navmeshEntity = new InvisibleBody(
				shape, 0, gameModel.modelInstance.transform, GameEngine.NAVMESH_FLAG, GameEngine.NAVMESH_FLAG, false, false);
		worldBounds.set(gameModel.boundingBox);
	}

	/**
	 * Adds a model to the scene. A rigid body will be added/generated unless the shape map says not to.
	 *
	 * @param bModel
	 */
	private void addModel(BlenderObject.BModel bModel) {
		if (shapesMap.containsKey(bModel.name) && shapesMap.get(bModel.name).shapeTypeName.equals("none")) {
			addNoBodyModel(bModel);
		} else {
			addBodyModel(bModel);
		}
	}

	/**
	 * Creates and adds a model along with a rigid body to the scene. If a rigid body was defined and mapped to it
	 * either in the Blender JSON, or created and cached in an earlier call to this method, this rigid body will be
	 * used. Otherwise one will be generated.
	 *
	 * @param bModel
	 */
	private void addBodyModel(BlenderObject.BModel bModel) {
		modelAssets.finishLoadingAsset(bModel.model_file_name);
		Model model = modelAssets.get(bModel.model_file_name, Model.class);
		btCollisionShape shape;

		if (!shapesMap.containsKey(bModel.name)) {
			GameModel gameModel = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
			shapesMap.put(bModel.name, new CollisionShapeDef(bModel.name, gameModel.modelInstance));

		} else if (shapesMap.get(bModel.name).shape == null) {
			float mass = shapesMap.get(bModel.name).mass;
			String shapeName = shapesMap.get(bModel.name).shapeTypeName;
			GameModel gameModel = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
			shapesMap.put(bModel.name, new CollisionShapeDef(shapeName, mass, gameModel.modelInstance));
		}

		CollisionShapeDef def = shapesMap.get(bModel.name);
		short belongsToFlag;
		short collidesWithFlag;

		if (def.mass > 0) {
			belongsToFlag = GameEngine.OBJECT_FLAG;
			collidesWithFlag = (short) (GameEngine.GROUND_FLAG
					| GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);
		} else {
			belongsToFlag = GameEngine.GROUND_FLAG;
			collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);
		}


		GameModelBody entity = new GameModelBody(model, bModel.name, bModel.position, bModel.rotation, bModel.scale,
				def.shape, def.mass, belongsToFlag, collidesWithFlag, false, false);

		for (int i = 0; i < bModel.layers.length; i++) {
			if (bModel.layers[i]) {
				entity.layers.set(i);
			}
		}

		// TODO: Add constraint support in blender scene somehow
		if (bModel.name.startsWith("door")) {
			entity.constraints.add(new btHingeConstraint(entity.body, new Vector3(0, 0, -0.6f), Vector3.Y));
		}

		add(entity);
	}

	/**
	 * Add a model without a rigid body attached.
	 *
	 * @param bModel
	 */
	private void addNoBodyModel(BlenderObject.BModel bModel) {
		modelAssets.finishLoadingAsset(bModel.model_file_name);
		Model model = modelAssets.get(bModel.model_file_name, Model.class);
		GameModel entity = new GameModel(model, bModel.name, bModel.position, bModel.rotation, bModel.scale);
		add(entity);
		for (int i = 0; i < bModel.layers.length; i++) {
			if (bModel.layers[i]) {
				entity.layers.set(i);
			}
		}
	}

	/**
	 * Add a rigid body without a model attached.
	 *
	 * @param empty
	 */
	private void addInvisibleBody(BlenderObject.BEmpty empty) {
		CollisionShapeDef def = shapesMap.get(empty.name);

		short belongsToFlag = GameEngine.GROUND_FLAG;
		short collidesWithFlag = (short) (GameEngine.OBJECT_FLAG | GameEngine.PC_FLAG);

		InvisibleBody entity = new InvisibleBody(def.shape, def.mass, empty.position, empty.rotation,
				belongsToFlag, collidesWithFlag, false, false);

		add(entity);
	}

	/**
	 * Read a Blender empty object. Either a collision shape definition or an invisible body will be added.
	 *
	 * @param empty
	 */
	private void addEmpty(BlenderObject.BEmpty empty) {
		float mass = 0;
		String shapeType = null;
		if (empty.custom_properties.containsKey("mass")) {
			mass = Float.parseFloat(empty.custom_properties.get("mass"));
		}
		if (empty.custom_properties.containsKey("collision_shape")) {
			shapeType = empty.custom_properties.get("collision_shape");
		}
		addShapeDef(empty.name, shapeType, mass, empty.scale);

		if (empty.custom_properties.containsKey("invisible")) {
			addInvisibleBody(empty);
		}
	}

	/**
	 * Create and map a rigid body to a model object name.
	 *
	 * @param ownerName
	 * @param shapeType
	 * @param mass
	 * @param scale
	 * @return
	 */
	private btCollisionShape addShapeDef(String ownerName, String shapeType, float mass, Vector3 scale) {
		if (shapesMap.containsKey(ownerName)) {
			return shapesMap.get(ownerName).shape;
		}
		CollisionShapeDef shapeDef = new CollisionShapeDef();
		if (shapeType == null || shapeType.equals("convex_hull")) {

		} else if (shapeType.equals("capsule")) {
			float radius = Math.max(Math.abs(scale.x), Math.abs(scale.z));
			shapeDef.shape = new btCapsuleShape(radius, scale.y);

		} else if (shapeType.equals("sphere")) {
			float radius = Math.max(
					Math.max(Math.abs(scale.x), Math.abs(scale.y)),
					Math.abs(scale.z));
			shapeDef.shape = new btSphereShape(radius);

		} else if (shapeType.equals("box")) {
			Vector3 halfExtents = new Vector3(scale);
			halfExtents.x = Math.abs(halfExtents.x);
			halfExtents.y = Math.abs(halfExtents.y);
			halfExtents.z = Math.abs(halfExtents.z);
			shapeDef.shape = new btBoxShape(halfExtents);
		}

		shapeDef.shapeTypeName = shapeType;
		shapeDef.mass = mass;
		shapesMap.put(ownerName, shapeDef);
		return shapeDef.shape;
	}

	public void addLight(BlenderObject.BLight bLight) {
		Vector3 direction = new Vector3(Vector3.Y).scl(-1);
		direction.rotate(Vector3.X, bLight.rotation.x);
		direction.rotate(Vector3.Z, bLight.rotation.z);
		direction.rotate(Vector3.Y, bLight.rotation.y);

		// TODO: Don't know how to map lamp intensity in blender to libgdx correctly
		float intensity = bLight.lamp_energy;
		float cutoffAngle = bLight.lamp_falloff;
		float exponent = 1;

		if (bLight.type.equals("PointLamp")) {
			BaseLight light = new PointLight().set(
					bLight.lamp_color.r, bLight.lamp_color.g, bLight.lamp_color.b,
					bLight.position, bLight.lamp_energy);
			lights.add(light);

		} else if (bLight.type.equals("SpotLamp")) {
			BaseLight light = new SpotLight().set(bLight.lamp_color, bLight.position,
					direction, intensity, cutoffAngle, exponent);
			lights.add(light);

		} else if (bLight.type.equals("SunLamp")) {
			BaseLight light = new DirectionalLight().set(
					bLight.lamp_color.r,
					bLight.lamp_color.g,
					bLight.lamp_color.b,
					direction.x, direction.y, direction.z);
			lights.add(light);

			shadowCameraDirection.set(direction);
		}
	}

	public void addCameras(BlenderObject.BCamera bCamera) {
		// TODO: Not sure if multiple cameras will ever be supported
		sceneCamera = bCamera;
	}

	public void setToSceneCamera(GhostCamera camera) {
		Vector3 direction = new Vector3(Vector3.Y).scl(-1);
		direction.rotate(Vector3.X, sceneCamera.rotation.x);
		direction.rotate(Vector3.Z, sceneCamera.rotation.z);
		direction.rotate(Vector3.Y, sceneCamera.rotation.y);
		direction.nor();

		camera.fieldOfView = sceneCamera.fov;
		camera.targetPosition.set(sceneCamera.position);
		camera.targetDirection.set(direction);
		camera.targetUp.set(Vector3.Y);
		camera.snapToTarget();
		camera.update();
	}
}
