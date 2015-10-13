package com.mygdx.game.components;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.mygdx.game.systems.RagdollSystem;

/**
 * Created by Johannes Sjolund on 10/10/15.
 */
public enum CharacterState implements State<CharacterStateComponent> {
	MOVE_RUN() {
		@Override
		public void enter(CharacterStateComponent entity) {
			// TODO: Set animation, sound, movement speed
			entity.controller.setAnimation("armature|move_run", -1);
			entity.pathCmp.moveSpeed = 8f;
			entity.idleState = IDLE_STAND;
			entity.isMoving = true;

		}
	},
	MOVE_WALK() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|move_walk", -1);
			entity.pathCmp.moveSpeed = 4f;
			entity.idleState = IDLE_STAND;
			entity.isMoving = true;

		}
	},
	MOVE_CROUCH() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|move_crouch", -1);
			entity.pathCmp.moveSpeed = 2.5f;
			entity.idleState = IDLE_CROUCH;
			entity.isMoving = true;

		}
	},
	MOVE_CRAWL() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|move_crouch", -1);
			entity.pathCmp.moveSpeed = 1f;
			entity.idleState = IDLE_CRAWL;
			entity.isMoving = true;

		}
	},
	IDLE_STAND() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|idle_stand", -1);
			entity.pathCmp.moveSpeed = 0;
			entity.isMoving = false;
		}
	},
	IDLE_CROUCH() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|idle_crouch", -1);
			entity.pathCmp.moveSpeed = 0;
			entity.isMoving = false;
		}
	},
	IDLE_CRAWL() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|idle_crouch", -1);
			entity.pathCmp.moveSpeed = 0;
			entity.isMoving = false;
		}
	},
	THROW() {
		@Override
		public void enter(CharacterStateComponent entity) {
			entity.controller.setAnimation("armature|action_throw");
			entity.pathCmp.moveSpeed = 0;
			entity.isMoving = false;
		}
	},
	DEAD() {
		@Override
		public void enter(CharacterStateComponent entity) {
			// Turn off animation, set ragdoll control
			entity.controller.setAnimation("armature|idle_stand", -1);
			entity.controller.paused = true;
			entity.pathCmp.moveSpeed = 0;
			entity.pathCmp.clearPath();
			entity.isMoving = false;
			RagdollSystem.toggle(true, entity.mdlCmp, entity.ragdollCmp, entity.phyCmp, entity.motionCmp);
		}

		@Override
		public void exit(CharacterStateComponent entity) {
			entity.controller.paused = false;
			RagdollSystem.toggle(false, entity.mdlCmp, entity.ragdollCmp, entity.phyCmp, entity.motionCmp);
		}
	},
	GLOBAL() {};

	@Override
	public void enter(CharacterStateComponent entity) {

	}

	@Override
	public void update(CharacterStateComponent entity) {
	}

	@Override
	public void exit(CharacterStateComponent entity) {
	}

	@Override
	public boolean onMessage(CharacterStateComponent entity, Telegram telegram) {
		return false;
	}


}
