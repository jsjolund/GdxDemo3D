package com.mygdx.game.utilities;

import com.badlogic.ashley.core.Entity;

/**
 * Created by Johannes Sjolund on 10/13/15.
 */
public interface Observable {

	void addObserver(Observer observer);

	void removeObserver(Observer observer);

	void notifyObserversEntitySelected(Entity entity);
	void notifyObserversLayerSelected(int layer);

}
