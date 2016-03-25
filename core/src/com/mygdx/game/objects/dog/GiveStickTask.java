package com.mygdx.game.objects.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;

public class GiveStickTask extends LeafTask<DogCharacter> {

	@Override
	public Task.Status execute() {
		if (getObject().stickCarried)
			getObject().giveStickToHuman();
		return Status.SUCCEEDED;
	}

	@Override
	protected Task<DogCharacter> copyTo(Task<DogCharacter> task) {
		return task;
	}
}