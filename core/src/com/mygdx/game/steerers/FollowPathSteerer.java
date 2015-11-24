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

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.CollisionAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.Segment;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.GameEngine;
import com.mygdx.game.GameRenderer;
import com.mygdx.game.objects.SteerableBody;
import com.mygdx.game.pathfinding.NavMeshGraphPath;
import com.mygdx.game.pathfinding.NavMeshPointPath;
import com.mygdx.game.utilities.MyShapeRenderer;
import com.mygdx.game.utilities.Steerer;

/**
 * @author jsjolund
 * @author davebaol
 */
public class FollowPathSteerer implements Steerer {

	// This should become part of gdx-ai since the ability to know
	// the selected behavior is generally useful.
	private static class MyPrioritySteering extends PrioritySteering<Vector3> {
		protected int selectedBehaviorIndex;

		public MyPrioritySteering (Steerable<Vector3> owner, float epsilon) {
			super(owner, epsilon);
		}
		
		public int getSelectedBehaviorIndex() {
			return selectedBehaviorIndex;
		}

		@Override
		protected SteeringAcceleration<Vector3> calculateRealSteering (SteeringAcceleration<Vector3> steering) {
			// We'll need epsilon squared later.
			float epsilonSquared = epsilon * epsilon;

			// Go through the behaviors until one has a large enough acceleration
			int n = behaviors.size;
			selectedBehaviorIndex = -1;
			for (int i = 0; i < n; i++) {
				selectedBehaviorIndex = i;
				SteeringBehavior<Vector3> behavior = behaviors.get(i);

				// Calculate the behavior's steering
				behavior.calculateSteering(steering);

				// If we're above the threshold return the current steering
				if (steering.calculateSquareMagnitude() > epsilonSquared) return steering;
			}

			// If we get here, it means that no behavior had a large enough acceleration,
			// so return the small acceleration from the final behavior or zero if there are
			// no behaviors in the list.
			return n > 0 ? steering : steering.setZero();
		}
	}
	
	private final SteerableBody steerableBody;
	
	/**
	 * Path of triangles on the navigation mesh. Used to construct path points.
	 */
	public final NavMeshGraphPath navMeshGraphPath = new NavMeshGraphPath();
	
	/**
	 * Path points on the navigation mesh, which the steerable will follow.
	 */
	public final NavMeshPointPath navMeshPointPath = new NavMeshPointPath();

	/**
	 * Path which is rendered on screen
	 */
	public final Array<Vector3> pathToRender = new Array<Vector3>();
	
	/**
	 * Steering behaviour for path following
	 */
	public final FollowPath<Vector3, LinePath.LinePathParam> followPathSB;
	
	/**
	 * Holds the path segments for steering behaviour
	 */
	protected final LinePath<Vector3> linePath;

	/**
	 * Path segment index the steerable is currently following.
	 */
	private int currentSegmentIndex = -1;
	
	/**
	 * Points from which to construct the path segments the steerable should follow
	 */
	private final Array<Vector3> centerOfMassPath = new Array<Vector3>();
	
	private final CollisionAvoidance<Vector3> collisionAvoidanceSB;
	private final RadiusProximity<Vector3> proximity;

	public final MyPrioritySteering prioritySteering;

	public FollowPathSteerer (final SteerableBody steerableBody) {
		this.steerableBody = steerableBody;
		
		// At least two points are needed to construct a line path
		Array<Vector3> waypoints = new Array<Vector3>(new Vector3[] {new Vector3(), new Vector3(1,0,1)});
		this.linePath = new LinePath<Vector3>(waypoints, true);
		this.followPathSB = new FollowPath<Vector3, LinePath.LinePathParam>(steerableBody, linePath, 1);

		this.proximity = new RadiusProximity<Vector3>(steerableBody, GameEngine.engine.characters, steerableBody.getBoundingRadius() * 1.8f);
		this.collisionAvoidanceSB = new CollisionAvoidance<Vector3>(steerableBody, proximity) {
			@Override
			protected SteeringAcceleration<Vector3> calculateRealSteering (SteeringAcceleration<Vector3> steering) {
				super.calculateRealSteering(steering);
				steering.linear.y = 0; // remove any vertical acceleration
				return steering;
			}
		};

		this.prioritySteering = new MyPrioritySteering(steerableBody, 0.001f); //
		prioritySteering.add(collisionAvoidanceSB); //
		prioritySteering.add(followPathSB);
	}

	/**
	 * Calculate the navigation mesh point path, then assign this steering provider to the owner
	 */
	public void calculateNewPath() {
		navMeshPointPath.calculateForGraphPath(navMeshGraphPath);

		pathToRender.clear();
		pathToRender.addAll(navMeshPointPath.getVectors());

		centerOfMassPath.clear();
		// Since the navmesh path is on the ground, we need to translate
		// it to align with body origin
		for (Vector3 v : navMeshPointPath) {
			centerOfMassPath.add(new Vector3(v).add(0, steerableBody.halfExtents.y, 0));
		}
		linePath.createPath(centerOfMassPath);
		followPathSB.setTimeToTarget(steerableBody.steerSettings.getTimeToTarget())
						.setArrivalTolerance(steerableBody.steerSettings.getArrivalTolerance())
						.setDecelerationRadius(steerableBody.steerSettings.getDecelerationRadius())
						.setPredictionTime(steerableBody.steerSettings.getPredictionTime())
						.setPathOffset(steerableBody.steerSettings.getPathOffset());
		steerableBody.setZeroLinearSpeedThreshold(steerableBody.steerSettings.getZeroLinearSpeedThreshold());
		currentSegmentIndex = -1;

		collisionAvoidanceSB.setEnabled(true);

		deadlockDetection = false;

		// Make this steerer active
		steerableBody.steerer = this;
	}

	/**
	 * Path segment index the steerable is currently following.
	 */
	public int getCurrentSegmentIndex() {
		return currentSegmentIndex;
	}

	@Override
	public SteeringBehavior<Vector3> getSteeringBehavior () {
		return prioritySteering;
	}

	@Override
	public void startSteering () {
	}

	@Override
	public void stopSteering () {
		// Clear path
		navMeshPointPath.clear();
		navMeshGraphPath.clear();
	}

	@Override
	public void finishSteering () {
		pathToRender.clear();
	}

	boolean deadlockDetection;
	float deadlockDetectionStartTime;
	float collisionDuration;
	static final float deadlockTime = .5f;
	static final float maxNoCollisionTime = deadlockTime + .5f;

	@Override
	public void onSteering () {
		
		if (prioritySteering.getSelectedBehaviorIndex() == 0) {
				float pr = proximity.getRadius() * 1.5f;
				if (linePath.getEndPoint().dst2(steerableBody.getPosition()) <= pr * pr) {
					// Disable collision avoidance near the end of the path since the obstacle 
					// will likely prevent the entity from reaching the target.
					collisionAvoidanceSB.setEnabled(false);
					deadlockDetectionStartTime = Float.POSITIVE_INFINITY;
				}
				else if (deadlockDetection) {
					// Accumulate collision time during deadlock detection
					collisionDuration += GdxAI.getTimepiece().getDeltaTime();

					if (GdxAI.getTimepiece().getTime() - deadlockDetectionStartTime > deadlockTime && collisionDuration > deadlockTime *.6f) {
						// Disable collision avoidance since most of the deadlock detection period has been spent on collision avoidance
						collisionAvoidanceSB.setEnabled(false);
					}
				}
				else {
					// Start deadlock detection
					deadlockDetectionStartTime = GdxAI.getTimepiece().getTime();
					collisionDuration = 0;
					deadlockDetection = true;
				}
		}
		else {
			if (deadlockDetection && !collisionAvoidanceSB.isEnabled() && GdxAI.getTimepiece().getTime() - deadlockDetectionStartTime > maxNoCollisionTime) {
				collisionAvoidanceSB.setEnabled(true);
				deadlockDetection = false;
			}
		}

		// Check if steering target path segment changed.
		int traversedSegment = followPathSB.getPathParam().getSegmentIndex();
		if (traversedSegment > currentSegmentIndex) {
			// Update model target orientation. Current orientation wi
			currentSegmentIndex = traversedSegment;
/*
//			Segment<Vector3> segment = linePath.getSegments().get(currentSegmentIndex);
//			steerableBody.setModelTargetOrientation(segment.getEnd().x - segment.getBegin().x, segment.getEnd().z - segment.getBegin().z);
			// Update current navmesh triangle
			steerableBody.currentTriangle = navMeshPointPath.getToTriangle(currentSegmentIndex);
			// Set model to be visible on the same layer as mesh part index of current triangle
			steerableBody.visibleOnLayers.clear();
			steerableBody.visibleOnLayers.set(steerableBody.currentTriangle.meshPartIndex);
 */
		}
	}
	
	@Override
	public void draw(GameRenderer gameRenderer) {
		if (pathToRender.size > 0 && currentSegmentIndex >= 0) {
			MyShapeRenderer shapeRenderer = gameRenderer.shapeRenderer;
			shapeRenderer.setProjectionMatrix(gameRenderer.viewport.getCamera().combined);

			// Draw path target position
			Vector3 t = gameRenderer.vTmpDraw1.set(followPathSB.getInternalTargetPosition());
			t.y -= steerableBody.halfExtents.y;
			float size = .05f;
			float offset = size / 2;
			shapeRenderer.begin(MyShapeRenderer.ShapeType.Filled);
			shapeRenderer.setColor(Color.CORAL);
			shapeRenderer.box(t.x - offset, t.y - offset, t.z + offset, size, size, size);

			// Draw path
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			Vector3 p = t;
			int i = getCurrentSegmentIndex() + 1;
			if (i + 1 < pathToRender.size && linePath.calculatePointSegmentSquareDistance(gameRenderer.vTmpDraw2, pathToRender.get(i), pathToRender.get(i+1), p) < 0.0001)
				i++;
			while (i < pathToRender.size) {
				Vector3 q = pathToRender.get(i++);
				shapeRenderer.line(p, q);
				p = q;
			}

			// Draw collision avoidance proximity
			shapeRenderer.set(MyShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(Color.YELLOW);
			Vector3 pos = steerableBody.getPosition();
			shapeRenderer.circle3(pos.x, pos.y - steerableBody.halfExtents.y, pos.z, proximity.getRadius(), 12);
			
			shapeRenderer.end();
		}
	}

}
