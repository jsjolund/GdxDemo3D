package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 7/31/15.
 */
public class BlenderComponent extends Component {
	public String name;
	public String type;
	public String modelPath;

	public Vector3 position;
	public Vector3 rotation;
	public Vector3 scale;

	public Color lamp_color;
	public float lamp_energy;
	public float lamp_distance;
	public float lamp_falloff;

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("BlenderComponent{");
		sb.append("name='").append(name).append('\'');
		sb.append(", type='").append(type).append('\'');
		sb.append(", position=").append(position);
		sb.append(", rotation=").append(rotation);
		sb.append(", scale=").append(scale);
		sb.append(", lamp_color=").append(lamp_color);
		sb.append(", lamp_energy=").append(lamp_energy);
		sb.append(", lamp_distance=").append(lamp_distance);
		sb.append(", lamp_falloff=").append(lamp_falloff);
		sb.append('}');
		return sb.toString();
	}
}
