/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedNode;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * @author jsjolund
 */
public class Triangle implements IndexedNode<Triangle> {

	public int triIndex;
	public int meshPartIndex;
	public Vector3 a;
	public Vector3 b;
	public Vector3 c;
	public Vector3 centroid;
	public Array<Connection<Triangle>> connections;

	public Triangle(Vector3 a, Vector3 b, Vector3 c, int triIndex, int meshPartIndex) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.triIndex = triIndex;
		this.meshPartIndex = meshPartIndex;
		this.centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
		this.connections = new Array<Connection<Triangle>>();
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("Triangle{");
		sb.append("triIndex=").append(triIndex);
		sb.append(", meshPartIndex=").append(meshPartIndex);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public int getIndex() {
		return triIndex;
	}

	@Override
	public Array<Connection<Triangle>> getConnections() {
		return connections;
	}

	/**
	 * Calculates the angle in radians between a reference vector and the (plane) normal of the triangle.
	 *
	 * @param reference
	 * @return
	 */
	public float getAngle(Vector3 reference) {
		float x = reference.x;
		float y = reference.y;
		float z = reference.z;
		Vector3 normal = reference;
		normal.set(a).sub(b).crs(b.x - c.x, b.y - c.y, b.z - c.z).nor();
		float angle = (float) Math.acos(normal.dot(x, y, z) / (normal.len() * Math.sqrt(x * x + y * y + z * z)));
		reference.set(x, y, z);
		return angle;
	}

	/**
	 * Calculates a random point in this triangle.
	 *
	 * @param out Output vector
	 * @return Output for chaining
	 */
	public Vector3 getRandomPoint(Vector3 out) {
		float r1 = MathUtils.random(0f, 1f);
		float r2 = MathUtils.random(0f, 1f);
		float sr1 = (float) Math.sqrt(r1);
		out.x = (1 - sr1) * a.x + sr1 * (1 - r2) * b.x + (sr1 * r2) * c.x;
		out.y = (1 - sr1) * a.y + sr1 * (1 - r2) * b.y + (sr1 * r2) * c.y;
		out.z = (1 - sr1) * a.z + sr1 * (1 - r2) * b.z + (sr1 * r2) * c.z;
		return out;
	}

	/**
	 * Calculates the area of the triangle.
	 *
	 * @return
	 */
	public float area() {
		double ab = a.dst(b);
		double bc = b.dst(c);
		double ca = a.dst(c);
		double s = (ab + bc + ca) / 2;
		return (float) Math.sqrt(s * (s - ab) * (s - bc) * (s - ca));
	}
}

