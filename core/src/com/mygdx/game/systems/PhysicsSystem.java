package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.components.PhysicsComponent;

/**
 * Created by user on 7/31/15.
 */
public class PhysicsSystem extends EntitySystem implements Disposable {

	// Collision flags
	public final static short NONE_FLAG = 0;
	public final static short GROUND_FLAG = 1 << 8;
	public final static short OBJECT_FLAG = 1 << 9;
	public final static short ALL_FLAG = -1;
	public static final String tag = "PhysicsSystem";
	public PhysicsListener listener;
	public Family systemFamily;
	private ImmutableArray<Entity> entities;

	private CollisionContactListener contactListener;
	private btCollisionConfiguration collisionConfig;
	private btDispatcher dispatcher;
	public btDynamicsWorld dynamicsWorld;
	private btConstraintSolver constraintSolver;
	private btDbvtBroadphase broadphase;
	private DebugDrawer debugDrawer;

	public PhysicsSystem() {
		super(0);
		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase,
				constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(new Vector3(0, -9.82f, 0));

		debugDrawer = new DebugDrawer();
		dynamicsWorld.setDebugDrawer(debugDrawer);
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_DrawWireframe);

		contactListener = new CollisionContactListener();
		listener = new PhysicsListener();
		systemFamily = Family.all(PhysicsComponent.class).get();
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
//		Gdx.app.debug(tag, "");
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
	}

	public Entity rayTest(Ray ray, Vector3 point, short mask,
						  float maxDistance) {
		Vector3 tmp = new Vector3();

		Vector3 rayFrom = new Vector3(ray.origin);
		Vector3 rayTo = new Vector3(ray.direction).scl(maxDistance)
				.add(rayFrom);

		ClosestRayResultCallback callback = new ClosestRayResultCallback(
				rayFrom, rayTo);
		callback.setCollisionFilterMask(mask);
		dynamicsWorld.rayTest(rayFrom, rayTo, callback);
		if (callback.hasHit()) {
			long entityId = callback.getCollisionObject().getUserPointer();
			callback.getHitPointWorld(point);
			callback.getHitNormalWorld(tmp);
			point.add(tmp.nor());
			for (Entity e : entities) {
				if (e.getId() == entityId) {
					return e;
				}
			}
		}
		callback.dispose();
		return null;
	}

	public class CollisionContactListener extends ContactListener {

		@Override
		public boolean onContactAdded(btManifoldPoint cp,
									  btCollisionObject colObj0, int partId0, int index0,
									  btCollisionObject colObj1, int partId1, int index1) {

			long entityId0 = colObj0.getUserPointer();
			long entityId1 = colObj1.getUserPointer();

//			System.out.println(entityId0 + " " + entityId1);

			return true;
		}
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

}
