package com.mygdx.game.utilities;

import com.badlogic.gdx.utils.Bits;
import com.mygdx.game.objects.GameCharacter;

/**
 * Created by Johannes Sjolund on 10/13/15.
 */
public interface Observer {

	void notifyEntitySelected(GameCharacter entity);

	void notifyLayerChanged(Bits layer);
}
