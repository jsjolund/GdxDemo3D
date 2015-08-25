package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Created by user on 8/25/15.
 */
public class CameraTargetingComponent extends Component {

	public final Viewport viewport;
	public final Camera camera;
	public Vector3 directionTarget = new Vector3();
	public Vector3 positionTarget = new Vector3();

	public Vector3 velocity = new Vector3();

	public CameraTargetingComponent(Camera camera, Viewport viewport) {
		this.viewport = viewport;
		this.camera = camera;
	}
}
