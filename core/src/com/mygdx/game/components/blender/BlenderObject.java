package com.mygdx.game.components.blender;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ArrayMap;

/**
 * Created by user on 7/31/15.
 */
public abstract class BlenderObject {
	public String type;
	public String name;

	public Vector3 position;
	public Vector3 rotation;
	public Vector3 scale;

	public ArrayMap<String, String> custom_properties;

	public static class BLight extends BlenderObject {
		public Color lamp_color;
		public float lamp_energy;
		public float lamp_distance;
		public float lamp_falloff;
	}

	public static class BModel extends BlenderObject {
		public String model_file_name;
	}

	public static class BEmpty extends BlenderObject {

	}

}
