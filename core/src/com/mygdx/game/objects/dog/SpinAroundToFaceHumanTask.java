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

import com.badlogic.gdx.ai.utils.ArithmeticUtils;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.GameScreen;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.utilities.Constants;

/**
 * A one shot animation task that makes the dog spin around until it's facing its owner.
 * 
 * @author davebaol
 */
public class SpinAroundToFaceHumanTask extends SpinAroundTask {

	private static final float ORIENTATION_TOLERANCE = 8 * MathUtils.degreesToRadians; // 8 degrees tolerance

	private float targetOrientation;
	private boolean facingHuman;

	public SpinAroundToFaceHumanTask () {
	}

	@Override
	public AnimationDesc startAnimation(DogCharacter dog) {
		AnimationDesc animationDesc = super.startAnimation(dog);
		if (dog.currentTaskAnimation == getTaskAnimation()) {
			// Calculate target orientation to make the dog face human
			targetOrientation = ArithmeticUtils.wrapAngleAroundZero(dog.human.getOrientation() + Constants.PI);
		}
		return animationDesc;
	}

	@Override
	public void start() {
		// Init the flag indicating whether the task has to succeed
		facingHuman = false;
		super.start();
	}

	@Override
	public Status execute () {
		if (facingHuman) {
			return Status.SUCCEEDED;
		}
		DogCharacter dog = getObject();
		updateAnimation(dog);
		if (dog.currentTaskAnimation == getTaskAnimation()) {
			// Get current model orientation
			float currentDogOrientation = dog.getBoneOrientation(DogCharacter.DogArmature.FRONT_SPINE.id);
			
			// Calculate the difference between current and target orientation
			float orientationDiff = ArithmeticUtils.wrapAngleAroundZero(currentDogOrientation - targetOrientation);
			
			// Is dog facing human with enough precision?
			if (MathUtils.isZero(orientationDiff, ORIENTATION_TOLERANCE)) {
				// Make the task succeed on the next frame
				facingHuman = true;

				// Bark
				GameScreen.screen.sounds.bark.play();

				// Finish the animation
				truncateAnimationCleanly(dog, currentDogOrientation);
			}
		}
		return Status.RUNNING;
	}

	@Override
	protected boolean mustTruncateAnimationCleanly() {
		return !facingHuman;
	}

}
