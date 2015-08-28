package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.components.ModelComponent;
import com.mygdx.game.components.SelectableComponent;


/**
 * Created by user on 7/31/15.
 */
public class ModelRenderSystem extends EntitySystem {

	public Family systemFamily;
	private Vector3 pos = new Vector3();
	private ModelBatch modelBatch;
	private ImmutableArray<Entity> entities;
	private Camera camera;
	private ComponentMapper<ModelComponent> models = ComponentMapper.getFor(ModelComponent.class);
	private ComponentMapper<SelectableComponent> selectables = ComponentMapper.getFor(SelectableComponent.class);
	private Environment environment;

	private Camera cameraLight;

	public ModelRenderSystem(Camera camera, Environment environment) {
		systemFamily = Family.all(ModelComponent.class).get();

		this.camera = camera;
		this.environment = environment;

		modelBatch = new ModelBatch(Gdx.files.internal("shaders/vertex.glsl"),
				Gdx.files.internal("shaders/fragment.glsl"));

		cameraLight = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cameraLight.near = 1f;
		cameraLight.far = 100;
		cameraLight.position.set(33, 10, 3);
		cameraLight.lookAt(-1, 0, 0);
		cameraLight.update();

	}

	@Override
	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
	}

	private boolean isVisible(final Camera camera, final ModelComponent cmp) {
		cmp.modelInstance.transform.getTranslation(pos);
//		pos.add(cmp.center);
		return camera.frustum.sphereInFrustum(pos, cmp.radius);
	}

	@Override
	public void update(float deltaTime) {

		camera.update();
		modelBatch.begin(camera);
		for (int i = 0; i < entities.size(); ++i) {

			Entity entity = entities.get(i);
			ModelComponent cmp = models.get(entity);


			if (isVisible(camera, cmp)) {
				modelBatch.render(cmp.modelInstance, environment);
			}

			SelectableComponent selCmp = selectables.get(entity);
			if (selCmp != null && selCmp.isSelected) {
//				modelBatch.render(cmp.modelInstance);
			}
		}
		modelBatch.end();
	}
}
