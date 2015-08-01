package com.mygdx.game.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.mygdx.game.components.LightComponent;

/**
 * Created by user on 7/31/15.
 */
public class ModelEnvironmentSystem extends EntitySystem {

	public static final String tag = "ModelEnvSystem";
	public LightListener lightListener;
	public Family systemFamily;
	public Environment environment;
	private ImmutableArray<Entity> entities;
	private ComponentMapper<LightComponent> lights = ComponentMapper.getFor(LightComponent.class);
	public ModelEnvironmentSystem() {
		environment = new Environment();
		systemFamily = Family.all(LightComponent.class).get();
		lightListener = new LightListener();

		this.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.1f, 0.1f, 0.1f,
				1.f));
	}

	public void addedToEngine(Engine engine) {
		entities = engine.getEntitiesFor(systemFamily);
		for (int i = 0; i < entities.size(); ++i) {
			Entity entity = entities.get(i);
			LightComponent cmp = entity.getComponent(LightComponent.class);
			environment.add(cmp.light);
		}
	}

	public class LightListener implements EntityListener {

		@Override
		public void entityAdded(Entity entity) {
			LightComponent cmp = entity.getComponent(LightComponent.class);
			environment.add(cmp.light);
		}

		@Override
		public void entityRemoved(Entity entity) {
			LightComponent cmp = entity.getComponent(LightComponent.class);
			environment.remove(cmp.light);
		}
	}

}
