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
 * @author davebaol
 */
public abstract class Steerer {

	protected final SteerableBody steerableBody;

	public Steerer(SteerableBody steerableBody) {
		this.steerableBody = steerableBody;
	}

	public boolean calculateSteering (SteeringAcceleration<Vector3> steering) {
		return processSteering(getSteeringBehavior().calculateSteering(steering));
	}

	public abstract SteeringBehavior<Vector3> getSteeringBehavior();

	public abstract boolean processSteering(SteeringAcceleration<Vector3> steering);

	public abstract void startSteering();

	public abstract void stopSteering();

	public abstract void draw(GameRenderer gameRenderer);

}
