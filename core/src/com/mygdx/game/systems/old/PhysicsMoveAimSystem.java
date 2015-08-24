package com.mygdx.game.systems.old;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.mygdx.game.components.PhysicsComponent;

/**
 * Created by user on 8/1/15.
 */
public class PhysicsMoveAimSystem extends IteratingSystem {

	public PhysicsMoveListener listener;
	//	public Family family;
	Vector3 prevVelocity = new Vector3();
	Vector3 velocity = new Vector3();
	Vector3 xzVelocity = new Vector3();
	Vector3 xzRotation = new Vector3();
	Matrix4 transform = new Matrix4();
	//	private ImmutableArray<Entity> entities;
	private ComponentMapper<MoveAimComponent> moveComponents = ComponentMapper.getFor(MoveAimComponent.class);
	private ComponentMapper<PhysicsComponent> phyComponents = ComponentMapper.getFor(PhysicsComponent.class);

	public PhysicsMoveAimSystem(Family family) {
		super(family);
	}

	@Override
	public void addedToEngine(Engine engine) {
		super.addedToEngine(engine);
		for (Entity entity : super.getEntities()) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.DISABLE_DEACTIVATION);
			phyCmp.body.setAngularFactor(Vector3.Y);
		}
	}

	@Override
	public void removedFromEngine(Engine engine) {
		super.removedFromEngine(engine);
		for (Entity entity : super.getEntities()) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.ACTIVE_TAG);
			phyCmp.body.setAngularFactor(Vector3.Zero);
		}
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		MoveAimComponent moveCmp = moveComponents.get(entity);
		PhysicsComponent phyCmp = phyComponents.get(entity);

		prevVelocity = phyCmp.body.getLinearVelocity();

		xzVelocity.set(moveCmp.directionMove).scl(1, 0, 1).nor().scl(deltaTime).scl(moveCmp.speed);

		velocity.set(xzVelocity).add(0, prevVelocity.y, 0);

		phyCmp.body.getWorldTransform(transform);
		transform.getTranslation(moveCmp.position);
		transform.setToLookAt(xzRotation.set(moveCmp.directionAim).scl(1, 0, 1), moveCmp.up).inv();
		transform.setTranslation(moveCmp.position);
		phyCmp.body.proceedToTransform(transform);

		phyCmp.body.setLinearVelocity(velocity);
	}

	public class PhysicsMoveListener implements EntityListener {

		@Override
		public void entityAdded(Entity entity) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.DISABLE_DEACTIVATION);
		}

		@Override
		public void entityRemoved(Entity entity) {
			PhysicsComponent phyCmp = phyComponents.get(entity);
			phyCmp.body.setActivationState(Collision.ACTIVE_TAG);
		}
	}
}
