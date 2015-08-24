package com.mygdx.game.components.blender;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * Created by user on 7/31/15.
 */
public abstract class BlenderComponent extends Component {
	public String type;
	public String name;

	public Vector3 position;
	public Vector3 rotation;
	public Vector3 scale;

	public ArrayMap<String, String> custom_properties;


}
