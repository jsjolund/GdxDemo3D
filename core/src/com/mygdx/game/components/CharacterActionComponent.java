package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.utils.Array;

/**
 * Created by user on 8/30/15.
 */
public class CharacterActionComponent extends Component {

	public final Array<AnimationController> controllers;
	public Action nextAction = Action.IDLE;
	public Action currentAction = Action.NULL;
	public boolean ragdollControl = false;

	public CharacterActionComponent(ModelInstance modelInstance) {
		controllers = new Array<AnimationController>();
		addModel(modelInstance);
	}

	public void addModel(ModelInstance modelInstance) {
		AnimationController controller = new AnimationController(modelInstance);
		controller.setAnimation(currentAction.animationId, -1);
		controllers.add(controller);
	}

	public enum Action {
		WALK("armature|walk"),
		WALK_RIFLE("armature|walk_rifle"),
		RUN("armature|run"),
		IDLE("armature|stand"),
		NULL(null);

		public final String animationId;

		Action(String animationName) {
			this.animationId = animationName;
		}
	}

}
