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
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * @author davebaol
 */
public class RunTask extends LoopedAnimationTaskBase {

	public RunTask () {
		super(0.2f, true);
	}

	@Override
	protected void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|move_run", -1, 1, animationListener, 0.1f);

		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed * DogSteerSettings.runMultiplier);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration * DogSteerSettings.runMultiplier);

		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed * DogSteerSettings.runMultiplier);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration * DogSteerSettings.runMultiplier);

		dog.followPathSteerer.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius * DogSteerSettings.runMultiplier);
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();
		updateAnimation(dog);
		if (getStatus() == Task.Status.RUNNING && !dog.isSteering()) {
			return Status.SUCCEEDED;
		}
		return Status.RUNNING;
	}

}
