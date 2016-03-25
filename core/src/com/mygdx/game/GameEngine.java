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

package com.mygdx.game;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import com.mygdx.game.objects.*;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.scene.GameScene;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.Engine;
import com.mygdx.game.utilities.Entity;
import com.mygdx.game.utilities.Observer;

/**
 * Class which keeps track of game objects, performs physics simulation and collision detection,
 * as well as decides which models to render.
 *
 * @author jsjolund
 */
public class GameEngine extends Engine implements Disposable, Observer {

	/**
	 * A ClosestRayResultCallback which takes object layers into account (e.g. house floors)
	 */
	private class LayeredClosestRayResultCallback extends ClosestRayResultCallback {
		private final Vector3 rayFrom = new Vector3();
		private final Vector3 rayTo = new Vector3();
		private final Vector3 tmp = new Vector3();
		private final Ray ray = new Ray();
		private Bits layers;
		private float hitFraction = 1;
		private float rayDistance = 0;

		public LayeredClosestRayResultCallback(Vector3 rayFromWorld, Vector3 rayToWorld) {
			super(rayFromWorld, rayToWorld);
		}

		@Override
		public void setClosestHitFraction(float value) {
			super.setClosestHitFraction(value);
			this.hitFraction = value;
		}

		public void setRay(Ray ray, float rayDistance) {
			this.ray.set(ray);
			this.rayDistance = rayDistance;
			rayFrom.set(ray.origin);
			rayTo.set(ray.direction).scl(rayDistance).add(rayFrom);
			setRayFromWorld(rayFrom);
			setRayToWorld(rayTo);
		}

		public void setLayers(Bits layers) {
			this.layers = layers;
		}

		@Override
		public float addSingleResult(LocalRayResult rayResult, boolean normalInWorldSpace) {
			float hitFraction = rayResult.getHitFraction();
			btCollisionObject hitObj = rayResult.getCollisionObject();
			Entity entity = getEntity(hitObj.getUserPointer());

			if (entity instanceof GameModel) {
				GameModel model = (GameModel) entity;
				if (hitFraction < this.hitFraction && (layers == null || model.visibleOnLayers.intersects(layers))) {
					this.hitFraction = hitFraction;
					super.addSingleResult(rayResult, normalInWorldSpace);
					return hitFraction;
				}

			} else if (entity.getId() == scene.navmeshBody.getId()) {
				Triangle triangle = scene.navMesh.rayTest(ray, rayDistance, layers);
				if (triangle == null) {
					// Triangle is not on allowed layer
					return 1;
				}
				Intersector.intersectRayTriangle(ray, triangle.a, triangle.b, triangle.c, tmp);
				hitFraction = rayFrom.dst(tmp) / rayFrom.dst(rayTo);
				if (hitFraction < this.hitFraction) {
					this.hitFraction = hitFraction;
					rayResult.setHitFraction(hitFraction);
					super.addSingleResult(rayResult, normalInWorldSpace);
					return hitFraction;
				}
			}
			return 1;
		}
	}

	// Collision flags
	public final static short NONE_FLAG = 0;
	public final static short NAVMESH_FLAG = 1 << 6;
	public final static short PC_FLAG = 1 << 10;
	public final static short GROUND_FLAG = 1 << 8;
	public final static short OBJECT_FLAG = 1 << 9;
	public final static short ALL_FLAG = -1;

	// Bullet classes
	private final btDynamicsWorld dynamicsWorld;
	private final btDispatcher dispatcher;
	private final btConstraintSolver constraintSolver;
	private final btDbvtBroadphase broadphase;
	private final DebugDrawer debugDrawer;
	private final LayeredClosestRayResultCallback callback = new LayeredClosestRayResultCallback(Vector3.Zero, Vector3.Z);
	private final btCollisionConfiguration collisionConfig;
	private final CollisionContactListener contactListener;

	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();
	private final LongMap<GameObject> objectsById = new LongMap<GameObject>();
	private final LongMap<GameModel> modelsById = new LongMap<GameModel>();
	private GameScene scene;

	// Models
	private boolean modelCacheDirty = true;
	private final ModelCache modelCache = new ModelCache(new ModelCache.Sorter(), new ModelCache.TightMeshPool());
	private final Array<GameModel> dynamicModels = new Array<GameModel>();
	private Bits visibleLayers = new Bits();

	public Array<SteerableBody> characters = new Array<SteerableBody>();

	public GameEngine() {
		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase,
				constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(GameSettings.GRAVITY);

		debugDrawer = new DebugDrawer();
		dynamicsWorld.setDebugDrawer(debugDrawer);
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);
		contactListener = new CollisionContactListener();
	}

	public Entity rayTest(Ray ray, Vector3 hitPointWorld, short belongsToFlag, short collidesWithFlag,
						  float rayDistance, Bits layers) {
		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(rayDistance).add(rayFrom);
		callback.setCollisionObject(null);
		callback.setClosestHitFraction(1f);
		callback.setRay(ray, rayDistance);
		callback.setLayers(layers);

		callback.setCollisionFilterMask(belongsToFlag);
		callback.setCollisionFilterGroup(collidesWithFlag);

		dynamicsWorld.rayTest(rayFrom, rayTo, callback);

		if (callback.hasHit()) {
			if (hitPointWorld != null) {
				callback.getHitPointWorld(hitPointWorld);
			}
			long entityId = callback.getCollisionObject().getUserPointer();
			return getEntity(entityId);
		}
		return null;
	}

	public GameScene getScene() {
		return scene;
	}

	public Bits getVisibleLayers() {
		return visibleLayers;
	}

	public void setScene(GameScene scene) {
		// TODO: Remove any previous scene
		this.scene = scene;

		addEntity(scene.navmeshBody);

		Array<GameObject> objs = new Array<GameObject>();
		scene.getGameObjects(objs);
		for (GameObject obj : objs) {
			addEntity(obj);

			// TODO: handle this in a better way
			// Ideally the engine should not know the name of the entities in the scene
			if (obj.name.equals("human") || obj.name.equals("dog")) {
				characters.add((SteerableBody) obj);
			}
		}
	}

	@Override
	public void dispose() {
		collisionConfig.dispose();
		dispatcher.dispose();
		dynamicsWorld.dispose();
		broadphase.dispose();
		constraintSolver.dispose();
		contactListener.dispose();
		debugDrawer.dispose();
		callback.dispose();
	}

	public ModelCache getModelCache() {
		if (modelCacheDirty) {
			updateModelCache(visibleLayers);
		}
		return modelCache;
	}

	public Array<GameModel> getDynamicModels() {
		if (modelCacheDirty) {
			updateModelCache(visibleLayers);
		}
		return dynamicModels;
	}

	private void updateModelCache(Bits visibleLayers) {
		dynamicModels.clear();
		modelCache.begin();
		for (GameObject obj : objectsById.values()) {
			if (obj instanceof GameModelBody) {
				GameModelBody model = (GameModelBody) obj;
				if (model.mass == 0) {
					// All bodies with mass of zero are static so cache them if visible
					if (model.visibleOnLayers.intersects(visibleLayers)) {
						modelCache.add(model.modelInstance);
					}
				} else {
					// Dynamic bodies are checked for visibility in render method
					dynamicModels.add(model);
				}
			} else if (obj instanceof Billboard) {
				// TODO: If more billboards than selection marker are ever used, handle them here
				dynamicModels.add((Billboard) obj);

			} else if (obj instanceof GameModel) {
				// TODO: If non-static models without bodies are ever used, handle them here
				GameModel model = (GameModel) obj;
				if (model.visibleOnLayers.intersects(visibleLayers)) {
					modelCache.add(model.modelInstance);
				}
			}
		}
		modelCache.end();
		modelCacheDirty = false;
	}

	@Override
	public void addEntity(Entity entity) {
		super.addEntity(entity);

		boolean isStaticBody = true;

		if (entity instanceof Ragdoll) {
			Ragdoll gameObj = (Ragdoll) entity;
			for (btRigidBody bodyPart : gameObj.bodyPartMap.keys()) {
				bodyPart.setUserPointer(entity.getId());
				dynamicsWorld.addRigidBody(bodyPart, gameObj.belongsToFlag, gameObj.collidesWithFlag);
			}
		}

		if (entity instanceof GameModelBody) {
			GameModelBody gameObj = (GameModelBody) entity;
			gameObj.body.setUserPointer(entity.getId());
			dynamicsWorld.addRigidBody(gameObj.body, gameObj.belongsToFlag, gameObj.collidesWithFlag);
			for (btTypedConstraint constraint : gameObj.constraints) {
				dynamicsWorld.addConstraint(constraint, true);
			}
			if (gameObj.mass > 0) {
				isStaticBody = false;
				dynamicModels.add(gameObj);
			}

		} else if (entity instanceof InvisibleBody) {
			InvisibleBody gameObj = (InvisibleBody) entity;
			gameObj.body.setUserPointer(entity.getId());
			dynamicsWorld.addRigidBody(gameObj.body, gameObj.belongsToFlag, gameObj.collidesWithFlag);
		}

		if (entity instanceof GameModel) {
			GameModel gameObj = (GameModel) entity;
			modelsById.put(entity.getId(), gameObj);
		}

		if (entity instanceof GameObject) {
			GameObject gameObj = (GameObject) entity;
			objectsById.put(entity.getId(), gameObj);
		}

		modelCacheDirty = isStaticBody;
	}

	@Override
	public void removeEntity(Entity entity) {
		modelCacheDirty = true;

		if (entity instanceof Ragdoll) {
			Ragdoll gameObj = (Ragdoll) entity;
			for (btRigidBody bodyPart : gameObj.bodyPartMap.keys()) {
				dynamicsWorld.removeCollisionObject(bodyPart);
			}
		}

		if (entity instanceof GameModelBody) {
			GameModelBody gameObj = (GameModelBody) entity;
			dynamicsWorld.removeCollisionObject(gameObj.body);

			for (btTypedConstraint constraint : gameObj.constraints) {
				dynamicsWorld.removeConstraint(constraint);
			}

		} else if (entity instanceof InvisibleBody) {
			InvisibleBody gameObj = (InvisibleBody) entity;
			dynamicsWorld.removeCollisionObject(gameObj.body);
		}

		if (entity instanceof GameModel) {
			modelsById.remove(entity.getId());
			GameModel gameObj = (GameModel) entity;
			if (dynamicModels.contains(gameObj, true)) {
				modelCacheDirty = false;
			}
			dynamicModels.removeValue(gameObj, true);
		}

		if (entity instanceof GameObject) {
			objectsById.remove(entity.getId());
		}

		super.removeEntity(entity);
	}

	public void debugDrawWorld(Camera camera) {
		debugDrawer.begin(camera);
		dynamicsWorld.debugDrawWorld();
		debugDrawer.end();
	}

	public void setDebugMode(int mode) {
		debugDrawer.setDebugMode(mode);
	}

	public void update(float deltaTime) {
		// Update AI time
		GdxAI.getTimepiece().update(deltaTime);

		// Dispatch delayed messages
		MessageManager.getInstance().update();

		// Update Bullet simulation
		// On default fixedTimeStep = 1/60, small objects (the stick) will fall through 
		// the ground (the ground has relatively big triangles).
		dynamicsWorld.stepSimulation(deltaTime, 10, 1f / 240f);

		for (GameObject object : objectsById.values()) {
			if (object != null) {
				object.update(deltaTime);
			}
		}
	}

	@Override
	public void notifyEntitySelected(GameCharacter entity) {

	}

	@Override
	public void notifyLayerChanged(Bits layer) {
		visibleLayers.clear();
		visibleLayers.or(layer);
		updateModelCache(visibleLayers);
	}

	@Override
	public void notifyCursorWorldPosition(float x, float y, float z) {

	}

	public class CollisionContactListener extends ContactListener {

		public boolean onContactAdded(btCollisionObject colObj0, int partId0, int index0, btCollisionObject colObj1, int partId1, int index1) {
			Entity entity0 = getEntity(colObj0.getUserPointer());
			Entity entity1 = getEntity(colObj1.getUserPointer());
			Stick stick = null;
			if (entity0 instanceof Stick) {
				stick = (Stick) entity0;
			} else if (entity1 instanceof Stick) {
				stick = (Stick) entity1;
			}
			if (stick != null && !stick.hasLanded && stick.body.getLinearVelocity().isZero(0.1f)) {
				stick.owner.onStickLanded();
			}
			return true;
		}
	}
}