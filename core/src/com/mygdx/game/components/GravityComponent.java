package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/1/15.
 */
public class GravityComponent extends Component {
	Vector3 gravity = new Vector3(0, -9.82f, 0);
}
