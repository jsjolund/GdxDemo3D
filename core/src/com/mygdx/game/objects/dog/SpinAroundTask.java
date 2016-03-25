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


/**
 * A one shot animation task that makes the dog spin around.
 * 
 * @author davebaol
 */
public class SpinAroundTask extends OneShotAnimationTaskBase {

	public SpinAroundTask () {
	}

	@Override
	protected TaskAnimation getTaskAnimation () {
		return TaskAnimation.SpinAround;
	}
	
	@Override
	public void end() {
		// Cleanly stop spinning around if task gets cancelled
		if (mustTruncateAnimationCleanly()) {
			truncateAnimationCleanly(getObject());
		}
		super.end();
	}

	protected boolean mustTruncateAnimationCleanly() {
		return getStatus() == Status.CANCELLED;
	}

	protected void truncateAnimationCleanly(DogCharacter dog) {
		// Get current model orientation
		float currentDogOrientation = dog.getBoneOrientation(DogCharacter.DogArmature.FRONT_SPINE.id);

		// Finish animation
		truncateAnimationCleanly(dog, currentDogOrientation);
	}

	protected void truncateAnimationCleanly(DogCharacter dog, float orientation) {
		// Set body and model orientation since the body does not rotate during spin around animation 
		dog.setOrientation(orientation);
		
		// Set stand animation
		// Notice that we have to change the animation instantaneously to avoid the rapid but still
		// noticeable rotation effect due to animation blending occurring when transitionTime > 0 
		dog.animations.animate(TaskAnimation.Stand.animationId, 1, 1, animationListener, 0.0f);
		updateAnimation(dog);
	}

}
