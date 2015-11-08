package com.mygdx.game.objects.dog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.mygdx.game.objects.DogCharacter;
import com.mygdx.game.settings.GameSettings;

/**
 * @author davebaol
 */
public abstract class DogTaskBase extends LeafTask<DogCharacter> {

	protected float animationSpeedMultiplier;

	public DogTaskBase () {
		this(-1);
	}

	public DogTaskBase (float animationSpeedMultiplier) {
		this.animationSpeedMultiplier = animationSpeedMultiplier;
	}

	protected abstract void startAnimation(DogCharacter dog);

	protected void updateAnimation(DogCharacter dog) {
		// update current animation
		float deltaTime = Gdx.graphics.getDeltaTime();
		if (animationSpeedMultiplier >= 0) {
			// this is a movement task
			deltaTime *= dog.getLinearVelocity().len() * animationSpeedMultiplier;
		}
		dog.animations.update(deltaTime * GameSettings.GAME_SPEED);
	}

	@Override
	public void start() {
		startAnimation(getObject());
	}

	@Override
	public void run () {
		updateAnimation(getObject());
		running();
	}

	@Override
	protected Task<DogCharacter> copyTo (Task<DogCharacter> task) {
		DogTaskBase dogTask = (DogTaskBase)task;
		dogTask.animationSpeedMultiplier = animationSpeedMultiplier;
		return task;
	}
}
