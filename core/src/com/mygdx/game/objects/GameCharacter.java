package com.mygdx.game.objects;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;

/**
 * Created by Johannes Sjolund on 10/18/15.
 */
public class GameCharacter extends Ragdoll {

	public final StateMachine<GameCharacter> stateMachine;
	public final AnimationController controller;
	public final CharacterAnimationListener animationListener;
	public CharacterState moveState = CharacterState.MOVE_WALK;
	public CharacterState idleState = CharacterState.IDLE_STAND;
	public boolean isMoving = false;

	public GameCharacter(Model model,
						 String id,
						 Vector3 location,
						 Vector3 rotation,
						 Vector3 scale,
						 btCollisionShape shape,
						 float mass,
						 short belongsToFlag,
						 short collidesWithFlag,
						 boolean callback,
						 boolean noDeactivate,
						 String ragdollJson,
						 String armatureNodeId) {

		super(model, id, location, rotation, scale, shape, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate, ragdollJson, armatureNodeId);

		pathData = new PathFindingData(location);
		controller = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();
		stateMachine = new DefaultStateMachine<GameCharacter>(this, CharacterState.GLOBAL);
		stateMachine.changeState(CharacterState.IDLE_STAND);

	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		stateMachine.update();
		controller.update(deltaTime);

		if (pathData.goalReached && isMoving) {
			stateMachine.changeState(idleState);
		} else if (!pathData.goalReached && !isMoving) {
			stateMachine.changeState(moveState);
		}
	}

	public enum CharacterState implements State<GameCharacter> {
		MOVE_RUN() {
			@Override
			public void enter(GameCharacter entity) {
				// TODO: Set animation, sound, movement speed
				entity.controller.setAnimation("armature|move_run", -1);
				entity.pathData.moveSpeed = 8f;
				entity.idleState = IDLE_STAND;
				entity.isMoving = true;

			}
		},
		MOVE_WALK() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|move_walk", -1);
				entity.pathData.moveSpeed = 4f;
				entity.idleState = IDLE_STAND;
				entity.isMoving = true;

			}
		},
		MOVE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|move_crouch", -1);
				entity.pathData.moveSpeed = 2.5f;
				entity.idleState = IDLE_CROUCH;
				entity.isMoving = true;

			}
		},
		MOVE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|move_crouch", -1);
				entity.pathData.moveSpeed = 1f;
				entity.idleState = IDLE_CRAWL;
				entity.isMoving = true;

			}
		},
		IDLE_STAND() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|idle_stand", -1);
				entity.pathData.moveSpeed = 0;
				entity.isMoving = false;
			}
		},
		IDLE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|idle_crouch", -1);
				entity.pathData.moveSpeed = 0;
				entity.isMoving = false;
			}
		},
		IDLE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|idle_crouch", -1);
				entity.pathData.moveSpeed = 0;
				entity.isMoving = false;
			}
		},
		THROW() {
			@Override
			public void enter(GameCharacter entity) {
				entity.controller.setAnimation("armature|action_throw");
				entity.pathData.moveSpeed = 0;
				entity.isMoving = false;
			}
		},
		DEAD() {
			@Override
			public void enter(GameCharacter entity) {
				// Turn off animation, set ragdoll control
				entity.controller.setAnimation("armature|idle_stand", -1);
				entity.controller.paused = true;
				entity.pathData.moveSpeed = 0;
				entity.pathData.clearPath();
				entity.isMoving = false;
				entity.toggle(true);
			}

			@Override
			public void exit(GameCharacter entity) {
				entity.controller.paused = false;
				entity.toggle(false);
			}
		},
		GLOBAL() {};

		@Override
		public void enter(GameCharacter entity) {
		}

		@Override
		public void update(GameCharacter entity) {
		}

		@Override
		public void exit(GameCharacter entity) {
		}

		@Override
		public boolean onMessage(GameCharacter entity, Telegram telegram) {
			return false;
		}


	}

	public class CharacterAnimationListener implements AnimationController.AnimationListener {
		@Override
		public void onEnd(AnimationController.AnimationDesc animation) {

		}

		@Override
		public void onLoop(AnimationController.AnimationDesc animation) {

		}
	}
}
