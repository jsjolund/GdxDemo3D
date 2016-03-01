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
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.LinePathParam;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.mygdx.game.GameEngine;
import com.mygdx.game.GameRenderer;
import com.mygdx.game.GameScreen;
import com.mygdx.game.objects.GameObject;
import com.mygdx.game.objects.SteerableBody;
import com.mygdx.game.pathfinding.NavMeshGraphPath;
import com.mygdx.game.pathfinding.NavMeshPointPath;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.settings.GameSettings;
import com.mygdx.game.utilities.Entity;
import com.mygdx.game.utilities.MyShapeRenderer;

/**
 * A steerer to follow a path while avoiding collisions. 
 * 
 * @author jsjolund
 * @author davebaol
 */
public class FollowPathSteerer extends CollisionAvoidanceSteererBase {

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

	private Vector3 tmpVec1 = new Vector3();
	private Ray stationarityRayLow = new Ray();
	private Ray stationarityRayHigh = new Ray();
	private float stationarityRayLength;
	private Color stationarityRayColor;

	public FollowPathSteerer(final SteerableBody steerableBody) {
		super(steerableBody);

		// At least two points are needed to construct a line path
		Array<Vector3> waypoints = new Array<Vector3>(new Vector3[]{new Vector3(), new Vector3(1, 0, 1)});
		this.linePath = new LinePath<Vector3>(waypoints, true);
		this.followPathSB = new FollowPath<Vector3, LinePath.LinePathParam>(steerableBody, linePath, 1);

		this.prioritySteering.add(followPathSB);
	}

	public boolean calculateNewPath(Ray ray, Bits visibleLayers) {
		if (GameScreen.screen.engine.getScene().navMesh.getPath(
				steerableBody.getCurrentTriangle(),
				steerableBody.getGroundPosition(tmpVec1),
				ray, visibleLayers,
				GameSettings.CAMERA_PICK_RAY_DST,
				navMeshGraphPath)) {

			calculateNewPath0();
			return true;
		}
		return false;
	}

	public boolean calculateNewPath(Triangle targetTriangle, Vector3 targetPoint) {
		if (GameScreen.screen.engine.getScene().navMesh.getPath(
				steerableBody.getCurrentTriangle(),
				steerableBody.getGroundPosition(tmpVec1),
				targetTriangle,
				targetPoint,
				navMeshGraphPath)) {

			calculateNewPath0();
			return true;
		}
		return false;
	}

	/**
	 * Calculate the navigation mesh point path, then assign this steering provider to the owner
	 */
	private void calculateNewPath0() {
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
	public void startSteering() {
	}

	@Override
	public boolean stopSteering() {
		// Clear path
		pathToRender.clear();
		navMeshPointPath.clear();
		navMeshGraphPath.clear();
		return false;
	}

	boolean deadlockDetection;
	float deadlockDetectionStartTime;
	float collisionDuration;
	private static final float DEADLOCK_TIME = .5f;
	private static final float MAX_NO_COLLISION_TIME = DEADLOCK_TIME + .5f;

	@Override
	public boolean processSteering(SteeringAcceleration<Vector3> steering) {

		// Check if steering target path segment changed.
		LinePathParam pathParam = followPathSB.getPathParam();
		int traversedSegment = pathParam.getSegmentIndex();
		if (traversedSegment > currentSegmentIndex) {
			currentSegmentIndex = traversedSegment;
		}

		if (prioritySteering.getSelectedBehaviorIndex() == 0) {
			/*
			 * Collision avoidance management
			 */
			float pr = proximity.getRadius() * 1.5f;
			if (linePath.getEndPoint().dst2(steerableBody.getPosition()) <= pr * pr) {
				// Disable collision avoidance near the end of the path since the obstacle
				// will likely prevent the entity from reaching the target.
				collisionAvoidanceSB.setEnabled(false);
				deadlockDetectionStartTime = Float.POSITIVE_INFINITY;
			} else if (deadlockDetection) {
				// Accumulate collision time during deadlock detection
				collisionDuration += GdxAI.getTimepiece().getDeltaTime();

				if (GdxAI.getTimepiece().getTime() - deadlockDetectionStartTime > DEADLOCK_TIME && collisionDuration > DEADLOCK_TIME * .6f) {
					// Disable collision avoidance since most of the deadlock detection period has been spent on collision avoidance
					collisionAvoidanceSB.setEnabled(false);
				}
			} else {
				// Start deadlock detection
				deadlockDetectionStartTime = GdxAI.getTimepiece().getTime();
				collisionDuration = 0;
				deadlockDetection = true;
			}
			return true;
		}

		/*
		 * Path following management
		 */
		float dst2FromPathEnd = steerableBody.getPosition().dst2(linePath.getEndPoint());

		// Check to see if the entity has reached the end of the path
		if (steering.isZero() && dst2FromPathEnd < followPathSB.getArrivalTolerance() * followPathSB.getArrivalTolerance()) {
			return false;
		}

		// Check if collision avoidance must be re-enabled
		if (deadlockDetection && !collisionAvoidanceSB.isEnabled() && GdxAI.getTimepiece().getTime() - deadlockDetectionStartTime > MAX_NO_COLLISION_TIME) {
				collisionAvoidanceSB.setEnabled(true);
				deadlockDetection = false;
		}
		
		// If linear speed is very low and the entity is colliding something at his feet, like a step of the stairs
		// for instance, we have to increase the acceleration to make him go upstairs. 
		float minVel = .2f;
		if (steerableBody.getLinearVelocity().len2() > minVel * minVel) {
			stationarityRayColor = null;
		} else {
			steerableBody.getGroundPosition(stationarityRayLow.origin).add(0, 0.05f, 0);
			steerableBody.getDirection(stationarityRayLow.direction).scl(1f, 0f, 1f).nor();
			stationarityRayLength = steerableBody.getBoundingRadius() + 0.4f;
			Entity hitEntityLow = GameScreen.screen.engine.rayTest(stationarityRayLow, null, GameEngine.ALL_FLAG, GameEngine.PC_FLAG, stationarityRayLength, null);
			if (hitEntityLow instanceof GameObject) {
				stationarityRayColor = Color.RED;
				stationarityRayHigh.set(stationarityRayLow);
				stationarityRayHigh.origin.add(0, .8f, 0);
				Entity hitEntityHigh = GameScreen.screen.engine.rayTest(stationarityRayHigh, null, GameEngine.ALL_FLAG, GameEngine.PC_FLAG, stationarityRayLength, null);
				if (hitEntityHigh == null) {
					// The entity is touching a small obstacle with his feet like a step of the stairs.
					// Increase the acceleration to make him go upstairs.
					steering.linear.scl(8);
				}
				else if (hitEntityHigh instanceof GameObject) {
					// The entity is touching a higher obstacle like a tree, a column or something.
					// Here we should invent something to circumvent this kind of obstacles :)
					//steering.linear.rotateRad(Constants.V3_UP, Constants.PI0_25);
				}
			} else {
				stationarityRayColor = Color.BLUE;
			}
		}

		return true;
	}

	@Override
	public void draw(GameRenderer gameRenderer) {
		super.draw(gameRenderer);
		
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
			if (i + 1 < pathToRender.size && linePath.calculatePointSegmentSquareDistance(gameRenderer.vTmpDraw2, pathToRender.get(i), pathToRender.get(i + 1), p) < 0.0001)
				i++;
			while (i < pathToRender.size) {
				Vector3 q = pathToRender.get(i++);
				shapeRenderer.line(p, q);
				p = q;
			}			

			// Draw stationarity rays
			if (stationarityRayColor != null) {
				shapeRenderer.setColor(stationarityRayColor);
				shapeRenderer.line(stationarityRayLow.origin, tmpVec1.set(stationarityRayLow.origin).mulAdd(stationarityRayLow.direction, stationarityRayLength));
				shapeRenderer.line(stationarityRayHigh.origin, tmpVec1.set(stationarityRayHigh.origin).mulAdd(stationarityRayHigh.direction, stationarityRayLength));
			}

			shapeRenderer.end();
		}
	}

}
