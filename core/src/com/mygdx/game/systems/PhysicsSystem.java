package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.RagdollComponent;

/**
 * Created by user on 7/31/15.
 */
public class PhysicsSystem extends EntitySystem implements Disposable {

	public static final String tag = "PhysicsSystem";

	// Collision flags
	public final static short NONE_FLAG = 0;
	public final static short NAVMESH_FLAG = 1 << 6;
	public final static short PC_FLAG = 1 << 10;
	public final static short GROUND_FLAG = 1 << 8;
	public final static short OBJECT_FLAG = 1 << 9;
	public final static short ALL_FLAG = -1;

	// Ashley
	public final PhysicsListener physicsComponentListener;
	public final RagdollListener ragdollComponentListener;
	public final Family systemFamily;

	// Bullet classes
	public final btDynamicsWorld dynamicsWorld;
	private ImmutableArray<Entity> entities;
	private final CollisionContactListener contactListener;
	private final btCollisionConfiguration collisionConfig;
	private final btDispatcher dispatcher;
	private final btConstraintSolver constraintSolver;
	private final btDbvtBroadphase broadphase;
	private final DebugDrawer debugDrawer;
	private final ClosestRayResultCallback callback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);

	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();

	public PhysicsSystem() {
		super(0);
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

		systemFamily = Family.all(PhysicsComponent.class).get();

		contactListener = new CollisionContactListener();
		physicsComponentListener = new PhysicsListener();
		ragdollComponentListener = new RagdollListener();
	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	public void debugDrawWorld(Camera camera) {
		debugDrawer.begin(camera);
		dynamicsWorld.debugDrawWorld();
		debugDrawer.end();
	}

	@Override
	public void update(float deltaTime) {
		dynamicsWorld.stepSimulation(deltaTime, 5, 1f / 60f);
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

		Entity e = null;
		if (callback.hasHit()) {
			long entityId = callback.getCollisionObject().getUserPointer();
			callback.getHitPointWorld(hitPointWorld);
			for (Entity entity : entities) {
				if (entity.getId() == entityId) {
					e = entity;
					break;
				}
			}
		}
		return e;
	}


	public class CollisionContactListener extends ContactListener {
//		@Override
//		public boolean onContactAdded(btManifoldPoint cp,
//									  btCollisionObject colObj0, int partId0, int index0,
//									  btCollisionObject colObj1, int partId1, int index1) {
//			long entityId0 = colObj0.getUserPointer();
//			long entityId1 = colObj1.getUserPointer();
//			return true;
//		}
	}

	public class PhysicsListener implements EntityListener {

		@Override
		public void entityAdded(Entity entity) {
			PhysicsComponent cmp = entity.getComponent(PhysicsComponent.class);
			cmp.body.setUserPointer(entity.getId());
			dynamicsWorld.addRigidBody(cmp.body, cmp.belongsToFlag, cmp.collidesWithFlag);

		}

		@Override
		public void entityRemoved(Entity entity) {
			PhysicsComponent cmp = entity.getComponent(PhysicsComponent.class);
			dynamicsWorld.removeCollisionObject(cmp.body);
		}
	}

	public class RagdollListener implements EntityListener {

		@Override
		public void entityAdded(Entity entity) {
			Gdx.app.debug(tag, "Adding ragdoll");
			RagdollComponent cmp = entity.getComponent(RagdollComponent.class);
			if (!cmp.ragdollControl) {
				for (btRigidBody body : cmp.map.keys()) {
					body.setGravity(Vector3.Zero);
				}
			}
			for (btRigidBody body : cmp.map.keys()) {
				body.setUserPointer(entity.getId());
				dynamicsWorld.addRigidBody(body, cmp.belongsToFlag, cmp.collidesWithFlag);
			}
			for (btTypedConstraint constraint : cmp.constraints) {
				dynamicsWorld.addConstraint(constraint, true);
			}
		}

		@Override
		public void entityRemoved(Entity entity) {
			Gdx.app.debug(tag, "Removing ragdoll");
			RagdollComponent cmp = entity.getComponent(RagdollComponent.class);
			for (btRigidBody body : cmp.map.keys()) {
				dynamicsWorld.removeCollisionObject(body);
			}
			for (btTypedConstraint constraint : cmp.constraints) {
				dynamicsWorld.removeConstraint(constraint);
			}
		}
	}

}
