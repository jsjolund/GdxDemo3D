package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.components.RagdollComponent;

/**
 * Created by user on 8/30/15.
 */
public class PathFollowSystem extends IteratingSystem {

	private final Matrix4 matrix = new Matrix4();
	private final Vector3 goalDirection = new Vector3();
	private final Vector3 newVelocity = new Vector3();

	private final ComponentMapper<PathFindingComponent> pathCmps = ComponentMapper.getFor(PathFindingComponent.class);
	private final ComponentMapper<PhysicsComponent> phyCmps = ComponentMapper.getFor(PhysicsComponent.class);
	private final ComponentMapper<RagdollComponent> ragCmp = ComponentMapper.getFor(RagdollComponent.class);

	public PathFollowSystem() {
		super(Family.all(PathFindingComponent.class).one(PhysicsComponent.class, RagdollComponent.class).get());
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		PathFindingComponent pathCmp = pathCmps.get(entity);
		PhysicsComponent phyCmp = phyCmps.get(entity);
		if (phyCmp == null) {
			phyCmp = ragCmp.get(entity);
		}

		if (pathCmp.currentGoal == null && pathCmp.path.size == 0) {
			pathCmp.goalReached = true;
			return;
		}

		if (pathCmp.currentGoal != null) {
			float yVelocity = phyCmp.body.getLinearVelocity().y;

			phyCmp.body.getWorldTransform(matrix);
			matrix.getTranslation(pathCmp.currentPosition);

			float xzDst = Vector2.dst2(pathCmp.currentGoal.x, pathCmp.currentGoal.z,
					pathCmp.currentPosition.x, pathCmp.currentPosition.z);

			if (xzDst < 0.1f) {

				// set new goal if not empty
				pathCmp.currentGoal = null;
				if (pathCmp.path.size > 0) {
					pathCmp.currentGoal = pathCmp.path.pop();
				} else {
					phyCmp.body.setLinearVelocity(newVelocity.set(0, yVelocity, 0));
					phyCmp.body.setAngularVelocity(Vector3.Zero);
				}

			} else {
				matrix.idt();
				goalDirection.set(pathCmp.currentPosition).sub(pathCmp.currentGoal).scl(-1, 0, 1).nor();
				matrix.setToLookAt(goalDirection, Vector3.Y);
				matrix.setTranslation(pathCmp.currentPosition);
				phyCmp.body.setWorldTransform(matrix);

				newVelocity.set(goalDirection.scl(1, 0, -1)).scl(pathCmp.moveSpeed);
				pathCmp.goalReached = false;

				newVelocity.y = yVelocity;
				phyCmp.body.setLinearVelocity(newVelocity);
			}
		}


	}
}
