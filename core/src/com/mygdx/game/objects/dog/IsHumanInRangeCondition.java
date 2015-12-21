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

/**
 * Condition task that checks if the dog's owner is within a given radius in meters.
 * 
 * @author davebaol
 */
public class IsHumanInRangeCondition extends LeafTask<DogCharacter> {

	@TaskAttribute(required=true)
	public float meters;
	
	public IsHumanInRangeCondition () {
		this(20);
	}
	
	public IsHumanInRangeCondition (float meters) {
		this.meters = meters;
	}

	@Override
	public Status execute () {
		return getObject().isHumanCloseEnough(meters)? Status.SUCCEEDED : Status.FAILED;
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		((IsHumanInRangeCondition)task).meters = meters;
		return task;
	}

}
