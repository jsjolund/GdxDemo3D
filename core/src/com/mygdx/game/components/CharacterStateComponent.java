package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

/**
 * Created by Johannes Sjolund on 10/11/15.
 */
public class CharacterStateComponent implements Component {

	public final StateMachine<CharacterStateComponent> stateMachine;
	public final AnimationController controller;
	public final CharacterAnimationListener animationListener;

	public CharacterState moveState = CharacterState.MOVE_WALK;
	public CharacterState idleState = CharacterState.IDLE_STAND;

	public PathFindingComponent pathCmp;
	public ModelComponent mdlCmp;
	public SelectableComponent selCmp;
	public RagdollComponent ragdollCmp;
	public PhysicsComponent phyCmp;
	public MotionStateComponent motionCmp;

	public boolean isMoving = false;

	public CharacterStateComponent(
			ModelComponent mdlCmp,
			PathFindingComponent pathCmp,
			SelectableComponent selCmp,
			RagdollComponent ragdollCmp,
			PhysicsComponent phyCmp,
			MotionStateComponent motionCmp) {

		this.mdlCmp = mdlCmp;
		this.motionCmp = motionCmp;
		this.pathCmp = pathCmp;
		this.phyCmp = phyCmp;
		this.ragdollCmp = ragdollCmp;
		this.selCmp = selCmp;

		controller = new AnimationController(mdlCmp.modelInstance);
		animationListener = new CharacterAnimationListener();
		stateMachine = new DefaultStateMachine<CharacterStateComponent>(this, CharacterState.GLOBAL);
		stateMachine.changeState(CharacterState.IDLE_STAND);

	}

	public void update(float delta) {
		stateMachine.update();
		controller.update(delta);

		if (pathCmp.goalReached && isMoving) {
			stateMachine.changeState(idleState);
		} else if (!pathCmp.goalReached && !isMoving) {
			stateMachine.changeState(moveState);
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
