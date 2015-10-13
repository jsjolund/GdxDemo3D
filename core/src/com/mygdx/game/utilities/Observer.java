package com.mygdx.game.utilities;

import com.badlogic.ashley.core.Entity;

/**
 * Created by Johannes Sjolund on 10/13/15.
 */
public interface Observer {

	void notifyEntitySelected(Entity entity);
}
