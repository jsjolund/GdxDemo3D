package com.mygdx.game.objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.settings.GameSettings;

/**
 * Created by Johannes Sjolund on 11/6/15.
 */
public class DogCharacter extends GameCharacter {

	public enum CharacterState implements State<DogCharacter> {
		MOVE_RUN() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|move_run", -1, 1, entity.animationListener, 0.1f);

				entity.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed * DogSteerSettings.runMultiplier);
				entity.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration * DogSteerSettings.runMultiplier);

				entity.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed * DogSteerSettings.runMultiplier);
				entity.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration * DogSteerSettings.runMultiplier);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius * DogSteerSettings.runMultiplier);
				}
			}

			@Override
			public void update(DogCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.2f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		MOVE_WALK() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|move_walk", -1, 1, entity.animationListener, 0.1f);

				entity.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed);
				entity.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration);

				entity.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed);
				entity.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration);

				if (entity.followPathSB != null) {
					entity.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius);
				}
			}

			@Override
			public void update(DogCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.7f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		IDLE_STAND() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|idle_stand", -1, 1, entity.animationListener, 0.1f);
			}
		},
		IDLE_SIT() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|idle_sit", -1, 1, entity.animationListener, 0.1f);
			}
		},
		IDLE_SEARCH() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|idle_search", -1, 1, entity.animationListener, 0.1f);
			}
		},
		IDLE_LIE_DOWN() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|idle_lie_down", -1, 1, entity.animationListener, 0.1f);
			}
		},
		ACTION_PISS() {
			@Override
			public void enter(DogCharacter entity) {
				entity.animations.animate("armature|action_piss", -1, 1, entity.animationListener, 0.1f);
			}
		},
		GLOBAL() {};

		public static <T extends Enum<?>> T randomMoveState(Class<T> clazz) {
			int x = MathUtils.random(0, 1);
			return clazz.getEnumConstants()[x];
		}

		public static <T extends Enum<?>> T randomIdleState(Class<T> clazz) {
			int x = MathUtils.random(2, 6);
			return clazz.getEnumConstants()[x];
		}

		@Override
		public void enter(DogCharacter entity) {

		}

		@Override
		public void update(DogCharacter entity) {
			float deltaTime = Gdx.graphics.getDeltaTime();
			entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
		}

		@Override
		public void exit(DogCharacter entity) {

		}

		@Override
		public boolean onMessage(DogCharacter entity, Telegram telegram) {
			return false;
		}
	}

	public static class DogSteerSettings implements SteerSettings {
		public static float maxLinearAcceleration = 50f;
		public static float maxLinearSpeed = 2f;
		public static float maxAngularAcceleration = 100f;
		public static float maxAngularSpeed = 15f;
		public static float idleFriction = 0.9f;
		public static float zeroLinearSpeedThreshold = 0.001f;
		public static float runMultiplier = 3f;
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

	public final StateMachine<DogCharacter> stateMachine;
	public final AnimationController animations;
	public final CharacterAnimationListener animationListener;
	private boolean wasSteering = false;

	//	private CharacterState moveState = CharacterState.MOVE_WALK;
	private CharacterState moveState = CharacterState.MOVE_RUN;
	private CharacterState idleState = CharacterState.IDLE_STAND;

	public DogCharacter(Model model, String id,
						Vector3 location, Vector3 rotation, Vector3 scale,
						btCollisionShape shape, float mass,
						short belongsToFlag, short collidesWithFlag,
						boolean callback, boolean noDeactivate) {
		super(model, id,
				location, rotation, scale,
				shape, mass,
				belongsToFlag, collidesWithFlag,
				callback, noDeactivate,
				new DogSteerSettings());

		body.setAngularFactor(Vector3.Y);

		animations = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();

		stateMachine = new DefaultStateMachine<DogCharacter>(this, CharacterState.GLOBAL);

		stateMachine.changeState(moveState);
		stateMachine.changeState(idleState);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		stateMachine.update();

		boolean isSteering = isSteering();
		if (isSteering && !wasSteering) {
			wasSteering = isSteering;
			stateMachine.changeState(moveState.randomMoveState(CharacterState.class));

		} else if (!isSteering && wasSteering) {
			wasSteering = isSteering;
			stateMachine.changeState(idleState.randomIdleState(CharacterState.class));
		}
	}
}
