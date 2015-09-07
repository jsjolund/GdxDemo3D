package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.GameSettings;
import com.mygdx.game.components.CharacterActionComponent;
import com.mygdx.game.components.PathFindingComponent;
import com.mygdx.game.components.PhysicsComponent;

/**
 * Created by user on 8/30/15.
 */
public class PathFindingSystem extends IteratingSystem {

	Matrix4 matrix = new Matrix4();
	Vector3 pos = new Vector3();
	Vector3 goalDirection = new Vector3();
	Vector3 newVelocity = new Vector3();
	float yVelocity = 0;
	private ComponentMapper<PathFindingComponent> pathCmps =
			ComponentMapper.getFor(PathFindingComponent.class);
	private ComponentMapper<PhysicsComponent> phyCmps =
			ComponentMapper.getFor(PhysicsComponent.class);
	private ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);
	public PathFindingSystem(Family family) {
		super(family);
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		PhysicsComponent phyCmp = phyCmps.get(entity);
		PathFindingComponent pathCmp = pathCmps.get(entity);

		if (pathCmp.goal == null) {
			return;
		}

		phyCmp.body.getWorldTransform(matrix);
		matrix.getTranslation(pos);

		if (pathCmp.goal.equals(pathCmp.lastProcessedGoal)) {

			CharacterActionComponent actionCmp = actionCmps.get(entity);

			yVelocity = phyCmp.body.getLinearVelocity().y;
			float xzDst = Vector2.dst2(pathCmp.goal.x, pathCmp.goal.z, pos.x, pos.z);
			if (xzDst < 0.01f) {
				phyCmp.body.setLinearVelocity(newVelocity.set(0, yVelocity, 0));
				phyCmp.body.setAngularVelocity(Vector3.Zero);

				if (actionCmp != null) {
					actionCmp.nextAction = CharacterActionComponent.Action.IDLE;
				}


			} else {
				matrix.idt();
				goalDirection.set(pos).sub(pathCmp.goal).scl(-1, 0, 1).nor();
				matrix.setToLookAt(goalDirection, Vector3.Y);
				matrix.setTranslation(pos);
				phyCmp.body.setWorldTransform(matrix);


				if (pathCmp.run) {
					newVelocity.set(goalDirection.scl(1, 0, -1)).scl(GameSettings.PLAYER_RUN_SPEED);
					if (actionCmp != null) {
						actionCmp.nextAction = CharacterActionComponent.Action.RUN;
					}
				} else {
					newVelocity.set(goalDirection.scl(1, 0, -1)).scl(GameSettings.PLAYER_WALK_SPEED);
					if (actionCmp != null) {
						actionCmp.nextAction = CharacterActionComponent.Action.WALK;
					}
				}

				newVelocity.y = yVelocity;
				phyCmp.body.setLinearVelocity(newVelocity);

			}
			return;
		}

		if (pathCmp.lastProcessedGoal == null) {
			pathCmp.lastProcessedGoal = new Vector3(pathCmp.goal);
		} else {
			pathCmp.lastProcessedGoal.set(pathCmp.goal);
		}
	}
}
