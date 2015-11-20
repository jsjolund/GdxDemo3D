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

import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath.Segment;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
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
	public FollowPath<Vector3, LinePath.LinePathParam> followPathSB;
	
	/**
	 * Holds the path segments for steering behaviour
	 */
	protected LinePath<Vector3> linePath;

	/**
	 * Path segment index the steerable is currently following.
	 */
	private int currentSegmentIndex = -1;
	
	/**
	 * Points from which to construct the path segments the steerable should follow
	 */
	private final Array<Vector3> centerOfMassPath = new Array<Vector3>();

	public FollowPathSteerer (SteerableBody steerableBody) {
		this.steerableBody = steerableBody;
		// At least two points are needed to construct a line path
		Array<Vector3> waypoints = new Array<Vector3>(new Vector3[] {new Vector3(), new Vector3(1,0,1)});
		this.linePath = new LinePath<Vector3>(waypoints, true);
		this.followPathSB = new FollowPath<Vector3, LinePath.LinePathParam>(steerableBody, linePath, 1);
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
		return followPathSB;
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
	
	@Override
	public void onSteering () {
		// Check if steering target path segment changed.
		int traversedSegment = followPathSB.getPathParam().getSegmentIndex();
		if (traversedSegment > currentSegmentIndex) {
			// Update model target orientation. Current orientation wi
			currentSegmentIndex = traversedSegment;
			Segment<Vector3> segment = linePath.getSegments().get(currentSegmentIndex);
			steerableBody.setModelTargetOrientation(segment.getEnd().x - segment.getBegin().x, segment.getEnd().z - segment.getBegin().z);
			// Update current navmesh triangle
			steerableBody.currentTriangle = navMeshPointPath.getToTriangle(currentSegmentIndex);
			// Set model to be visible on the same layer as mesh part index of current triangle
			steerableBody.visibleOnLayers.clear();
			steerableBody.visibleOnLayers.set(steerableBody.currentTriangle.meshPartIndex);
		}
	}
	
	@Override
	public void draw(GameRenderer gameRenderer) {
		if (pathToRender.size > 0 && currentSegmentIndex >= 0) {
			MyShapeRenderer shapeRenderer = gameRenderer.shapeRenderer;
			shapeRenderer.setProjectionMatrix(gameRenderer.viewport.getCamera().combined);
			shapeRenderer.begin(MyShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(Color.CORAL);

			// Path
			Vector3 q;
			Vector3 p = getLinePathPosition(gameRenderer.vTmpDraw1, gameRenderer.vTmpDraw2);
			for (int i = getCurrentSegmentIndex() + 1; i < pathToRender.size; i++) {
				q = pathToRender.get(i);
				shapeRenderer.line(p, q);
				p = q;
			}

			// Target position
			Vector3 t = followPathSB.getInternalTargetPosition();
			float size = .05f;
			float offset = size / 2;
			shapeRenderer.set(MyShapeRenderer.ShapeType.Filled);
			shapeRenderer.box(t.x - offset, t.y - offset - steerableBody.halfExtents.y, t.z + offset, size, size, size);

			shapeRenderer.end();
		}
	}

	/**
	 * Returns the point on the current path segment closest to the Steerable.
	 *
	 * @param out Output vector
	 * @param tmp temporary vector
	 * @return The output vector for chaining
	 */
	private Vector3 getLinePathPosition(Vector3 out, Vector3 tmp) {
		Segment<Vector3> segment = linePath.getSegments().get(currentSegmentIndex);
		linePath.calculatePointSegmentSquareDistance(out,
				segment.getBegin(), segment.getEnd(),
				steerableBody.getGroundPosition(tmp));
		out.y -= steerableBody.halfExtents.y;
		return out;
	}

}
