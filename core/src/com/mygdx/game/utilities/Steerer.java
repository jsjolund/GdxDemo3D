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

package com.mygdx.game.utilities;

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.GameRenderer;
import com.mygdx.game.objects.SteerableBody;

/**
 * The base class for steerers. Typically, a steerer gathers some steering behaviors and makes them cooperate somehow
 * in order to create more complex behaviors. 
 * 
 * @author davebaol
 */
public abstract class Steerer {

	protected final SteerableBody steerableBody;

	/**
	 * Create a {@code Steerer} for the given steerable body.
	 * @param steerableBody the steerable body
	 */
	public Steerer(SteerableBody steerableBody) {
		this.steerableBody = steerableBody;
	}

	/**
	 * Calculate the steering acceleration produced by this steerer. 
	 * @param steering the output steering acceleration
	 * @return {@code false} if steering has completed (for instance the end of the path has been reached); {@code true} otherwise 
	 */
	public boolean calculateSteering (SteeringAcceleration<Vector3> steering) {
		return processSteering(getSteeringBehavior().calculateSteering(steering));
	}

	/**
	 * Returns the steering behavior of this steerer, usually a priority or a blended steering grouping other steering behaviors. 
	 */
	public abstract SteeringBehavior<Vector3> getSteeringBehavior();

	/**
	 * Called by {@link Steerer#calculateSteering(SteeringAcceleration)} to give this steerer the chance to:
	 * <ul>
	 * <li>decide whether this steerer has completed, for instance the end of the path has been reached</li> 
	 * <li>manipulate the acceleration produced by the steering behavior of this steerer</li> 
	 * </ul>
	 * @param steering the input/output steering acceleration
	 * @return {@code false} if steering has completed; {@code true} otherwise 
	 */
	public abstract boolean processSteering(SteeringAcceleration<Vector3> steering);

	/**
	 * Called by {@link SteerableBody#startSteering()} to give this steerer the chance to prepare resources, for instance. 
	 */
	public abstract void startSteering();

	/**
	 * Called by {@link SteerableBody#stopSteering()} to give this steerer the chance to release resources, for instance. 
	 * @return {@code true} if the linear velocity must be cleared; {@code false} otherwise 
	 */
	public abstract boolean stopSteering();

	/**
	 * Draws the debug info of this steerer.
	 * @param gameRenderer the renderer
	 */
	public abstract void draw(GameRenderer gameRenderer);

}
