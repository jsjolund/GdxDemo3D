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

package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.HumanCharacter;
import com.mygdx.game.utilities.Constants;

/**
 * Action task that enables/disables the man's throw button.
 * 
 * @author davebaol
 */
public class SetThrowButtonTask extends LeafTask<DogCharacter> {

	@TaskAttribute(required=true)
	public boolean enabled;

	public SetThrowButtonTask () {
	}

	public void startAnimation(DogCharacter dog) {
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();
		HumanCharacter human = dog.human;
		if (human.selected && dog.humanWantToPlay) {
			int msg = enabled ? Constants.MSG_GUI_SET_DOG_BUTTON_TO_THROW : Constants.MSG_GUI_CLEAR_DOG_BUTTON;
			boolean sendTelegram = enabled && dog.humanWantToPlay && !dog.stickThrown;
			if (!enabled)
				sendTelegram = dog.humanWantToPlay && dog.stickThrown;
			if (sendTelegram) {
				MessageManager.getInstance().dispatchMessage(msg, human);
			}
		}
		return Status.SUCCEEDED;
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		SetThrowButtonTask stbTask = (SetThrowButtonTask)task;
		stbTask.enabled = enabled;
		return task;
	}

}
