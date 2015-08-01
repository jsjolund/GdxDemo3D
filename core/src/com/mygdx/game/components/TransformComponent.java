package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;

/**
 * Created by user on 7/31/15.
 */
public class TransformComponent extends Component {

	Matrix4 transform = new Matrix4();

	public TransformComponent(Matrix4 transform) {
		this.transform = transform;
	}

}
