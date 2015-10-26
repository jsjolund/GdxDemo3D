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
				entity.animations.setAnimation("armature|move_run", -1);
				entity.idleState = IDLE_STAND;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed * SteerSettings.runMultiplier);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration * SteerSettings.runMultiplier);
			}

		},
		MOVE_WALK() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|move_walk", -1);
				entity.idleState = IDLE_STAND;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration);
			}

			@Override
			public void update(GameCharacter entity) {
				float deltaTime = Gdx.graphics.getDeltaTime() * entity.getLinearVelocity().len() * 0.25f;
				entity.animations.update(deltaTime * GameSettings.GAME_SPEED);
			}
		},
		MOVE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|move_crouch", -1);
				entity.idleState = IDLE_CROUCH;

				entity.setMaxLinearSpeed(SteerSettings.maxLinearSpeed * SteerSettings.crouchMultiplier);
				entity.setMaxLinearAcceleration(SteerSettings.maxLinearAcceleration * SteerSettings.crouchMultiplier);
			}
		},
		MOVE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|move_crouch", -1);
				entity.idleState = IDLE_CRAWL;
			}
		},
		IDLE_STAND() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|idle_stand", -1);
			}
		},
		IDLE_CROUCH() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|idle_crouch", -1);
			}
		},
		IDLE_CRAWL() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|idle_crouch", -1);
			}
		},
		THROW() {
			@Override
			public void enter(GameCharacter entity) {
				entity.animations.setAnimation("armature|action_throw");
			}
		},
		DEAD() {
			@Override
			public void enter(GameCharacter entity) {
				// Turn off animation, set ragdoll control
				entity.animations.setAnimation("armature|idle_stand", -1);
				entity.animations.paused = true;
				entity.toggle(true);
			}

			@Override
			public void exit(GameCharacter entity) {
				entity.animations.paused = false;
				entity.toggle(false);
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
	private float currentFriction = SteerSettings.fricion;
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

		super(model, id, location, rotation, scale, shape, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate, ragdollJson, armatureNodeId);

		animations = new AnimationController(modelInstance);
		animationListener = new CharacterAnimationListener();
		stateMachine = new DefaultStateMachine<GameCharacter>(this, CharacterState.GLOBAL);
		stateMachine.changeState(idleState);

		body.setAngularFactor(Vector3.Y);
		body.setFriction(SteerSettings.fricion);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		stateMachine.update();
		boolean isSteering = isSteering();
		if (isSteering && !wasSteering) {
			stateMachine.changeState(moveState);
			wasSteering = isSteering;
		} else if (!isSteering && wasSteering) {
			stateMachine.changeState(idleState);
			wasSteering = isSteering;
		}

		if (SteerSettings.fricion != currentFriction) {
			body.setFriction(SteerSettings.fricion);
			currentFriction = SteerSettings.fricion;
		}
		setZeroLinearSpeedThreshold(SteerSettings.zeroLinearSpeedThreshold);
	}
}
