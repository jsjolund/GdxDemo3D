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
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogState;

/**
 * Action task that makes the dog's state machine switch to the specified state (behavior tree).
 * 
 * @author davebaol
 */
public class StateMachineTransition extends LeafTask<DogCharacter> {
	
	@TaskAttribute(required=true, name="to")
	public DogState state;

	public StateMachineTransition() {
		state = DogState.ACT_ON_YOUR_OWN;
	}

	@Override
	public Status execute() {
		DogCharacter dog = getObject();
		dog.bTreeSwitchFSM.changeState(state);
		return  Status.SUCCEEDED;
	}

	@Override
	protected Task<DogCharacter> copyTo(Task<DogCharacter> task) {
		((StateMachineTransition)task).state = state;
		return task;
	}

}
