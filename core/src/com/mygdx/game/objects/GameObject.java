package com.mygdx.game.objects;

import com.badlogic.ashley.core.Entity;

/**
 * Created by Johannes Sjolund on 10/18/15.
 */
public abstract class GameObject extends Entity {
	public abstract void update(float deltaTime);

	public abstract void dispose();
}
