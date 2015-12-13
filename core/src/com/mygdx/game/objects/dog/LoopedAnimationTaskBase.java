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
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.DogCharacter.DogSteerSettings;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.AnimationListener;

/**
 * The base class for dog's animated tasks. This class will use the animation defined by the task as long as the dog's speed is close to 0.
 * As soon as the dog moves a proper animation based on its linear speed will be used: {@link TaskAnimation#Run run} animation if 
 * speed is higher than {@link DogSteerSettings#maxLinearSpeed}; {@link TaskAnimation#Walk walk} animation otherwise. This allows idle tasks
 * to adapt silently the animation to variations in speed. The typical use case occurs when you enter an idle task while the dog is moving
 * because of some residual speed not consumed by a previous movement task.
 * 
 * @author davebaol
 */
public abstract class LoopedAnimationTaskBase extends LeafTask<DogCharacter> {

	protected AnimationListener animationListener;

	public LoopedAnimationTaskBase () {
		this(false);
	}

	public LoopedAnimationTaskBase (boolean useAnimationListener) {
		this.animationListener = useAnimationListener ? new AnimationListener() : null;
	}
	
	protected abstract TaskAnimation getTaskAnimation();

	protected void setSteeringLimits (DogCharacter dog, float steeringMultiplier) {
	}

	protected int getAnimationLoopCount () {
		return -1; // endless loop count
	}

	protected AnimationDesc startAnimation (DogCharacter dog) {
		int loopCount = -1;
//		if (animationListener != null && animationListener != dog.currentAnimationListener) {
		if (animationListener != null) {
			animationListener.setAnimationCompleted(false);
			loopCount = getAnimationLoopCount();
		}
		dog.currentAnimationListener = animationListener;
		return dog.animations.animate(dog.currentTaskAnimation.animationId, loopCount, 1, animationListener, 0.1f);
	}

	@Override
	public void start () {
		float steeringMultiplier = getTaskAnimation().getSteeringMultiplier();
		if (steeringMultiplier >= 0) {
			setSteeringLimits(getObject(), steeringMultiplier);
		}
	}

	protected void monitorAnimationTransition(DogCharacter dog, TaskAnimation ta) {
		if (dog.monitoredTaskAnimation != ta) {
			if (dog.currentTaskAnimation != null) {
				dog.monitoredTaskAnimation = ta;
				dog.switchAnimationTime = GdxAI.getTimepiece().getTime() + 0.2f;
				return;
			}
		}
		else if (dog.switchAnimationTime < GdxAI.getTimepiece().getTime()) {
			return;
		}

		// Start the new animation since the dog has maintained appropriate speed for long enough
		dog.currentTaskAnimation = ta;
		dog.monitoredTaskAnimation = null;
		dog.switchAnimationTime = -1;
		startAnimation(dog);
	}

	/**
	 * Switches the animation based on the linear speed, if needed.
	 * @param dog the dog
	 * @param speed the linear speed
	 */
	protected void switchAnimation(DogCharacter dog, float speed) {
		if (speed > DogSteerSettings.maxLinearSpeed + .2f) {
			if (dog.currentTaskAnimation != TaskAnimation.Run) {
				monitorAnimationTransition(dog, TaskAnimation.Run);
			}
		} else if (speed > dog.getZeroLinearSpeedThreshold() + .1f) {
			if (dog.currentTaskAnimation != TaskAnimation.Walk) {
				monitorAnimationTransition(dog, TaskAnimation.Walk);
			}
		} else {
			TaskAnimation taskAnimation = getTaskAnimation();
			if (taskAnimation.idleTaskAnimation != null) {
				taskAnimation = taskAnimation.idleTaskAnimation;
			}
			if (dog.currentTaskAnimation != taskAnimation) {
				monitorAnimationTransition(dog, taskAnimation);
			}
		}
	}
	
	protected void updateAnimation(DogCharacter dog) {
		// Switch animation based on linear velocity if needed
		float speed = dog.getLinearVelocity().len();
		switchAnimation(dog, speed);
		
		float deltaTime = Gdx.graphics.getDeltaTime();
		if (dog.currentTaskAnimation != null && dog.currentTaskAnimation.animationSpeedMultiplier >= 0) {
			deltaTime *= speed * dog.currentTaskAnimation.animationSpeedMultiplier;
		}
		dog.animations.update(deltaTime * GameSettings.GAME_SPEED);
	}

	@Override
	public Status execute () {
		updateAnimation(getObject());
		return Status.RUNNING;
	}

	@Override
	public void end() {
//		if (animationListener != null)
//			animationListener.animationFinished = true;
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		LoopedAnimationTaskBase dogTask = (LoopedAnimationTaskBase)task;
		dogTask.animationListener = animationListener == null? null : new AnimationListener();
		return task;
	}
}
