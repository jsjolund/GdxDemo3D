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

package com.mygdx.game.steerers;

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.Wander;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.GameRenderer;
import com.mygdx.game.objects.SteerableBody;
import com.mygdx.game.utilities.MyShapeRenderer;

/**
 * A steerer to wander while avoiding collisions. 
 * 
 * @author davebaol
 */
public class WanderSteerer extends CollisionAvoidanceSteererBase {

	final Wander<Vector3> wanderSB;
	boolean keepWandering;
	
	public WanderSteerer(final SteerableBody steerableBody) {
		super(steerableBody);

		this.wanderSB = new Wander<Vector3>(steerableBody) {
			@Override
			protected SteeringAcceleration<Vector3> calculateRealSteering(SteeringAcceleration<Vector3> steering) {
				super.calculateRealSteering(steering);
				steering.linear.y = 0; // remove any vertical acceleration
				return steering;
			}
		};
		this.wanderSB.setWanderOffset(8) //
			.setWanderOrientation(0) //
			.setWanderRadius(0.5f) //
			.setWanderRate(MathUtils.PI2 * 4);

		this.prioritySteering.add(wanderSB);
	}

	public void startWandering() {
		steerableBody.steerer = this;
		keepWandering = true;
	}

	public void stopWandering() {
		keepWandering = false;
	}
	
	@Override
	public boolean processSteering (SteeringAcceleration<Vector3> steering) {
		return keepWandering;
	}

	@Override
	public void startSteering () {
	}

	@Override
	public boolean stopSteering () {
		return false;
	}
	
	@Override
	public void draw(GameRenderer gameRenderer) {
		super.draw(gameRenderer);
		
		MyShapeRenderer shapeRenderer = gameRenderer.shapeRenderer;

		// Draw wander circle
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.CORAL);
		Vector3 wanderCenter = wanderSB.getWanderCenter();
		shapeRenderer.circle3(wanderCenter.x, wanderCenter.y - steerableBody.halfExtents.y, wanderCenter.z, wanderSB.getWanderRadius(), 12);
		shapeRenderer.end();

		// Draw wander target
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.CORAL);
		Vector3 wanderTarget = wanderSB.getInternalTargetPosition();
		shapeRenderer.circle3(wanderTarget.x, wanderTarget.y - steerableBody.halfExtents.y, wanderTarget.z, .1f, 6);
		shapeRenderer.end();
	}

}
