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

import com.mygdx.game.objects.DogCharacter.DogSteerSettings;

/**
 * An enumeration of task animations.
 * 
 * @author davebaol
 */
public enum TaskAnimation {
	Stand("armature|idle_stand"),
	Sit("armature|idle_sit"),
	LieDown("armature|idle_lie_down"),
	Piss("armature|action_piss"),
	SpinAround("armature|idle_search"),
	Walk("armature|move_walk", 0.7f, Stand) {
		@Override
		public float getSteeringMultiplier() {
			return 1f;
		}
	},
	Run("armature|move_run", 0.2f, Stand) {
		@Override
		public float getSteeringMultiplier() {
			return DogSteerSettings.runMultiplier;
		}
	};
	
	public final String animationId;
	public final float animationSpeedMultiplier;
	public final TaskAnimation idleTaskAnimation;
	
	/**
	 * Creates an idle task animation 
	 * @param animationId the animation id
	 */
	TaskAnimation(String animationId) {
		this(animationId, -1, null);
	}
	
	/**
	 * Creates a movement or idle task animation
	 * @param animationId the animation id
	 * @param animationSpeedMultiplier the animation speed multiplier
	 * @param idleTaskAnimation the animation to use when the speed is close enough to 0 (is {@code null} for idle task animations)
	 */
	TaskAnimation(String animationId, float animationSpeedMultiplier, TaskAnimation idleTaskAnimation) {
		this.animationId = animationId;
		this.animationSpeedMultiplier = animationSpeedMultiplier;
		this.idleTaskAnimation = idleTaskAnimation;
	}
	
	public float getSteeringMultiplier() {
		return -1;
	}
}
