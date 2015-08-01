package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.mygdx.game.components.MoveAimComponent;
import com.mygdx.game.components.PhysicsComponent;

/**
 * Created by user on 8/1/15.
 */
public class PhysicsMoveSystem extends EntitySystem {

	public PhysicsMoveListener listener;
	public Family systemFamily;
	Vector3 prevVelocity = new Vector3();
	Vector3 velocity = new Vector3();
	Vector3 xzVelocity = new Vector3();
	Matrix4 transform = new Matrix4();
	private ImmutableArray<Entity> entities;
	private ComponentMapper<MoveAimComponent> moveComponents = ComponentMapper.getFor(MoveAimComponent.class);
	private ComponentMapper<PhysicsComponent> phyComponents = ComponentMapper.getFor(PhysicsComponent.class);
	public PhysicsMoveSystem() {
		systemFamily = Family.all(MoveAimComponent.class, PhysicsComponent.class).get();
		listener = new PhysicsMoveListener();
	}

	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
		for (Entity entity : entities) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.DISABLE_DEACTIVATION);
			phyCmp.body.setAngularFactor(Vector3.Y);
		}
	}

	@Override
	public void update(float deltaTime) {
		for (int i = 0; i < entities.size(); ++i) {

			Entity entity = entities.get(i);

			MoveAimComponent moveCmp = moveComponents.get(entity);
			PhysicsComponent phyCmp = phyComponents.get(entity);

			prevVelocity = phyCmp.body.getLinearVelocity();
			xzVelocity.set(moveCmp.directionMove).scl(1, 0, 1).nor().scl(deltaTime).scl(moveCmp.speed);
			velocity.set(xzVelocity).add(0, prevVelocity.y, 0);

			phyCmp.body.getWorldTransform(transform);
			transform.getTranslation(moveCmp.position);
			transform.setToLookAt(moveCmp.directionAim, moveCmp.up).inv();
			transform.setTranslation(moveCmp.position);

			phyCmp.body.proceedToTransform(transform);

			phyCmp.body.setLinearVelocity(velocity);
//			phyCmp.body.setAngularVelocity(Vector3.Zero);

		}
	}

	public class PhysicsMoveListener implements EntityListener {

		@Override
		public void entityAdded(Entity entity) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.DISABLE_DEACTIVATION);
			phyCmp.body.setAngularFactor(Vector3.Y);
		}

		@Override
		public void entityRemoved(Entity entity) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.ACTIVE_TAG);
			phyCmp.body.setAngularFactor(Vector3.Zero);
		}
	}
}
