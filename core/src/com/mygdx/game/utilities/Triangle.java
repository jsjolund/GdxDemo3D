package com.mygdx.game.utilities;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by Johannes Sjolund on 9/23/15.
 */
public class Triangle {
	public Vector3 a;
	public Vector3 b;
	public Vector3 c;
	public Vector3 centroid;

	public Triangle(Vector3 a, Vector3 b, Vector3 c) {
		this.a = a;
		this.b = b;
		this.c = c;
		centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Triangle: ");
		sb.append("a=").append(a);
		sb.append(", b=").append(b);
		sb.append(", c=").append(c);
		sb.append(", centroid=").append(centroid);
		return sb.toString();
	}
}

