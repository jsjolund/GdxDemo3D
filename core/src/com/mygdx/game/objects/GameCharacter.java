package com.mygdx.game.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.settings.SteerSettings;

/**
 * Created by Johannes Sjolund on 10/18/15.
 */
public class GameCharacter extends Ragdoll {

	public enum CharacterState implements State<GameCharacter> {
		MOVE_RUN() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|move_run", -1, 1, entity.animationListener, 0.1f);
				entity.idleState = IDLE_STAND;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed * SteerSettings.runMultiplier);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration * SteerSettings.runMultiplier);

				entity.setMaxAngularSpeed(SteerSettings.maxAngularSpeed * SteerSettings.runMultiplier);
				entity.setMaxAngularAcceleration(SteerSettings.maxAngularAcceleration * SteerSettings.runMultiplier);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(SteerSettings.decelerationRadius * SteerSettings.runMultiplier);
				}
			}

			@Override
			public void update(GameCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.2f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		MOVE_WALK() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|move_walk", -1, 1, entity.animationListener, 0.1f);
				entity.idleState = IDLE_STAND;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration);

				entity.setMaxAngularSpeed(SteerSettings.maxAngularSpeed);
				entity.setMaxAngularAcceleration(SteerSettings.maxAngularAcceleration);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(SteerSettings.decelerationRadius);
				}
			}

			@Override
			public void update(GameCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.4f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		MOVE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|move_crouch", -1, 1, entity.animationListener, 0.15f);
				entity.idleState = IDLE_CROUCH;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed * SteerSettings.crouchMultiplier);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration * SteerSettings.crouchMultiplier);

				entity.setMaxAngularSpeed(SteerSettings.maxAngularSpeed * SteerSettings.crouchMultiplier);
				entity.setMaxAngularAcceleration(SteerSettings.maxAngularAcceleration * SteerSettings.crouchMultiplier);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(SteerSettings.decelerationRadius * SteerSettings.crouchMultiplier);
				}
			}

			@Override
			public void update(GameCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.5f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		MOVE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|move_crouch", -1, 1, entity.animationListener, 0.1f);
				entity.idleState = IDLE_CRAWL;
			}
		},
		IDLE_STAND() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|idle_stand", -1, 1, entity.animationListener, 0.2f);
			}
		},
		IDLE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|idle_crouch", -1, 1, entity.animationListener, 0.2f);
			}
		},
		IDLE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|idle_crouch", -1, 1, entity.animationListener, 0.2f);
			}
		},
		THROW() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|action_throw", 1, 1, entity.animationListener, 0.1f);
			}
		},
		DEAD() {
			@Override
			public void enter(GameCharacter entity) {
				// Turn off animation, set ragdoll control
				entity.animations.setAnimation("armature|idle_stand", -1);
				entity.animations.paused = true;
				entity.setRagdollControl(true);
			}

			@Override
			public void exit(GameCharacter entity) {
				entity.animations.paused = false;
				entity.setRagdollControl(false);
			}
		},
		GLOBAL() {};

		@Override
		public void enter(GameCharacter entity) {
		}

		@Override
		public void update(GameCharacter entity) {
			float deltaTime = Gdx.graphics.getDeltaTime();
			entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
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

	public final StateMachine<GameCharacter> stateMachine;
	public final AnimationController animations;
	public final CharacterAnimationListener animationListener;
	public CharacterState moveState = CharacterState.MOVE_WALK;
	public CharacterState idleState = CharacterState.IDLE_STAND;
	private boolean wasSteering = false;

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

		super(model, id, location, rotation, scale,
				shape, mass, belongsToFlag, collidesWithFlag,
				callback, noDeactivate, ragdollJson, armatureNodeId);

		animations = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();
		stateMachine = new DefaultStateMachine<GameCharacter>(this, CharacterState.GLOBAL);
		stateMachine.changeState(idleState);

		body.setAngularFactor(Vector3.Y);
		body.setFriction(SteerSettings.idleFriction);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		stateMachine.update();

		boolean isSteering = isSteering();
		if (isSteering && !wasSteering) {
			wasSteering = isSteering;
			stateMachine.changeState(moveState);
		} else if (!isSteering && wasSteering) {
			wasSteering = isSteering;
			if (stateMachine.getCurrentState() != CharacterState.DEAD) {
				stateMachine.changeState(idleState);
			}
		}

	}
}
