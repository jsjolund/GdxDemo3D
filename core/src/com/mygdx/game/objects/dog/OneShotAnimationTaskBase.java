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
 * The base class for dog's animated tasks that must last for one animation loop.
 * The task keeps running until the animation is completed, which finally makes the task succeed. 
 * 
 * @author davebaol
 */
public abstract class OneShotAnimationTaskBase extends LoopedAnimationTaskBase {
	
	public OneShotAnimationTaskBase () {
		super(true); // Use an animation listener
	}

	@Override
	protected int getAnimationLoopCount () {
		return 1; // one shot
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();
		updateAnimation(dog);
		return animationListener == dog.currentAnimationListener && animationListener.isAnimationCompleted()? Status.SUCCEEDED : Status.RUNNING;
	}

}
