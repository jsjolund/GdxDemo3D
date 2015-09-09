package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.bullet.dynamics.btFixedConstraint;
import com.badlogic.gdx.physics.bullet.dynamics.btTypedConstraint;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;

/**
 * Created by user on 9/8/15.
 */
public class RagdollConstraintComponent extends Component {
	public Array<btTypedConstraint> typedConstraints = new Array<btTypedConstraint>();
}
