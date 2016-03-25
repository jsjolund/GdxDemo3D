package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.objects.dog.*;

import io.piotrjastrzebski.bte.AIEditor;

public class BehaviorTreeController {

	private final AIEditor btreeEditor;
	private DogCharacter currentDog;

	public BehaviorTreeController () {
		btreeEditor = createDogBTEditor();
	}

	public void step(DogCharacter dog, float deltaTime) {
		if (currentDog == dog) {
			btreeEditor.update(deltaTime);
		}
		else {
			try {
				dog.tree.step();
			} catch (Exception ex) {
				Gdx.app.log("BehaviorTreeController", "Exception from dog '" + dog.dogName + "': " + ex.getMessage());
			}
		}
	}

	public void toggleEditorWindow(Group group, TextButton toggle) {
		btreeEditor.toggleWindow(group);
		toggle.setText(btreeEditor.isWindowVisible() ?  "Hide Editor" : "Show Editor");
	}

	public void setCurrentDog(DogCharacter dog) {
		currentDog = dog;
		btreeEditor.getWindow().getTitleLabel().setText(dog.dogName);
		btreeEditor.initialize(dog.tree);
	}

	public void reset() {
		currentDog = null;
		btreeEditor.reset();
	}
	
	private static AIEditor createDogBTEditor() {
		AIEditor editor = new AIEditor();
		// add default task classes to task drawer with default tags
		editor.addDefaultTaskClasses();
		editor.setUpdateStrategy(new AIEditor.BehaviorTreeStepStrategy() {
			@Override public boolean shouldStep (@SuppressWarnings("rawtypes") BehaviorTree behaviorTree, float v) {
				return true;
			}
		});
		
		// Add dog-specific tasks
		editor.addTaskClass("dog", AlreadyCriedForHumanDeathCondition.class);
		editor.addTaskClass("dog", CalculatePathToHumanTask.class);
		editor.addTaskClass("dog", CalculatePathToStickTask.class);
		editor.addTaskClass("dog", FollowPathTask.class);
		editor.addTaskClass("dog", GiveStickTask.class);
		editor.addTaskClass("dog", HumanWantToPlayCondition.class);
		editor.addTaskClass("dog", IsHumanDeadCondition.class);
		editor.addTaskClass("dog", IsHumanInRangeCondition.class);
		editor.addTaskClass("dog", LieDownTask.class);
		editor.addTaskClass("dog", PissTask.class);
		editor.addTaskClass("dog", PickUpStickTask.class);
		editor.addTaskClass("dog", SetAlreadyCriedForHumanDeathTask.class);
		editor.addTaskClass("dog", SetThrowButtonTask.class);
		editor.addTaskClass("dog", SitTask.class);
		editor.addTaskClass("dog", SpinAroundTask.class);
		editor.addTaskClass("dog", SpinAroundToFaceHumanTask.class);
		editor.addTaskClass("dog", StandTask.class);
		editor.addTaskClass("dog", StickThrownCondition.class);
		editor.addTaskClass("dog", WanderTask.class);
		editor.addTaskClass("dog", WhineTask.class);
		
		return editor;
	}

}
