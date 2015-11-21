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

package com.mygdx.game.scene;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
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
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btHingeConstraint;
import com.badlogic.gdx.utils.*;
import com.mygdx.game.GameEngine;
import com.mygdx.game.blender.BlenderAssetManager;
import com.mygdx.game.blender.objects.BlenderCamera;
import com.mygdx.game.blender.objects.BlenderEmpty;
import com.mygdx.game.blender.objects.BlenderLight;
import com.mygdx.game.blender.objects.BlenderModel;
import com.mygdx.game.objects.*;
import com.mygdx.game.pathfinding.NavMesh;
import com.mygdx.game.utilities.GhostCamera;

import java.util.Comparator;

import static com.mygdx.game.utilities.Constants.V3_DOWN;

/**
 * @author jsjolund
 */
public class GameScene implements Disposable {

	/**
	 * For now, this is used to order the MeshParts of the navmesh in layers
	 */
	private class NavMeshNodeSorter implements Comparator<NodePart> {
		@Override
		public int compare(NodePart a, NodePart b) {
			return a.material.id.compareTo(b.material.id);
		}
	}

	public final static String tag = "GameScene";
	public final BlenderAssetManager assets;
	private final ArrayMap<String, Array<GameObject>> gameObjects = new ArrayMap<String, Array<GameObject>>();
	private final ObjectMap<String, GameObjectBlueprint> sharedBlueprints;
	public NavMesh navMesh;
	public Entity navmeshBody;
	public BoundingBox worldBounds = new BoundingBox();
	public Array<BaseLight<?>> lights = new Array<BaseLight<?>>();
	public Vector3 shadowCameraDirection = new Vector3(V3_DOWN);
	private BlenderCamera sceneCamera;

	public GameScene(String modelPath, String modelExt, ObjectMap<String, GameObjectBlueprint> sharedBlueprints) {
		this.sharedBlueprints = sharedBlueprints;
		this.assets = new BlenderAssetManager(modelPath, modelExt);
	}

	public void addGameObject(GameObject obj) {
		if (!gameObjects.containsKey(obj.name)) {
			gameObjects.put(obj.name, new Array<GameObject>());
		}
		gameObjects.get(obj.name).add(obj);
	}

	public void getGameModelById(String id, Array<GameObject> out) {
		out.addAll(gameObjects.get(id));
	}

	public void spawnGameObjectsFromPlaceholders() {
		Gdx.app.debug(tag, "Spawning predefined game objects.");
		// TODO: Clean this up
		Array<GameObjectBlueprint> blueprints = new Array<GameObjectBlueprint>();

		Array<String> modelsIdsInScene = assets.getPlaceholderIdsByType(BlenderModel.class);

		Array<BlenderEmpty> emptiesWithId = new Array<BlenderEmpty>();
		Array<BlenderModel> instancesWithId = new Array<BlenderModel>();
		Vector3 tmpScale = new Vector3();

		for (String id : modelsIdsInScene) {
			// TODO: handle this in a better way
			if (id.equals("navmesh")) {
				continue;
			}
			emptiesWithId.clear();
			instancesWithId.clear();

			Model model = assets.getAsset(id, Model.class);
			assets.getPlaceholders(id, BlenderModel.class, instancesWithId);
			assets.getPlaceholders(id, BlenderEmpty.class, emptiesWithId);

			boolean shapeIsDefined = emptiesWithId.size > 0;

			// Collision shape must be of correct size, but we do not want to create
			// new btCollisionShapes if we can reuse old ones.
			// Create an unscaled version of the shape to be shared if dimensions match.
			BlenderModel firstInstance = instancesWithId.first();
			tmpScale.set(firstInstance.scale);
			firstInstance.scale.set(1, 1, 1);
			GameObjectBlueprint refBp;
			boolean refUsed = false;
			refBp = (shapeIsDefined) ?
					new GameObjectBlueprint(firstInstance, model, emptiesWithId.first()) :
					new GameObjectBlueprint(firstInstance, model);
			firstInstance.scale.set(tmpScale);

			for (BlenderModel modelPlaceholder : instancesWithId) {
				// If model is not scaled, reuse the collision shape
				GameObjectBlueprint bp;
				if (modelPlaceholder.scale.epsilonEquals(1, 1, 1, 0.00001f)) {
					bp = new GameObjectBlueprint(modelPlaceholder, model,
							refBp.shape, refBp.mass, refBp.shapeType);
					refUsed = true;
				} else {
					bp = (shapeIsDefined) ?
							new GameObjectBlueprint(modelPlaceholder, model, emptiesWithId.first()) :
							new GameObjectBlueprint(modelPlaceholder, model);
				}
				blueprints.add(bp);
			}
			if (!refUsed) {
				refBp.dispose();
			}
		}

		Array<String> emptiesIdsInScene = assets.getPlaceholderIdsByType(BlenderEmpty.class);
		for (String id : modelsIdsInScene) {
			emptiesIdsInScene.removeValue(id, false);
		}
		for (String id : emptiesIdsInScene) {
			emptiesWithId.clear();
			assets.getPlaceholders(id, BlenderEmpty.class, emptiesWithId);
			for (BlenderEmpty blenderEmpty : emptiesWithId) {
				blueprints.add(new GameObjectBlueprint(blenderEmpty));
			}
		}

		Array<BlenderLight> sceneLights = new Array<BlenderLight>();
		assets.getAllPlaceholders(BlenderLight.class, sceneLights);
		for (BlenderLight light : sceneLights) {
			spawnLight(light);
		}

		Array<BlenderCamera> sceneCameras = new Array<BlenderCamera>();
		assets.getAllPlaceholders(BlenderCamera.class, sceneCameras);
		sceneCamera = sceneCameras.first();

		Array<BlenderModel> navmeshObjects = new Array<BlenderModel>();
		assets.getPlaceholders("navmesh", BlenderModel.class, navmeshObjects);
		GameObjectBlueprint navmeshBp = new GameObjectBlueprint();
		navmeshBp.name = "navmesh";
		navmeshBp.model = assets.getAsset("navmesh", Model.class);
		navmeshBp.position = navmeshObjects.first().position;
		navmeshBp.rotation = navmeshObjects.first().rotation;
		navmeshBp.scale = navmeshObjects.first().scale;
		setNavmesh(navmeshBp);

		for (GameObjectBlueprint blueprint : blueprints) {
			spawnFromBlueprint(blueprint);
		}
	}


	public HumanCharacter spawnHuman(String sharedBlueprintId, Vector3 initialPosition) {
		GameObjectBlueprint bp = sharedBlueprints.get(sharedBlueprintId);
		HumanCharacter obj = new HumanCharacter(
				bp.model, bp.name,
				initialPosition, bp.rotation, bp.scale,
				bp.shape, bp.mass,
				bp.belongsToFlag, bp.collidesWithFlag,
				bp.callback, bp.noDeactivate,
				bp.ragdollJson, bp.armatureNodeId);
		setSteerableData(obj);
		addGameObject(obj);
		return obj;
	}

	public DogCharacter spawnDog(String sharedBlueprintId, Vector3 initialPosition) {
		GameObjectBlueprint bp = sharedBlueprints.get(sharedBlueprintId);
		DogCharacter obj = new DogCharacter(
				bp.model, bp.name,
				initialPosition, bp.rotation, bp.scale,
				bp.shape, bp.mass,
				bp.belongsToFlag, bp.collidesWithFlag,
				bp.callback, bp.noDeactivate);
		setSteerableData(obj);
		addGameObject(obj);
		return obj;
	}

	private <T extends SteerableBody> void setSteerableData(T obj) {
		Ray posGroundRay = new Ray(obj.getPosition(), V3_DOWN);
		obj.currentTriangle = navMesh.rayTest(posGroundRay, 100, null);
		if (obj.currentTriangle == null) {
			throw new GdxRuntimeException(String.format("Failed to find navigation mesh position for %s", obj));
		}
		obj.visibleOnLayers.set(obj.currentTriangle.meshPartIndex);
	}

	public Billboard spawnSelectionBillboard(String sharedBlueprintId, Camera camera) {
		GameObjectBlueprint bp = sharedBlueprints.get(sharedBlueprintId);
		Billboard obj = new Billboard(bp.model, bp.name, camera, true, new Matrix4(), new Vector3());
		addGameObject(obj);
		return obj;
	}

	private void spawnFromBlueprint(GameObjectBlueprint bp) {
		if (bp.model != null && bp.shape != null) {
			GameModelBody obj = new GameModelBody(
					bp.model, bp.name,
					bp.position, bp.rotation, bp.scale,
					bp.shape, bp.mass,
					bp.belongsToFlag, bp.collidesWithFlag,
					bp.callback, bp.noDeactivate);
			obj.visibleOnLayers.clear();
			obj.visibleOnLayers.or(bp.visibleOnLayers);

			if (bp.name.startsWith("door")) {
				obj.constraints.add(new btHingeConstraint(obj.body, new Vector3(0, 0, -0.6f), Vector3.Y));
			}

			addGameObject(obj);

		} else if (bp.model == null && bp.shape != null) {
			InvisibleBody obj = new InvisibleBody(bp.name, bp.shape, bp.mass,
					bp.position, bp.rotation,
					bp.belongsToFlag, bp.collidesWithFlag,
					bp.callback, bp.noDeactivate);
			addGameObject(obj);

		} else if (bp.model != null) {
			GameModel obj = new GameModel(bp.model, bp.name, bp.position, bp.rotation, bp.scale);
			obj.visibleOnLayers.clear();
			obj.visibleOnLayers.or(bp.visibleOnLayers);
			addGameObject(obj);
		} else {
			throw new GdxRuntimeException(String.format("Could not read blueprint %s", bp));
		}
	}

	public void getGameObjects(Array<GameObject> out) {
		for (Array<GameObject> objs : gameObjects.values()) {
			out.addAll(objs);
		}
	}

	public void dispose() {
		navMesh.dispose();
		for (Array<GameObject> objs : gameObjects.values()) {
			for (GameObject obj : objs) {
				obj.dispose();
			}
		}
		gameObjects.clear();
		assets.dispose();
	}

	/**
	 * Creates and adds the navmesh to this scene.
	 */
	private void setNavmesh(GameObjectBlueprint bp) {
		// We need to set the node transforms before calculating the navmesh shape
		GameModel gameModel = new GameModel(bp.model, bp.name, bp.position, bp.rotation, bp.scale);

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

		navmeshBody = new InvisibleBody("navmesh",
				shape, 0, gameModel.modelInstance.transform, GameEngine.NAVMESH_FLAG, GameEngine.NAVMESH_FLAG, false, false);
		worldBounds.set(gameModel.boundingBox);
		gameModel.dispose();
	}


	private void spawnLight(BlenderLight bLight) {
		Vector3 direction = new Vector3(V3_DOWN);
		direction.rotate(Vector3.X, bLight.rotation.x);
		direction.rotate(Vector3.Z, bLight.rotation.z);
		direction.rotate(Vector3.Y, bLight.rotation.y);

		// TODO: Don't know how to map lamp intensity in blender to libgdx correctly
		float intensity = bLight.lamp_energy;
		float cutoffAngle = bLight.lamp_falloff;
		float exponent = 1;

		if (bLight.type.equals("PointLamp")) {
			BaseLight<?> light = new PointLight().set(
					bLight.lamp_color.r, bLight.lamp_color.g, bLight.lamp_color.b,
					bLight.position, bLight.lamp_energy);
			lights.add(light);

		} else if (bLight.type.equals("SpotLamp")) {
			BaseLight<?> light = new SpotLight().set(bLight.lamp_color, bLight.position,
					direction, intensity, cutoffAngle, exponent);
			lights.add(light);

		} else if (bLight.type.equals("SunLamp")) {
			BaseLight<?> light = new DirectionalLight().set(
					bLight.lamp_color.r,
					bLight.lamp_color.g,
					bLight.lamp_color.b,
					direction.x, direction.y, direction.z);
			lights.add(light);
			shadowCameraDirection.set(direction);
		}
	}


	public void setToSceneCamera(GhostCamera camera) {
		Vector3 direction = new Vector3(V3_DOWN);
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
