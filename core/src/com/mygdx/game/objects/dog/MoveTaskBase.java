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

import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * @author davebaol
 */
public abstract class MoveTaskBase extends LoopedAnimationTaskBase {

	@TaskAttribute(required=true)
	public Gait gait;
	
	public MoveTaskBase () {
		this.gait = Gait.Walk;
	}

	@Override
	protected void startAnimation (DogCharacter dog) {
		this.animationSpeedMultiplier = gait.animationSpeedMultiplier;
		dog.animations.animate(gait.animationId, -1, 1, animationListener, 0.1f);

		float steeringMultiplier = gait.getSteeringMultiplier();
		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed * steeringMultiplier);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration * steeringMultiplier);
		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed * steeringMultiplier);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration * steeringMultiplier);
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		MoveTaskBase wanderTask = (MoveTaskBase)task;
		wanderTask.gait = gait;
		return super.copyTo(task);
	}

}
