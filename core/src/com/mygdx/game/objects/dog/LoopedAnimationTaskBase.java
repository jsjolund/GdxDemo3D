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
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.AnimationListener;

/**
 * @author davebaol
 */
public abstract class LoopedAnimationTaskBase extends LeafTask<DogCharacter> {

	protected float animationSpeedMultiplier;
	protected AnimationListener animationListener;

	public LoopedAnimationTaskBase () {
		this(-1, false);
	}

	public LoopedAnimationTaskBase (float animationSpeedMultiplier) {
		this(animationSpeedMultiplier, false);
	}

	public LoopedAnimationTaskBase (float animationSpeedMultiplier, boolean useAnimationListener) {
		this.animationSpeedMultiplier = animationSpeedMultiplier;
		this.animationListener = useAnimationListener ? new AnimationListener() : null;
	}

	protected abstract void startAnimation(DogCharacter dog);

	protected void updateAnimation(DogCharacter dog) {
		// update current animation
		float deltaTime = Gdx.graphics.getDeltaTime();
		if (animationSpeedMultiplier >= 0) {
			// this is a movement task
			deltaTime *= dog.getLinearVelocity().len() * animationSpeedMultiplier;
		}
		dog.animations.update(deltaTime * GameSettings.GAME_SPEED);
	}

	@Override
	public void start() {
		if (animationListener != null)
			animationListener.setAnimationCompleted(false);
		startAnimation(getObject());
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
		dogTask.animationSpeedMultiplier = animationSpeedMultiplier;
		dogTask.animationListener = animationListener == null? null : new AnimationListener();
		return task;
	}
}
