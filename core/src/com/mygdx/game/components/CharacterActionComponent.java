package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 8/30/15.
 */
public class CharacterActionComponent extends Component {

	public CharacterActionComponent(ModelInstance modelInstance) {
		controllers = new Array<AnimationController>();
		addModel(modelInstance);
	}

	public void addModel(ModelInstance modelInstance) {
		AnimationController controller = new AnimationController(modelInstance);
		controller.setAnimation(currentAction.animationId, -1);
		controllers.add(controller);
	}

	public Array<AnimationController> controllers;

	public Action nextAction = Action.IDLE;
	public Action currentAction = Action.IDLE;

	public enum Action {
		WALK("Armature|walk"),
		RUN("Armature|run"),
		IDLE("Armature|stand");

		public final String animationId;

		Action(String animationName) {
			this.animationId = animationName;
		}
	}

}
