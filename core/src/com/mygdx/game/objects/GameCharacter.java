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
import com.mygdx.game.settings.SteerSettings;

import java.util.EnumMap;

/**
 * @author jsjolund
 */
public class GameCharacter extends Ragdoll {

	public enum CharacterState implements State<GameCharacter> {
		MOVE_RUN() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.animate("armature|move_run", -1, 1, entity.animationListener, 0.1f);
				entity.moveState = this;
				entity.idleState = entity.moveIdleMap.get(this);

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
				entity.moveState = this;
				entity.idleState = entity.moveIdleMap.get(this);

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
				entity.moveState = this;
				entity.idleState = entity.moveIdleMap.get(this);

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
		MOVE_CRAWL() {},
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
				entity.steeringBehavior = null;
				entity.navMeshPointPath.clear();
				entity.navMeshGraphPath.clear();
				entity.finishSteering();
				entity.body.setFriction(1);
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
	private CharacterState moveState = CharacterState.MOVE_WALK;
	private CharacterState idleState = CharacterState.IDLE_STAND;
	private EnumMap<CharacterState, CharacterState> moveIdleMap =
			new EnumMap<CharacterState, CharacterState>(CharacterState.class);
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
		// Only allow physics engine to turn player capsule around the up axis
		body.setAngularFactor(Vector3.Y);

		animations = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();

		moveIdleMap.put(CharacterState.MOVE_WALK, CharacterState.IDLE_STAND);
		moveIdleMap.put(CharacterState.MOVE_RUN, CharacterState.IDLE_STAND);
		moveIdleMap.put(CharacterState.MOVE_CROUCH, CharacterState.IDLE_CROUCH);

		stateMachine = new DefaultStateMachine<GameCharacter>(this, CharacterState.GLOBAL);
		// Set the steering variables associated with default move state (walking)
		stateMachine.changeState(moveState);
		// Then make the character idle
		stateMachine.changeState(idleState);
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

	public void handleStateCommand(CharacterState newState) {
		boolean isSteering = isSteering();

		if (newState == CharacterState.DEAD) {
			stateMachine.changeState(newState);
		} else if (isSteering) {
			stateMachine.changeState(newState);
		} else if (moveIdleMap.containsKey(newState)) {
			moveState = newState;
			idleState = moveIdleMap.get(newState);
			stateMachine.changeState(idleState);
		}
	}


	public CharacterState getCurrentMoveState() {
		return moveState;
	}

	public CharacterState getCurrentIdleState() {
		return idleState;
	}
}
