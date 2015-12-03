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
 * The base class for dog's movement animated tasks. It defines the task attribute {@link #gait} that allows you to specify
 * how the dog should move, i.e. the animation to use. Typical values are {@link TaskAnimation#Walk} and {@link TaskAnimation#Run}
 * but there's nothing stopping you from using any of the other values defined by the {@link TaskAnimation} enumeration, although
 * most of them are not suitable for movement.
 * 
 * @author davebaol
 */
public abstract class MovementAnimationTaskBase extends LoopedAnimationTaskBase {

	@TaskAttribute(required=true)
	public TaskAnimation gait;
	
	public MovementAnimationTaskBase () {
		this.gait = TaskAnimation.Walk;
	}
	
	@Override
	protected TaskAnimation getTaskAnimation() {
		return gait;
	}

	@Override
	protected void setSteeringLimits (DogCharacter dog, float steeringMultiplier) {
		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed * steeringMultiplier);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration * steeringMultiplier);
		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed * steeringMultiplier);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration * steeringMultiplier);
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		MovementAnimationTaskBase thisTask = (MovementAnimationTaskBase)task;
		thisTask.gait = gait;
		return super.copyTo(task);
	}

}
