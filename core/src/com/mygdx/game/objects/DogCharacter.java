package com.mygdx.game.objects;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.objects.dog.TaskAnimation;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.steerers.FollowPathSteerer;
import com.mygdx.game.steerers.WanderSteerer;
import com.mygdx.game.utilities.AnimationListener;
import com.mygdx.game.utilities.Constants;

/**
 * A dog character whose brain is modeled through a behavior tree.
 *
 * @author jsjolund
 * @author davebaol
 */
public class DogCharacter extends GameCharacter implements Telegraph {

	public static class DogSteerSettings implements SteerSettings {
		public static float maxLinearAcceleration = 50f;
		public static float maxLinearSpeed = 2f;
		public static float maxAngularAcceleration = 100f;
		public static float maxAngularSpeed = 15f;
		public static float idleFriction = 0.9f;
		public static float zeroLinearSpeedThreshold = 0.001f;
		public static float runMultiplier = 3f;
		public static float timeToTarget = 0.1f;
		public static float arrivalTolerance = 0.1f;
		public static float decelerationRadius = 0.5f;
		public static float predictionTime = 0f;
		public static float pathOffset = 1f;

		@Override
		public float getTimeToTarget() {
			return timeToTarget;
		}

		@Override
		public float getArrivalTolerance() {
			return arrivalTolerance;
		}

		@Override
		public float getDecelerationRadius() {
			return decelerationRadius;
		}

		@Override
		public float getPredictionTime() {
			return predictionTime;
		}

		@Override
		public float getPathOffset() {
			return pathOffset;
		}

		@Override
		public float getZeroLinearSpeedThreshold() {
			return zeroLinearSpeedThreshold;
		}

		@Override
		public float getIdleFriction() {
			return idleFriction;
		}
	}

	public final BehaviorTree<DogCharacter> btree;
	public final AnimationController animations;
	public final FollowPathSteerer followPathSteerer;
	public final WanderSteerer wanderSteerer;
	public HumanCharacter human;
	public boolean humanWantToPlay;
	public boolean stickThrown;

	/*
	 * Fields used to switch animation
	 */
	public TaskAnimation currentTaskAnimation;
	public AnimationListener currentAnimationListener;
	public TaskAnimation monitoredTaskAnimation;
	public float switchAnimationTime;

	public DogCharacter(Model model, String name,
						Vector3 location, Vector3 rotation, Vector3 scale,
						btCollisionShape shape, float mass,
						short belongsToFlag, short collidesWithFlag,
						boolean callback, boolean noDeactivate) {
		super(model, name,
				location, rotation, scale,
				shape, mass,
				belongsToFlag, collidesWithFlag,
				callback, noDeactivate,
				new DogSteerSettings());

		animations = new AnimationController(modelInstance);

		// Create behavior tree
		btree = BehaviorTreeLibraryManager.getInstance().createBehaviorTree("btrees/dog.btree", this);

		// Create path follower
		followPathSteerer = new FollowPathSteerer(this);

		// Create wander steerer
		wanderSteerer = new WanderSteerer(this);

		humanWantToPlay = false;
	}

	public boolean followPath(Triangle targetTriangle, Vector3 targetPoint) {
		return followPathSteerer.calculateNewPath(targetTriangle, targetPoint);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		btree.step();
	}

	@Override
	public boolean handleMessage(Telegram telegram) {
		switch (telegram.message) {
			case Constants.MSG_DOG_LETS_PLAY:
				humanWantToPlay = true;
				stickThrown = false;
				break;
			case Constants.MSG_DOG_LETS_STOP_PLAYING:
				humanWantToPlay = false;
				break;
			case Constants.MSG_DOG_STICK_THROWN:
				stickThrown = true;
				break;
		}
		return true;
	}


	public Vector3 getFrontSpineBoneWorldDirection(Vector3 out) {
		return getBoneDirection("front_spine", out);
	}
	public float getFrontSpineBoneOrientation() {
		return getBoneOrientation("front_spine");
	}
}
