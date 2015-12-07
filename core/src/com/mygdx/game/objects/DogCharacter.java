package com.mygdx.game.objects;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.Task.Status;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeLibraryManager;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
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
 * A dog character whose brain is modeled through some behavior trees coordinated by a state machine.
 *
 * @author jsjolund
 * @author davebaol
 */
public class DogCharacter extends GameCharacter implements Telegraph {

	public enum DogState implements State<DogCharacter> {
		ACT_ON_YOUR_OWN() {
			@Override
			protected boolean canEnter(DogCharacter dog) {
				return !dog.humanWantToPlay && !dog.humanIsDead;
			}
		},
		PLAY_WITH_HUMAN() {
			@Override
			protected boolean canEnter(DogCharacter dog) {
				return dog.humanWantToPlay;
			}
			@Override
			public void exit(DogCharacter dog) {
				super.exit(dog);
				dog.humanWantToPlay = false;
			}
		},
		FEEL_SAD_FOR_HUMAN_DEATH() {
			@Override
			protected boolean canEnter(DogCharacter dog) {
				return !dog.alreadyCriedForHumanDeath && dog.humanIsDead && dog.isHumanCloseEnough(20);
			}
			@Override
			public void exit(DogCharacter dog) {
				super.exit(dog);
				dog.alreadyCriedForHumanDeath = true;
			}
		};

		protected abstract boolean canEnter(DogCharacter dog);

		@Override
		public void enter(DogCharacter dog) {
			Gdx.app.log("DogState", "Enter state " + name());
			// Activate the tree of this state
			dog.activeBehaviorTree = dog.bTrees.get(this);
		}

		private static final DogState[] priority = new DogState[] {
			FEEL_SAD_FOR_HUMAN_DEATH, 
			PLAY_WITH_HUMAN, 
			ACT_ON_YOUR_OWN
		};

		@Override
		public void update(DogCharacter dog) {
			if (dog.human != null) {
				for (int i = 0 ; i < priority.length; i++) {
					DogState state = priority[i];
					// Should we change behavior tree?
					if (this != state && state.canEnter(dog)) {
						dog.bTreeSwitchFSM.changeState(state);
						return;
					}
				}
			}
			// No behavior tree changed, so perform one step of the active one 
			dog.activeBehaviorTree.step();
		}
		
		@Override
		public void exit(DogCharacter dog) {
			Gdx.app.log("DogState", "Exit state " + name());
			// Cancel the active tree, if running
			if (dog.activeBehaviorTree.getStatus() == Status.RUNNING) {
				dog.activeBehaviorTree.cancel();
			}
			dog.activeBehaviorTree = null;
		}

		@Override
		public boolean onMessage (DogCharacter dog, Telegram telegram) {
			return false;
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

	public final StateMachine<DogCharacter, DogState> bTreeSwitchFSM;
	public final EnumMap<DogState, BehaviorTree<DogCharacter>> bTrees;
	public BehaviorTree<DogCharacter> activeBehaviorTree;
	public final AnimationController animations;
	public final FollowPathSteerer followPathSteerer;
	public final WanderSteerer wanderSteerer;
	public HumanCharacter human;
	public boolean humanWantToPlay;
	public boolean humanIsDead;
	public boolean stickThrown;
	public boolean alreadyCriedForHumanDeath;

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

		activeBehaviorTree = null;

		// Create behavior trees and bind them to states
		BehaviorTreeLibraryManager btlm = BehaviorTreeLibraryManager.getInstance();
		bTrees = new EnumMap<DogState, BehaviorTree<DogCharacter>>(DogState.class);
		bTrees.put(DogState.PLAY_WITH_HUMAN, btlm.createBehaviorTree("btrees/dog_play_with_human.btree", this));
		bTrees.put(DogState.FEEL_SAD_FOR_HUMAN_DEATH, btlm.createBehaviorTree("btrees/dog_feel_sad_for_human_death.btree", this));
		bTrees.put(DogState.ACT_ON_YOUR_OWN, btlm.createBehaviorTree("btrees/dog_act_on_your_own.btree", this));

		// Create state machine to switch among behavior trees
		bTreeSwitchFSM = new DefaultStateMachine<DogCharacter, DogState>(this);
		bTreeSwitchFSM.changeState(DogState.ACT_ON_YOUR_OWN);

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
		bTreeSwitchFSM.update();
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
		return true;
	}


	public Vector3 getFrontSpineBoneWorldDirection(Vector3 out) {
		return getBoneDirection("front_spine", out);
	}
	
	public float getFrontSpineBoneOrientation() {
		return getBoneOrientation("front_spine");
	}
	
	public boolean isHumanCloseEnough(float radius) {
		return human.getPosition().dst2(this.getPosition()) < radius * radius; 
	}
	
}
