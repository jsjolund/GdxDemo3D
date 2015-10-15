package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.mygdx.game.components.CharacterStateComponent;

/**
 * Created by Johannes Sjolund on 10/11/15.
 */
public class CharacterStateSystem extends IteratingSystem {

	private final ComponentMapper<CharacterStateComponent> stateCmps =
			ComponentMapper.getFor(CharacterStateComponent.class);

	public CharacterStateSystem() {
		super(Family.all(CharacterStateComponent.class).get());
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		stateCmps.get(entity).update(deltaTime);
	}
}
