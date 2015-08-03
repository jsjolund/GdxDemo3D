package com.mygdx.game.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.BillboardTextureComponent;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.MotionStateComponent;

/**
 * Created by user on 8/3/15.
 */
public class BillboardSystem extends IteratingSystem {

	private ComponentMapper<BillboardTextureComponent> billboardComponents = ComponentMapper.getFor
			(BillboardTextureComponent
					.class);
	private ComponentMapper<MotionStateComponent> motionStateComponents = ComponentMapper.getFor(MotionStateComponent
			.class);
	private ComponentMapper<ModelComponent> modelComponents = ComponentMapper.getFor(ModelComponent.class);

	private Camera camera;
	private final Vector3 worldPos = new Vector3();

	public BillboardSystem(Family family, Camera camera) {
		super(family);
		this.camera = camera;
	}

	@Override
	protected void processEntity(Entity entity, float deltaTime) {
		MotionStateComponent motionCmp = motionStateComponents.get(entity);
		motionCmp.transform.getTranslation(worldPos);

		ModelComponent modelCmp = modelComponents.get(entity);
		modelCmp.modelInstance.transform.set(camera.view).inv();
		modelCmp.modelInstance.transform.setTranslation(worldPos);
		modelCmp.modelInstance.calculateTransforms();

//		float dst = worldPos.dst(camera.position);
//		float distanceFade = (viewDistance == 0) ? 1 : 1 - dst / viewDistance;
//		blendAttrib.opacity = distanceFade;
//		modelCmp.modelInstance.materials.get(0).set(blendAttrib);
	}
}
