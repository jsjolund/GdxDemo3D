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
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.CollisionAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.GameRenderer;
import com.mygdx.game.GameScreen;
import com.mygdx.game.objects.SteerableBody;
import com.mygdx.game.utilities.MyShapeRenderer;
import com.mygdx.game.utilities.Steerer;

/**
 * Base class for steerers requiring collision avoidance behavior with priority. 
 * 
 * @author davebaol
 */
public abstract class CollisionAvoidanceSteererBase extends Steerer {

	protected final CollisionAvoidance<Vector3> collisionAvoidanceSB;
	protected final RadiusProximity<Vector3> proximity;

	protected final PrioritySteering<Vector3> prioritySteering;

	public CollisionAvoidanceSteererBase(final SteerableBody steerableBody) {
		super(steerableBody);

		this.proximity = new RadiusProximity<Vector3>(steerableBody, GameScreen.screen.engine.characters, steerableBody.getBoundingRadius() * 1.8f);
		this.collisionAvoidanceSB = new CollisionAvoidance<Vector3>(steerableBody, proximity) {
			@Override
			protected SteeringAcceleration<Vector3> calculateRealSteering(SteeringAcceleration<Vector3> steering) {
				super.calculateRealSteering(steering);
				steering.linear.y = 0; // remove any vertical acceleration
				return steering;
			}
		};

		this.prioritySteering = new PrioritySteering<Vector3>(steerableBody, 0.001f) //
			.add(collisionAvoidanceSB);
	}

	@Override
	public SteeringBehavior<Vector3> getSteeringBehavior() {
		return prioritySteering;
	}

	@Override
	public void draw(GameRenderer gameRenderer) {
		MyShapeRenderer shapeRenderer = gameRenderer.shapeRenderer;
		shapeRenderer.setProjectionMatrix(gameRenderer.viewport.getCamera().combined);

		// Draw collision avoidance proximity
		shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.YELLOW);
		Vector3 pos = steerableBody.getPosition();
		shapeRenderer.circle3(pos.x, pos.y - steerableBody.halfExtents.y, pos.z, proximity.getRadius(), 12);

		shapeRenderer.end();
	}

}
