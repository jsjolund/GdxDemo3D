package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;

/**
 * Created by user on 7/31/15.
 */
public class LightComponent extends Component {

	public BaseLight light = new DirectionalLight();

	public LightComponent(PointLight light) {
		this.light = light;
	}

	public LightComponent(SpotLight light) {
		this.light = light;
	}

	public LightComponent(DirectionalLight light) {
		this.light = light;
	}
}
