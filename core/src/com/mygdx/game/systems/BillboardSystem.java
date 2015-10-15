package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.BillboardComponent;

/**
 * Created by user on 8/3/15.
 */
public class BillboardSystem extends IteratingSystem {

	private final Vector3 worldPos = new Vector3();
	private final ComponentMapper<BillboardComponent> billCmps = ComponentMapper.getFor(BillboardComponent.class);
	private final Camera camera;
	private Quaternion quat = new Quaternion();

	public BillboardSystem(Camera camera) {
		super(Family.all(BillboardComponent.class).get());
		this.camera = camera;
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {

		BillboardComponent billCmp = billCmps.get(entity);
		billCmp.followTransform.getTranslation(worldPos);
		billCmp.modelInstance.transform.set(camera.view).inv();
		billCmp.modelInstance.transform.setTranslation(Vector3.Zero);
		if (billCmp.faceUp) {

			camera.view.getRotation(quat);
			billCmp.modelInstance.transform.setFromEulerAngles(-quat.getYaw(), -90, 0);
		}
		billCmp.modelInstance.transform.setTranslation(worldPos.add(billCmp.offset));
		billCmp.modelInstance.calculateTransforms();
	}
}
