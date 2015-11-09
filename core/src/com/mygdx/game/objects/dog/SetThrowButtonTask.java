package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.mygdx.game.GameMessages;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.HumanCharacter;

/**
 * @author davebaol
 */
public class SetThrowButtonTask extends DogActionBase {

	@TaskAttribute(required=true)
	public boolean enabled;

	public SetThrowButtonTask () {
	}

	public void startAnimation(DogCharacter dog) {
	}

	@Override
	public void run () {
		HumanCharacter human = getObject().human;
		if (human.selected) {
			int msg = enabled ? GameMessages.GUI_SET_1ST_RADIO_BUTTON_TO_THROW : GameMessages.GUI_CLEAR_1ST_RADIO_BUTTON;
			MessageManager.getInstance().dispatchMessage(msg, human);
		}
		success();
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		SetThrowButtonTask stbTask = (SetThrowButtonTask)task;
		stbTask.enabled = enabled;
		return super.copyTo(task);
	}

}
