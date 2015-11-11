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

import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * @author davebaol
 */
public class WalkTask extends DogTaskBase {

	public WalkTask () {
		super(0.7f);
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		dog.animations.animate("armature|move_walk", -1, 1, dog.animationListener, 0.1f);

		dog.setMaxLinearSpeed(DogSteerSettings.maxLinearSpeed);
		dog.setMaxLinearAcceleration(DogSteerSettings.maxLinearAcceleration);

		dog.setMaxAngularSpeed(DogSteerSettings.maxAngularSpeed);
		dog.setMaxAngularAcceleration(DogSteerSettings.maxAngularAcceleration);

		if (dog.followPathSB != null) {
			dog.followPathSB.setDecelerationRadius(DogSteerSettings.decelerationRadius);
		}
	}

}
