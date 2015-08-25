package com.mygdx.game.systems.old;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.MotionStateComponent;

/**
 * Created by user on 8/1/15.
 */

public class CameraMoveAimSystem extends IteratingSystem {

	private Camera camera;
	private ComponentMapper<MoveAimComponent> moveCmps = ComponentMapper.getFor(MoveAimComponent.class);
	private ComponentMapper<MotionStateComponent> motionCmps = ComponentMapper.getFor(MotionStateComponent.class);
	private Vector3 interpolatedPosition = new Vector3();

	public CameraMoveAimSystem(Family family, Camera camera) {
		super(family);

		this.camera = camera;
		camera.lookAt(Vector3.X.cpy().scl(-1));
		camera.update();
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		MoveAimComponent moveAim = moveCmps.get(entity);
		MotionStateComponent motionState = motionCmps.get(entity);
		motionState.transform.getTranslation(interpolatedPosition);
		camera.position.set(interpolatedPosition);
		camera.position.add(moveAim.cameraPosOffset);
		camera.direction.set(moveAim.directionAim);
		camera.up.set(moveAim.up);
		camera.update();
	}

}