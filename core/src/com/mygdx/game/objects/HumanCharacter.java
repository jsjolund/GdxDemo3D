/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


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

/**
 * @author jsjolund
 */
public class HumanCharacter extends Ragdoll {

	public enum CharacterState implements State<HumanCharacter> {
		IDLE_STAND() {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|idle_stand", -1, 1, entity.animationListener, 0.2f);
			}
		},
		IDLE_CROUCH() {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|idle_crouch", -1, 1, entity.animationListener, 0.2f);
			}
		},
		IDLE_CRAWL() {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|idle_crouch", -1, 1, entity.animationListener, 0.2f);
			}
		},
		MOVE_RUN(CharacterState.IDLE_STAND, 0.2f) {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|move_run", -1, 1, entity.animationListener, 0.1f);
				entity.moveState = this;

				entity.setMaxLinearSpeed(HumanSteerSettings.maxLinearSpeed * HumanSteerSettings.runMultiplier);
				entity.setMaxLinearAcceleration(HumanSteerSettings.maxLinearAcceleration * HumanSteerSettings.runMultiplier);

				entity.setMaxAngularSpeed(HumanSteerSettings.maxAngularSpeed * HumanSteerSettings.runMultiplier);
				entity.setMaxAngularAcceleration(HumanSteerSettings.maxAngularAcceleration * HumanSteerSettings.runMultiplier);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(HumanSteerSettings.decelerationRadius * HumanSteerSettings.runMultiplier);
				}
			}
		},
		MOVE_WALK(CharacterState.IDLE_STAND, 0.4f) {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|move_walk", -1, 1, entity.animationListener, 0.1f);
				entity.moveState = this;

				entity.setMaxLinearSpeed(HumanSteerSettings.maxLinearSpeed);
				entity.setMaxLinearAcceleration(HumanSteerSettings.maxLinearAcceleration);

				entity.setMaxAngularSpeed(HumanSteerSettings.maxAngularSpeed);
				entity.setMaxAngularAcceleration(HumanSteerSettings.maxAngularAcceleration);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(HumanSteerSettings.decelerationRadius);
				}
			}
		},
		MOVE_CROUCH(CharacterState.IDLE_CROUCH, 0.5f) {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|move_crouch", -1, 1, entity.animationListener, 0.15f);
				entity.moveState = this;

				entity.setMaxLinearSpeed(HumanSteerSettings.maxLinearSpeed * HumanSteerSettings.crouchMultiplier);
				entity.setMaxLinearAcceleration(HumanSteerSettings.maxLinearAcceleration * HumanSteerSettings.crouchMultiplier);

				entity.setMaxAngularSpeed(HumanSteerSettings.maxAngularSpeed * HumanSteerSettings.crouchMultiplier);
				entity.setMaxAngularAcceleration(HumanSteerSettings.maxAngularAcceleration * HumanSteerSettings.crouchMultiplier);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(HumanSteerSettings.decelerationRadius * HumanSteerSettings.crouchMultiplier);
				}
			}
		},
		MOVE_CRAWL() {},
		THROW() {
			@Override
			public void enter(HumanCharacter entity) {
				entity.animations.animate("armature|action_throw", 1, 1, entity.animationListener, 0.1f);
			}
		},
		DEAD() {
			@Override
			public void enter(HumanCharacter entity) {
				// Turn off animation, set ragdoll control
				entity.animations.setAnimation("armature|idle_stand", -1);
				entity.animations.paused = true;
				entity.steeringBehavior = null;
				entity.navMeshPointPath.clear();
				entity.navMeshGraphPath.clear();
				entity.finishSteering();
				entity.body.setFriction(1);
				entity.setRagdollControl(true);

			}

			@Override
			public void exit(HumanCharacter entity) {
				entity.animations.paused = false;
				entity.setRagdollControl(false);
			}
		},
		GLOBAL() {};

		protected final CharacterState idleState;
		protected final float multiplier;
		
		private CharacterState() {
			this(null, 0);
		}
		
		private CharacterState(CharacterState idleState, float multiplier) {
			this.idleState = idleState;
			this.multiplier = multiplier;
		}

		private boolean isMovementState() {
			return idleState != null;
		}

		@Override
		public void enter(HumanCharacter entity) {
		}

		@Override
		public void update(HumanCharacter entity) {
			if (entity.isSteering()) {
				if (!entity.wasSteering) {
					entity.wasSteering = true;
					entity.stateMachine.changeState(entity.moveState);
					return;
				}
			} else {
				if (entity.wasSteering) {
					entity.wasSteering = false;
					if (entity.stateMachine.getCurrentState() != CharacterState.DEAD) {
						entity.stateMachine.changeState(entity.moveState.idleState);
						return;
					}
				}
				else {
					CharacterState previousState = (CharacterState)entity.stateMachine.getPreviousState();
					if (previousState.isMovementState()) {
						entity.moveState = previousState;
						entity.stateMachine.changeState(previousState.idleState);
						return;
					}
				}
			}

			// update current state's animation
			float deltaTime = Gdx.graphics.getDeltaTime();
			if (idleState != null) {
				// this is a movement state
				deltaTime *= entity.getLinearVelocity().len() * multiplier;
			}
			entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
		}

		@Override
		public void exit(HumanCharacter entity) {
		}

		@Override
		public boolean onMessage(HumanCharacter entity, Telegram telegram) {
			return false;
		}
	}

	public static class HumanSteerSettings implements SteerSettings {
		public static float maxLinearAcceleration = 50f;
		public static float maxLinearSpeed = 2f;
		public static float maxAngularAcceleration = 100f;
		public static float maxAngularSpeed = 15f;
		public static float idleFriction = 0.9f;
		public static float zeroLinearSpeedThreshold = 0.001f;
		public static float runMultiplier = 2f;
		public static float crouchMultiplier = 0.5f;
		public static float timeToTarget = 0.1f;
		public static float arrivalTolerance = 0.1f;
		public static float decelerationRadius = 0.5f;
		public static float predictionTime = 0f;
		public static float pathOffset = 1f;

		@Override
		public float getTimeToTarget() {
			return timeToTarget;
		}

		@Override
		public float getArrivalTolerance() {
			return arrivalTolerance;
		}

		@Override
		public float getDecelerationRadius() {
			return decelerationRadius;
		}

		@Override
		public float getPredictionTime() {
			return predictionTime;
		}

		@Override
		public float getPathOffset() {
			return pathOffset;
		}

		@Override
		public float getZeroLinearSpeedThreshold() {
			return zeroLinearSpeedThreshold;
		}

		@Override
		public float getIdleFriction() {
			return idleFriction;
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

	public final StateMachine<HumanCharacter> stateMachine;
	public final AnimationController animations;
	public final CharacterAnimationListener animationListener;
	private final SteerSettings steerSettings;
	private CharacterState moveState = CharacterState.MOVE_WALK;
	private boolean wasSteering = false;

	public HumanCharacter(Model model,
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
				callback, noDeactivate, ragdollJson, armatureNodeId,
				new HumanSteerSettings());
		// Only allow physics engine to turn player capsule around the up axis
		body.setAngularFactor(Vector3.Y);

		animations = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();

		stateMachine = new DefaultStateMachine<HumanCharacter>(this, CharacterState.GLOBAL);
		// Set the steering variables associated with default move state (walking)
		stateMachine.changeState(moveState);
		// Then make the character idle
		stateMachine.changeState(moveState.idleState);

		steerSettings = new HumanSteerSettings();
	}

	@Override
	public void calculateNewPath() {
		if (stateMachine.getCurrentState() != CharacterState.DEAD) {
			super.calculateNewPath();
		}
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		stateMachine.update();
	}

	public void handleStateCommand(CharacterState newState) {
		stateMachine.changeState(newState);
	}

	public CharacterState getCurrentMoveState() {
		return moveState;
	}

	public CharacterState getCurrentIdleState() {
		return moveState.idleState;
	}

}
