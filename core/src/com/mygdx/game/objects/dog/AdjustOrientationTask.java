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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.utils.ArithmeticUtils;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.utilities.Constants;
import com.mygdx.game.utilities.Sounds;

/**
 * @author davebaol
 */
public class AdjustOrientationTask extends OneShotAnimationTaskBase {
	
	float stopTime;
	float orientationDiff;

	public AdjustOrientationTask () {
	}

	@Override
	protected TaskAnimation getTaskAnimation () {
		return TaskAnimation.SpinAround;
	}

	@Override
	public AnimationDesc startAnimation(DogCharacter dog) {
		AnimationDesc animationDesc = super.startAnimation(dog);
		stopTime = -1;
		if (dog.currentTaskAnimation == getTaskAnimation()) {
			Gdx.app.log(getClass().getSimpleName(), "--------- Adjust dog orientation ----------");
			Gdx.app.log(getClass().getSimpleName(), "human.orientation = " + (MathUtils.radiansToDegrees * dog.human.getOrientation()));
			Gdx.app.log(getClass().getSimpleName(), "dog.orientation   = " + (MathUtils.radiansToDegrees * dog.getOrientation()));
			orientationDiff = dog.getOrientation() - dog.human.getOrientation();
			Gdx.app.log(getClass().getSimpleName(), "orientationDiff = " + (MathUtils.radiansToDegrees * orientationDiff));
			float wrappedOD = orientationDiff + Constants.PI;
			Gdx.app.log(getClass().getSimpleName(), "wrappedOD = " + (MathUtils.radiansToDegrees * wrappedOD));
			float startTime = GdxAI.getTimepiece().getTime();
			stopTime = startTime + animationDesc.duration * wrappedOD / Constants.PI2;
			Gdx.app.log(getClass().getSimpleName(), "animationDesc.totalDuration = " + animationDesc.duration);
			Gdx.app.log(getClass().getSimpleName(), "animationDesc.playDuration = " + (stopTime - startTime));
		}
		return animationDesc;
	}

	@Override
	public void start () {
		stopTime = -1;
		super.start();
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();
		if (stopTime == -1 || GdxAI.getTimepiece().getTime() < stopTime + .2f) {
			updateAnimation(dog);
			return Status.RUNNING;
		}
		Sounds.bark.play();
		float newOrientation = ArithmeticUtils.wrapAngleAroundZero(dog.getOrientation() - orientationDiff  + Constants.PI);
		dog.setOrientation(newOrientation);
		Gdx.app.log(getClass().getSimpleName(), "dog.orientation   = " + (MathUtils.radiansToDegrees * dog.getOrientation()));
		dog.animations.animate("armature|idle_stand", 1, 1, animationListener, 0.1f);
		updateAnimation(dog);
		return Status.SUCCEEDED;
	}

}
