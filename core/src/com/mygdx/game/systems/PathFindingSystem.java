package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.CharacterActionComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.PhysicsComponent;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingSystem extends IteratingSystem {

	private final Matrix4 matrix = new Matrix4();
	private final Vector3 goalDirection = new Vector3();
	private final Vector3 newVelocity = new Vector3();
	private final ComponentMapper<PathFindingComponent> pathCmps =
			ComponentMapper.getFor(PathFindingComponent.class);
	private final ComponentMapper<PhysicsComponent> phyCmps =
			ComponentMapper.getFor(PhysicsComponent.class);
	private final ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);
	private float yVelocity = 0;

	public PathFindingSystem(Family family) {
		super(family);
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		PhysicsComponent phyCmp = phyCmps.get(entity);
		PathFindingComponent pathCmp = pathCmps.get(entity);
		CharacterActionComponent actionCmp = actionCmps.get(entity);

		phyCmp.body.getWorldTransform(matrix);
		matrix.getTranslation(pathCmp.currentPosition);

		if (pathCmp.currentGoal == null && pathCmp.path.size == 0) {
			actionCmp.nextAction = CharacterActionComponent.Action.IDLE;
			return;
		}

		if (pathCmp.currentGoal == null && pathCmp.path.size > 0) {
			pathCmp.currentGoal = pathCmp.path.pop();
		}

		if (pathCmp.currentGoal != null) {
			yVelocity = phyCmp.body.getLinearVelocity().y;

			float xzDst = Vector2.dst2(pathCmp.currentGoal.x, pathCmp.currentGoal.z,
					pathCmp.currentPosition.x, pathCmp.currentPosition.z);

			if (xzDst < 0.01f) {
				phyCmp.body.setLinearVelocity(newVelocity.set(0, yVelocity, 0));
				phyCmp.body.setAngularVelocity(Vector3.Zero);
				// set new goal if not empty
				pathCmp.currentGoal = null;
				if (pathCmp.path.size != 0) {
					pathCmp.currentGoal = pathCmp.path.pop();
				}

			} else {
				matrix.idt();
				goalDirection.set(pathCmp.currentPosition).sub(pathCmp.currentGoal).scl(-1, 0, 1).nor();
				matrix.setToLookAt(goalDirection, Vector3.Y);
				matrix.setTranslation(pathCmp.currentPosition);
				phyCmp.body.setWorldTransform(matrix);

				if (pathCmp.run) {
					newVelocity.set(goalDirection.scl(1, 0, -1)).scl(GameSettings.PLAYER_RUN_SPEED);
					actionCmp.nextAction = CharacterActionComponent.Action.RUN;
				} else {
					newVelocity.set(goalDirection.scl(1, 0, -1)).scl(GameSettings.PLAYER_WALK_SPEED);
					actionCmp.nextAction = CharacterActionComponent.Action.WALK;
				}

				newVelocity.y = yVelocity;
				phyCmp.body.setLinearVelocity(newVelocity);
			}
		}


	}
}
