package com.mygdx.game.objects;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.pathfinding.MyLinePath;
import com.mygdx.game.pathfinding.NavMeshGraphPath;
import com.mygdx.game.pathfinding.NavMeshPointPath;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.settings.SteerSettings;
import com.mygdx.game.utilities.BulletLocation;
import com.mygdx.game.utilities.BulletSteeringUtils;

/**
 * Created by Johannes Sjolund on 10/23/15.
 */
public class SteerableBody extends GameModelBody implements Steerable<Vector3> {

	public NavMeshGraphPath navMeshGraphPath = new NavMeshGraphPath();
	public NavMeshPointPath navMeshPointPath = new NavMeshPointPath();
	public Triangle currentTriangle;
	SteeringAcceleration<Vector3> steeringOutput = new SteeringAcceleration<Vector3>(new Vector3());
//	public Vector3 currentPathEnd;
	SteeringBehavior<Vector3> steeringBehavior;
	FollowPath<Vector3, MyLinePath.LinePathParam> followPathSB;
	MyLinePath<Vector3> linePath;
	private Vector3 linearVelocity = new Vector3();
	private Vector3 angularVelocity = new Vector3();
	private float zeroLinearSpeedThreshold;
	private float maxLinearSpeed;
	private float maxLinearAcceleration;
	private boolean tagged;
	private float maxAngularSpeed;
	private float maxAngularAcceleration;
	private Vector3 position = new Vector3();
	private Vector3 groundPosition = new Vector3();
	private Quaternion tmpQuat = new Quaternion();
	private Vector3 tmpVec = new Vector3();
	public SteerableBody(Model model, String id,
						 Vector3 location, Vector3 rotation, Vector3 scale,
						 btCollisionShape shape, float mass,
						 short belongsToFlag, short collidesWithFlag,
						 boolean callback, boolean noDeactivate) {
		super(model, id,
				location, rotation, scale,
				shape, mass,
				belongsToFlag, collidesWithFlag,
				callback, noDeactivate);
	}

	public void calculateNewPath() {
//		currentPathEnd = navMeshGraphPath.end;
		navMeshPointPath.calculateForGraphPath(navMeshGraphPath);
		linePath = new MyLinePath<Vector3>(navMeshPointPath.getVectors(), true);
		followPathSB =
				new FollowPath<Vector3, MyLinePath.LinePathParam>(this, linePath, 1);

		followPathSB //
				// Setters below are only useful to arrive at the end of an open path
				.setTimeToTarget(SteerSettings.timeToTarget) //
				.setArrivalTolerance(SteerSettings.arrivalTolerance) //
				.setDecelerationRadius(SteerSettings.decelerationRadius)
				.setPredictionTime(SteerSettings.predictionTime)
				.setPathOffset(SteerSettings.pathOffset);
		steeringBehavior = followPathSB;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);

		if (steeringBehavior != null) {
			// Calculate steering acceleration
			steeringBehavior.calculateSteering(steeringOutput);

			/*
			 * Here you might want to add a motor control layer filtering steering accelerations.
			 *
			 * For instance, a car in a driving game has physical constraints on its movement: it cannot turn while stationary; the
			 * faster it moves, the slower it can turn (without going into a skid); it can brake much more quickly than it can
			 * accelerate; and it only moves in the direction it is facing (ignoring power slides).
			 */
			// Apply steering acceleration
			applySteering(steeringOutput, deltaTime);
			currentTriangle = navMeshPointPath.getToTriangle(followPathSB.getPathParam().getSegmentIndex());
			layers.clear();
			layers.set(currentTriangle.meshPartIndex);
		}
	}

	protected void applySteering(SteeringAcceleration<Vector3> steering, float deltaTime) {

		if (!isSteering()) {
			body.setAngularVelocity(Vector3.Zero);
			return;
		}

		Vector3 position = getPosition();
		tmpVec.set(steering.linear).scl(1, 0, -1).nor();
		motionState.transform.setToLookAt(tmpVec, Vector3.Y).setTranslation(position);

		Vector3 velocity = body.getLinearVelocity();
		float currentSpeedSquare = velocity.len2();
		float maxLinearSpeed = getMaxLinearSpeed();
		tmpVec.set(steeringOutput.linear).y = 0;
		if (currentSpeedSquare > maxLinearSpeed * maxLinearSpeed) {
			tmpVec.scl(maxLinearSpeed / (float) Math.sqrt(currentSpeedSquare));
		}
		body.applyCentralForce(tmpVec);
	}

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
		return radius;
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
		return transform.getTranslation(position);
	}

	@Override
	public float getOrientation() {
		return transform.getRotation(tmpQuat).getYawRad();
	}

	@Override
	public void setOrientation(float orientation) {
		transform.setToRotationRad(0, 1, 0, orientation);
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

	public Vector3 getGroundPosition() {
		transform.getTranslation(groundPosition);
		groundPosition.y -= bounds.getHeight() / 2;
		return groundPosition;
	}
}
