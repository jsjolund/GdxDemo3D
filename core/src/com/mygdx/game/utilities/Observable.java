package com.mygdx.game.utilities;

import com.badlogic.gdx.utils.Bits;
import com.mygdx.game.objects.GameCharacter;
import com.mygdx.game.objects.GameModelBody;

/**
 * Created by Johannes Sjolund on 10/13/15.
 */
public interface Observable {

	void addObserver(Observer observer);

	void removeObserver(Observer observer);

	void notifyObserversEntitySelected(GameCharacter entity);

	void notifyObserversLayerChanged(Bits layer);

}
