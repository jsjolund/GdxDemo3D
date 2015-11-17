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

package com.mygdx.game.objects;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.pathfinding.NavMeshGraphPath;
import com.mygdx.game.pathfinding.NavMeshPointPath;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.utilities.BulletLocation;
import com.mygdx.game.utilities.BulletSteeringUtils;
import com.mygdx.game.utilities.Constants;

/**
 * @author jsjolund
 */
public class SteerableBody extends GameModelBody implements Steerable<Vector3> {

	public interface SteerSettings {
		float getTimeToTarget();

		float getArrivalTolerance();

		float getDecelerationRadius();

		float getPredictionTime();

		float getPathOffset();

		float getZeroLinearSpeedThreshold();

		float getIdleFriction();
	}

	private final SteerSettings steerSettings;
	public NavMeshGraphPath navMeshGraphPath = new NavMeshGraphPath();
	public NavMeshPointPath navMeshPointPath = new NavMeshPointPath();
	public Triangle currentTriangle;
	public Array<Vector3> pathToRender = new Array<Vector3>();
	public FollowPath<Vector3, LinePath.LinePathParam> followPathSB;
	protected LinePath<Vector3> linePath;
	protected Vector3 position = new Vector3();
	protected Vector3 targetFacing = new Vector3(Vector3.Z);
	protected Quaternion targetFacingQuat = new Quaternion();
	protected Quaternion currentFacingQuat = new Quaternion();
	SteeringAcceleration<Vector3> steeringOutput = new SteeringAcceleration<Vector3>(new Vector3());
	SteeringBehavior<Vector3> steeringBehavior;
	private Matrix4 tmpMatrix = new Matrix4();
	private Vector3 linearVelocity = new Vector3();
	private Vector3 angularVelocity = new Vector3();
	private float zeroLinearSpeedThreshold;
	private float maxLinearSpeed;
	private float maxLinearAcceleration;
	private boolean tagged;
	private float maxAngularSpeed;
	private float maxAngularAcceleration;
	private Vector3 tmpVec = new Vector3();
	private Quaternion tmpQuat = new Quaternion();
	private int currentSegmentIndex = -1;
	private boolean wasSteering = false;
	private Array<Vector3> centerOfMassPath = new Array<Vector3>();

	public SteerableBody(Model model, String id,
						 Vector3 location, Vector3 rotation, Vector3 scale,
						 btCollisionShape shape, float mass,
						 short belongsToFlag, short collidesWithFlag,
						 boolean callback, boolean noDeactivate,
						 SteerSettings steerSettings) {
		super(model, id,
				location, rotation, scale,
				shape, mass,
				belongsToFlag, collidesWithFlag,
				callback, noDeactivate);
		this.steerSettings = steerSettings;
		setZeroLinearSpeedThreshold(steerSettings.getZeroLinearSpeedThreshold());
	}

	public void calculateNewPath() {
		navMeshPointPath.calculateForGraphPath(navMeshGraphPath);

		pathToRender.clear();
		pathToRender.addAll(navMeshPointPath.getVectors());

		centerOfMassPath.clear();
		// Since the navmesh path is on the ground, we need to translate
		// it to align with body origin
		for (Vector3 v : navMeshPointPath) {
			centerOfMassPath.add(new Vector3(v).add(0, halfExtents.y, 0));
		}
		linePath = new LinePath<Vector3>(centerOfMassPath, true);
		followPathSB =
				new FollowPath<Vector3, LinePath.LinePathParam>(this, linePath, 1)
						// Setters below are only useful to arrive at the end of an open path
						.setTimeToTarget(steerSettings.getTimeToTarget())
						.setArrivalTolerance(steerSettings.getArrivalTolerance())
						.setDecelerationRadius(steerSettings.getDecelerationRadius())
						.setPredictionTime(steerSettings.getPredictionTime())
						.setPathOffset(steerSettings.getPathOffset());
		steeringBehavior = followPathSB;
		setZeroLinearSpeedThreshold(steerSettings.getZeroLinearSpeedThreshold());
		currentSegmentIndex = -1;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);

		if (steeringBehavior == null) {
			return;
		}

		// Calculate steering acceleration
		steeringBehavior.calculateSteering(steeringOutput);

		boolean isSteering = isSteering();
		if (isSteering && !wasSteering) {
			startSteering();
		} else if (!isSteering && wasSteering) {
			finishSteering();
		}

		// Apply steering acceleration
		applySteering(steeringOutput, deltaTime);

		// Check if steering target path segment changed.
		int traversedSegments = followPathSB.getPathParam().getSegmentIndex() - currentSegmentIndex;
		if (traversedSegments > 0) {
			// Update rotation and current navmesh triangle.
			currentSegmentIndex = followPathSB.getPathParam().getSegmentIndex();
			LinePath.Segment<Vector3> segment = linePath.getSegments().get(currentSegmentIndex);
			targetFacing.set(segment.getEnd()).sub(segment.getBegin()).scl(1, 0, -1).nor();
			targetFacingQuat.setFromMatrix(true, tmpMatrix.setToLookAt(targetFacing, Vector3.Y));
			currentTriangle = navMeshPointPath.getToTriangle(currentSegmentIndex);
			layers.clear();
			layers.set(currentTriangle.meshPartIndex);
		}
	}

	public int getCurrentSegmentIndex() {
		return currentSegmentIndex;
	}

	/**
	 * Returns the point on the current path segment closest to the Steerable.
	 *
	 * @param out Output vector
	 * @return The output vector for chaining
	 */
	public Vector3 getLinePathPosition(Vector3 out) {
		linePath.calculatePointSegmentSquareDistance(out,
				linePath.getSegments().get(currentSegmentIndex).getBegin(),
				linePath.getSegments().get(currentSegmentIndex).getEnd(),
				getGroundPosition(tmpVec));
		out.sub(0, halfExtents.y, 0);
		return out;
	}

	protected void startSteering() {
		wasSteering = true;
		body.setFriction(0);
		transform.getRotation(currentFacingQuat, true);
	}

	protected void finishSteering() {
		wasSteering = false;
		body.setFriction(steerSettings.getIdleFriction());
		body.setAngularVelocity(Vector3.Zero);
		// Since we were only rotating the model when steering, set body to
		// model rotation when finished moving.
		position = getPosition();
		transform.setFromEulerAngles(
				currentFacingQuat.getYaw(),
				currentFacingQuat.getPitch(),
				currentFacingQuat.getRoll()).setTranslation(position);
		body.setWorldTransform(transform);

		pathToRender.clear();
	}


	protected void applySteering(SteeringAcceleration<Vector3> steering, float deltaTime) {
		boolean anyAccelerations = false;
		// Update position and linear velocity
		if (!steering.linear.isZero()) {
			// this method internally scales the force by deltaTime
			body.applyCentralForce(steering.linear);
			anyAccelerations = true;
		}

		if (anyAccelerations) {
			body.activate();

			// Cap the linear speed
			Vector3 velocity = body.getLinearVelocity();
			float currentSpeedSquare = velocity.len2();
			float maxLinearSpeed = getMaxLinearSpeed();
			if (currentSpeedSquare > maxLinearSpeed * maxLinearSpeed) {
				body.setLinearVelocity(velocity.scl(maxLinearSpeed / (float) Math.sqrt(currentSpeedSquare)));
			}

			// Set facing of model, setting facing of body causes problems when applying force.
			currentFacingQuat.slerp(targetFacingQuat, 10 * deltaTime);
			Vector3 position = getPosition();
			transform.setFromEulerAngles(
					currentFacingQuat.getYaw(),
					currentFacingQuat.getPitch(),
					currentFacingQuat.getRoll()).setTranslation(position);
		}
	}

	public boolean isMoving() {
		return !body.getLinearVelocity().isZero(getZeroLinearSpeedThreshold());
	}

	public boolean isSteering() {
		return !steeringOutput.linear.isZero(getZeroLinearSpeedThreshold());
	}

	protected void stopPathFollowing() {
		// Clear path and stop steering
		navMeshPointPath.clear();
		navMeshGraphPath.clear();
		steeringBehavior = null;
		steeringOutput.linear.setZero();
		finishSteering();
		body.setFriction(1);
	}

	@Override
	public Vector3 getLinearVelocity() {
		return linearVelocity.set(body.getLinearVelocity());
	}

	@Override
	public float getAngularVelocity() {
		return angularVelocity.set(body.getAngularVelocity()).len();
	}

	@Override
	public float getBoundingRadius() {
		return boundingRadius;
	}

	@Override
	public boolean isTagged() {
		return tagged;
	}

	@Override
	public void setTagged(boolean tagged) {
		this.tagged = tagged;
	}

	@Override
	public float getZeroLinearSpeedThreshold() {
		return zeroLinearSpeedThreshold;
	}

	@Override
	public void setZeroLinearSpeedThreshold(float value) {
		zeroLinearSpeedThreshold = value;
	}

	@Override
	public float getMaxLinearSpeed() {
		return maxLinearSpeed;
	}

	@Override
	public void setMaxLinearSpeed(float maxLinearSpeed) {
		this.maxLinearSpeed = maxLinearSpeed;
	}

	@Override
	public float getMaxLinearAcceleration() {
		return maxLinearAcceleration;
	}

	@Override
	public void setMaxLinearAcceleration(float maxLinearAcceleration) {
		this.maxLinearAcceleration = maxLinearAcceleration;
	}

	@Override
	public float getMaxAngularSpeed() {
		return maxAngularSpeed;
	}

	@Override
	public void setMaxAngularSpeed(float maxAngularSpeed) {
		this.maxAngularSpeed = maxAngularSpeed;
	}

	@Override
	public float getMaxAngularAcceleration() {
		return maxAngularAcceleration;
	}

	@Override
	public void setMaxAngularAcceleration(float maxAngularAcceleration) {
		this.maxAngularAcceleration = maxAngularAcceleration;
	}

	@Override
	public Vector3 getPosition() {
		return body.getWorldTransform().getTranslation(position);
	}

	/**
	 * Get the rotation of the model for this Steerable, around the Y-axis.
	 * This might not be equal to the rotation of the collision body while steering is active.
	 * <p>
	 * When orientation is 0, character faces positive X axis.
	 * Rotating [0:PI] makes the character turn to the left from its perspective.
	 * Rotating [0:-PI] turns it right.
	 *
	 * @return
	 */
	@Override
	public float getOrientation() {
		return BulletSteeringUtils.vectorToAngle(getDirection(tmpVec));
	}

	/**
	 * Set the rotation of the model and collision body for this Steerable, around the Y-axis.
	 * <p>
	 * When orientation is 0, character faces positive X axis.
	 * Rotating [0:PI] makes the character turn to the left from its perspective.
	 * Rotating [0:-PI] turns it right.
	 *
	 * @param orientation
	 */
	@Override
	public void setOrientation(float orientation) {
		position = getPosition();
		BulletSteeringUtils.angleToVector(tmpVec, -orientation);
		transform.setToLookAt(tmpVec, Constants.V3_UP).setTranslation(position);
		body.setWorldTransform(transform);
	}

	@Override
	public float vectorToAngle(Vector3 vector) {
		return BulletSteeringUtils.vectorToAngle(vector);
	}

	@Override
	public Vector3 angleToVector(Vector3 outVector, float angle) {
		return BulletSteeringUtils.angleToVector(outVector, angle);
	}

	@Override
	public Location<Vector3> newLocation() {
		return new BulletLocation();
	}

	/**
	 * Returns the world position of the lowest point of the body.
	 *
	 * @param out Output vector
	 * @return The output vector for chaining
	 */
	public Vector3 getGroundPosition(Vector3 out) {
		body.getWorldTransform().getTranslation(out);
		out.y -= boundingBox.getHeight() / 2;
		return out;
	}

	/**
	 * Returns the direction the model is facing.
	 *
	 * @param out Output vector
	 * @return The output vector for chaining
	 */
	public Vector3 getDirection(Vector3 out) {
		return transform.getRotation(tmpQuat, true).transform(out.set(Vector3.Z));
	}
}
