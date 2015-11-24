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
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.GameEngine;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.utilities.BulletLocation;
import com.mygdx.game.utilities.BulletSteeringUtils;
import com.mygdx.game.utilities.Constants;
import com.mygdx.game.utilities.Steerer;

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

	public final SteerSettings steerSettings;

	/**
	 * Triangle which the steerable is standing on
	 */
	public Triangle currentTriangle;

	/**
	 * Outputs the linear steering of the steering behaviour.
	 * Angular steering is currently not used.
	 */
	final SteeringAcceleration<Vector3> steeringOutput = new SteeringAcceleration<Vector3>(new Vector3());

	/**
	 * Holds the active steerer
	 */
	public Steerer steerer;

	/**
	 * Used to adjust model orientation when following a path.
	 */
	protected final Quaternion targetOrientation = new Quaternion();
	protected final Quaternion currentOrientation = new Quaternion();
	protected final Vector3 targetOrientationVector = new Vector3(Vector3.Z);

	/**
	 * Holds steering data. Use getters and setters.
	 */
	private Vector3 position = new Vector3();
	private float boundingRadius;
	private final Vector3 linearVelocity = new Vector3();
	private final Vector3 angularVelocity = new Vector3();
	private float zeroLinearSpeedThreshold;
	private float maxLinearSpeed;
	private float maxLinearAcceleration;
	private boolean tagged;
	private float maxAngularSpeed;
	private float maxAngularAcceleration;

	/**
	 * Various temporary objects used in calculation
	 */
	private boolean wasSteering = false;
	private final Matrix4 tmpMatrix = new Matrix4();
	private final Vector3 tmpVec = new Vector3();
	private final Quaternion tmpQuat = new Quaternion();

	/**
	 * @param model            Model to instantiate
	 * @param name               Name of model
	 * @param location         World position at which to place the model instance
	 * @param rotation         The rotation of the model instance in degrees
	 * @param scale            Scale of the model instance
	 * @param shape            Collision shape with which to construct a rigid body
	 * @param mass             Mass of the body
	 * @param belongsToFlag    Flag for which collision layers this body belongs to
	 * @param collidesWithFlag Flag for which collision layers this body collides with
	 * @param callback         If this body should trigger collision contact callbacks.
	 * @param noDeactivate     If this body should never 'sleep'
	 * @param steerSettings    Steerable settings
	 */
	public SteerableBody(Model model, String name,
						 Vector3 location, Vector3 rotation, Vector3 scale,
						 btCollisionShape shape, float mass,
						 short belongsToFlag, short collidesWithFlag,
						 boolean callback, boolean noDeactivate,
						 SteerSettings steerSettings) {
		super(model, name,
				location, rotation, scale,
				shape, mass,
				belongsToFlag, collidesWithFlag,
				callback, noDeactivate);
		this.boundingRadius = (boundingBox.getWidth() + boundingBox.getDepth()) / 4;
		this.steerSettings = steerSettings;
		setZeroLinearSpeedThreshold(steerSettings.getZeroLinearSpeedThreshold());
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);

		if (steerer == null) {
			return;
		}

		// Calculate steering acceleration
		steerer.getSteeringBehavior().calculateSteering(steeringOutput);

		boolean isSteering = isSteering();
		if (isSteering && !wasSteering) {
			startSteering();
		} else if (!isSteering && wasSteering) {
			finishSteering();
		}

		// Apply steering acceleration
		applySteering(steeringOutput, deltaTime);
	}

	/**
	 * Start steering. Friction must be zero for steering to work correctly.
	 */
	protected void startSteering() {
		wasSteering = true;
		body.setFriction(0);
		modelTransform.getRotation(currentOrientation, true);
		if (steerer != null) {
			steerer.startSteering();
		}
	}

	/**
	 * Stops the steering by removing the steering provider after invoking {@link #finishSteering()}
	 */
	protected void stopSteering() {
		finishSteering();
		if (steerer != null) {
			steerer.stopSteering();
		}
		steerer = null;
		steeringOutput.setZero();
	}

	/**
	 * Resets normal friction of body so it cannot slide down most slopes.
	 * Removes any angular velocity the body accumulated.
	 * Sets the body to the orientation of the model.
	 */
	protected void finishSteering() {
		wasSteering = false;
		body.setFriction(steerSettings.getIdleFriction());
		body.setAngularVelocity(Vector3.Zero);
		// Since we were only rotating the model when steering, set body to
		// model rotation when finished moving.
		position = getPosition();
		modelTransform.setFromEulerAngles(
				currentOrientation.getYaw(),
				currentOrientation.getPitch(),
				currentOrientation.getRoll()).setTranslation(position);
		body.setWorldTransform(modelTransform);

		if (steerer != null) {
			steerer.finishSteering();
		}
	}

	/**
	 * Applies the linear component of the steering behaviour. As for the angular component,
	 * the orientation of the model is set to follow the orientation of the path segments.
	 *
	 * @param steering
	 * @param deltaTime
	 */
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

			// Notify the active steerer
			steerer.onSteering();
			
			GameEngine.engine.getScene().setSteerableData(this);

			// Calculate the target orientation of the model based on the direction of motion
			// Note that the entity might twitch or jitter slightly when it finds itself in a situation with  
			// conflicting responses from different behaviors. If you need to mitigate this scenario you can decouple
			// the heading from the velocity vector and average its value over the last few frames, for instance 5.
			// This smoothed heading vector will be used to work out model's orientation.
			velocity = body.getLinearVelocity();
			if (!velocity.isZero(getZeroLinearSpeedThreshold()))
				setModelTargetOrientation(velocity.x, velocity.z);

			// Set current orientation of model, setting orientation of body causes problems when applying force.
			currentOrientation.slerp(targetOrientation, 10 * deltaTime);
			Vector3 position = getPosition();
			modelTransform.setFromEulerAngles(
					currentOrientation.getYaw(),
					currentOrientation.getPitch(),
					currentOrientation.getRoll()).setTranslation(position);
		}
	}

	public void setModelTargetOrientation(float x, float z) {
		targetOrientationVector.set(x, 0, -z).nor();
		targetOrientation.setFromMatrix(true, tmpMatrix.setToLookAt(targetOrientationVector, Vector3.Y));
	}

	/**
	 * @return True if linear velocity of body is not within threshold of zero
	 */
	public boolean isMoving() {
		return !body.getLinearVelocity().isZero(getZeroLinearSpeedThreshold());
	}

	/**
	 * @return True if linear steering output  is not within threshold of zero
	 */
	public boolean isSteering() {
		return !steeringOutput.linear.isZero(getZeroLinearSpeedThreshold());
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
		modelTransform.setToLookAt(tmpVec, Constants.V3_UP).setTranslation(position);
		body.setWorldTransform(modelTransform);
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
	 * Sets the vector to point in the direction the model is facing
	 *
	 * @param out Output vector
	 * @return The output vector for chaining
	 */
	public Vector3 getDirection(Vector3 out) {
		return modelTransform.getRotation(tmpQuat, true).transform(out.set(Vector3.Z));
	}
}
