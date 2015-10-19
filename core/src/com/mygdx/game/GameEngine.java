package com.mygdx.game;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.LongMap;
import com.mygdx.game.objects.*;
import com.mygdx.game.pathfinding.NavMesh;
import com.mygdx.game.settings.GameSettings;

import java.util.Iterator;

/**
 * Created by Johannes Sjolund on 10/19/15.
 */
public class GameEngine extends PooledEngine implements Disposable {

	public static final String tag = "PhysicsSystem";

	// Bullet classes
	public final btDynamicsWorld dynamicsWorld;
	private final btDispatcher dispatcher;
	private final btConstraintSolver constraintSolver;
	private final btDbvtBroadphase broadphase;
	private final DebugDrawer debugDrawer;
	private final ClosestRayResultCallback callback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);
	private final btCollisionConfiguration collisionConfig;

	//	private final CollisionContactListener contactListener;

	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();

	private final LongMap<GameModel> modelsById = new LongMap<GameModel>();
	private final LongMap<GameObject> objectsById = new LongMap<GameObject>();

	public NavMesh navmesh;

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
	}

	@Override
	public void dispose() {
		collisionConfig.dispose();
		dispatcher.dispose();
		dynamicsWorld.dispose();
		broadphase.dispose();
		constraintSolver.dispose();
//		contactListener.dispose();
		debugDrawer.dispose();
		callback.dispose();
	}

	public Iterator<GameModel> getModels() {
		return modelsById.values().iterator();
	}

	@Override
	public void addEntity(Entity entity) {
		super.addEntity(entity);

		if (entity instanceof GameCharacter) {
			GameCharacter gameObj = (GameCharacter) entity;
			gameObj.body.setUserPointer(entity.getId());
			dynamicsWorld.addRigidBody(gameObj.body, gameObj.belongsToFlag, gameObj.collidesWithFlag);
			for (btRigidBody body : gameObj.map.keys()) {
				body.setUserPointer(entity.getId());
				dynamicsWorld.addRigidBody(body, gameObj.belongsToFlag, gameObj.collidesWithFlag);
			}
			for (btTypedConstraint constraint : gameObj.constraints) {
				dynamicsWorld.addConstraint(constraint, true);
			}


		} else if (entity instanceof GameModelBody) {
			GameModelBody gameObj = (GameModelBody) entity;
			gameObj.body.setUserPointer(entity.getId());
			dynamicsWorld.addRigidBody(gameObj.body, gameObj.belongsToFlag, gameObj.collidesWithFlag);
			for (btTypedConstraint constraint : gameObj.constraints) {
				dynamicsWorld.addConstraint(constraint, true);
			}

		} else if (entity instanceof InvisibleBody) {
			InvisibleBody gameObj = (InvisibleBody) entity;
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
	}

	@Override
	public void removeEntity(Entity entity) {
		super.removeEntity(entity);

		if (entity instanceof GameCharacter) {
			GameCharacter gameObj = (GameCharacter) entity;
			dynamicsWorld.removeCollisionObject(gameObj.body);
			for (btRigidBody body : gameObj.map.keys()) {
				dynamicsWorld.removeCollisionObject(body);
			}
			for (btTypedConstraint constraint : gameObj.constraints) {
				dynamicsWorld.removeConstraint(constraint);
			}


		} else if (entity instanceof GameModelBody) {
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
		}

		if (entity instanceof GameObject) {
			objectsById.remove(entity.getId());
		}
	}

	public void debugDrawWorld(Camera camera) {
		debugDrawer.begin(camera);
		dynamicsWorld.debugDrawWorld();
		debugDrawer.end();
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		dynamicsWorld.stepSimulation(deltaTime, 5, 1f / 60f);

		for (GameObject object : objectsById.values()) {
			object.update(deltaTime);
		}
	}


	public Entity rayTest(Ray ray, Vector3 hitPointWorld, short belongsToFlag, short collidesWithFlag,
						  float maxDistance) {
		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(maxDistance).add(rayFrom);

		// Because we reuse the ClosestRayResultCallback, we need reset it's values
		callback.setCollisionObject(null);
		callback.setClosestHitFraction(1f);
		callback.setRayFromWorld(rayFrom);
		callback.setRayToWorld(rayTo);

		callback.setCollisionFilterMask(belongsToFlag);
		callback.setCollisionFilterGroup(collidesWithFlag);

		dynamicsWorld.rayTest(rayFrom, rayTo, callback);

		if (callback.hasHit()) {
			callback.getHitPointWorld(hitPointWorld);
			long entityId = callback.getCollisionObject().getUserPointer();
			return getEntity(entityId);
		}
		return null;
	}

//	public class CollisionContactListener extends ContactListener {
//		@Override
//		public boolean onContactAdded(btManifoldPoint cp,
//									  btCollisionObject colObj0, int partId0, int index0,
//									  btCollisionObject colObj1, int partId1, int index1) {
//			long entityId0 = colObj0.getUserPointer();
//			long entityId1 = colObj1.getUserPointer();
//			return true;
//		}
//	}
}