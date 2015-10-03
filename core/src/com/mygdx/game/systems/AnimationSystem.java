package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.mygdx.game.components.CharacterActionComponent;

/**
 * Created by user on 8/30/15.
 */
public class AnimationSystem extends IteratingSystem {

	private final ComponentMapper<CharacterActionComponent> actionCmps =
			ComponentMapper.getFor(CharacterActionComponent.class);

	public AnimationSystem(Family family) {
		super(family);
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		CharacterActionComponent actionCmp = actionCmps.get(entity);
		if (actionCmp.nextAction == CharacterActionComponent.Action.NULL || actionCmp.ragdollControl) {
			for (AnimationController controller : actionCmp.controllers) {
				controller.paused = true;
			}
			actionCmp.currentAction = CharacterActionComponent.Action.NULL;

		} else if (actionCmp.currentAction != actionCmp.nextAction) {
			for (AnimationController controller : actionCmp.controllers) {
				controller.paused = false;
				controller.setAnimation(actionCmp.nextAction.animationId, -1);
			}
			actionCmp.currentAction = actionCmp.nextAction;
		}
		for (AnimationController controller : actionCmp.controllers) {
			controller.update(deltaTime);
		}

	}
}
