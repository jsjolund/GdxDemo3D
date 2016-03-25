package com.mygdx.game.objects;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibrary;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.mygdx.game.GameScreen;
import com.mygdx.game.objects.dog.TaskAnimation;
import com.mygdx.game.pathfinding.Triangle;
import com.mygdx.game.steerers.FollowPathSteerer;
import com.mygdx.game.steerers.WanderSteerer;
import com.mygdx.game.utilities.AnimationListener;
import com.mygdx.game.utilities.Constants;

/**
 * A dog character whose brain is modeled by a behavior tree.
 *
 * @author jsjolund
 * @author davebaol
 */
public class DogCharacter extends GameCharacter implements Telegraph {

	public enum DogArmature {
		HEAD("head"), FRONT_SPINE("front_spine");

		public final String id;

		DogArmature(String id) {
			this.id = id;
		}
	}

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

	public String dogName;
	public final BehaviorTree<DogCharacter> tree;
	public final AnimationController animations;
	public final FollowPathSteerer followPathSteerer;
	public final WanderSteerer wanderSteerer;
	public HumanCharacter human;
	public boolean humanWantToPlay;
	public boolean humanIsDead;
	public boolean stickThrown;
	public boolean stickCarried;
	public boolean alreadyCriedForHumanDeath;

	private final static Vector3 TMP_V1 = new Vector3();
	private final static Vector3 TMP_V2 = new Vector3();
	private final static Vector3 TMP_V3 = new Vector3();
	/*
	 * Fields used to switch animation
	 */
	public TaskAnimation currentTaskAnimation;
	public AnimationListener currentAnimationListener;
	public TaskAnimation monitoredTaskAnimation;
	public float switchAnimationTime;

	static {
		// Make the behavior tree library parser log
		BehaviorTreeLibraryManager.getInstance().setLibrary(new BehaviorTreeLibrary(BehaviorTreeParser.DEBUG_HIGH));
	}

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

		// Create behavior tree through the library
		BehaviorTreeLibraryManager btlm = BehaviorTreeLibraryManager.getInstance();
		this.tree = btlm.createBehaviorTree("btrees/dog.btree", this);

		// Create animation controller
		animations = new AnimationController(modelInstance);

		// Create path follower
		followPathSteerer = new FollowPathSteerer(this);

		// Create wander steerer
		wanderSteerer = new WanderSteerer(this);

		// Init flags
		humanWantToPlay = false;
		stickThrown = false;
		alreadyCriedForHumanDeath = false;
		humanIsDead = false;
	}

	public boolean followPath(Triangle targetTriangle, Vector3 targetPoint) {
		return followPathSteerer.calculateNewPath(targetTriangle, targetPoint);
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		// Step the tree either directly or through the editor
		//
		// TODO: handle this in a better way
		// Ideally the dog should not know of the tree editor
		GameScreen.screen.stage.btreeController.step(this, deltaTime);

		if (stickCarried) {
			Vector3 mouthPos = getBoneMidpointWorldPosition(DogArmature.HEAD.id, TMP_V1);
			Vector3 headDirection = getBoneDirection(DogArmature.HEAD.id, TMP_V2);
			human.stick.modelTransform.setToLookAt(headDirection, TMP_V3.set(Constants.V3_UP).scl(-1));
			human.stick.modelTransform.setTranslation(mouthPos);
			human.stick.body.setWorldTransform(human.stick.modelTransform);
		}
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
			case Constants.MSG_DOG_HUMAN_IS_DEAD:
				humanIsDead = true;
				humanWantToPlay = false;
				alreadyCriedForHumanDeath = false;
				break;
			case Constants.MSG_DOG_HUMAN_IS_RESURRECTED:
				humanIsDead = false;
				alreadyCriedForHumanDeath = false;
				break;
			case Constants.MSG_DOG_STICK_THROWN:
				stickThrown = true;
				break;
		}
		// Update GUI buttons if the dog's owner is selected
		if (this.human != null && this.human.selected) {
			MessageManager.getInstance().dispatchMessage(Constants.MSG_GUI_UPDATE_DOG_BUTTON, this.human);
		}
		return true;
	}

	public boolean isHumanCloseEnough(float radius) {
		return human.getPosition().dst2(this.getPosition()) < radius * radius;
	}

	public void setCarryStick() {
		stickCarried = true;
	}

	public void giveStickToHuman() {
		stickCarried = false;
		stickThrown = false;
		human.assignStick(human.stick);
	}

}
