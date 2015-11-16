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

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.utils.ArithmeticUtils;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.math.MathUtils;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.utilities.Constants;

/**
 * @author davebaol
 */
public class AdjustOrientationTask extends OneShotAnimationTaskBase {
	
	float stopTime;
	float orientationDiff;

	public AdjustOrientationTask () {
	}

	@Override
	public void startAnimation(DogCharacter dog) {
		AnimationDesc animationDesc = dog.animations.animate("armature|idle_search", 1, 1, animationListener, 0.1f);
		System.out.println("------------------------------");
		System.out.println("human.orientation = " + (MathUtils.radiansToDegrees * dog.human.getOrientation()));
		System.out.println("dog.orientation   = " + (MathUtils.radiansToDegrees * dog.getOrientation()));
		orientationDiff = ArithmeticUtils.wrapAngleAroundZero(dog.human.getOrientation() - dog.getOrientation() + Constants.PI);
		if (orientationDiff < 0) {
			orientationDiff += Constants.PI2;
		}
		System.out.println("orientationDiff = " + (MathUtils.radiansToDegrees * orientationDiff));
		stopTime = GdxAI.getTimepiece().getTime() + animationDesc.duration * orientationDiff / Constants.PI2;
		System.out.println("animationDesc.duration = " + animationDesc.duration);
		System.out.println("startTime = " + GdxAI.getTimepiece().getTime());
		System.out.println("stopTime = " + stopTime);
	}

	@Override
	public Status execute () {
		DogCharacter dog = getObject();
		if (GdxAI.getTimepiece().getTime() < stopTime) {
			updateAnimation(dog);
			return Status.RUNNING;
		}
		float ori = dog.getOrientation() + orientationDiff;
		dog.setOrientation(ori);
		System.out.println("dog.orientation   = " + (MathUtils.radiansToDegrees * dog.getOrientation()));
		dog.animations.animate("armature|idle_stand", -1, 1, animationListener, 0.1f);
		updateAnimation(dog);
		return Status.SUCCEEDED;
	}

	@Override
	public void end () {
		super.end();
//		DogCharacter dog = getObject();
//		dog.animations.animate("armature|idle_stand", -1, 1, animationListener, 0.1f);
//		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		//dog.currentAnimationFinished = false;
	}

}
