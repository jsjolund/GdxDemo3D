package com.mygdx.game.objects.dog;


import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;

public class PickUpStickTask extends LeafTask<DogCharacter> {

	@Override
	public Status execute() {
		getObject().setCarryStick();
		return Status.SUCCEEDED;
	}

	@Override
	protected Task<DogCharacter> copyTo(Task<DogCharacter> task) {
		return task;
	}
}
