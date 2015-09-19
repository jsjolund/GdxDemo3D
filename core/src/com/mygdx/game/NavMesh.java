package com.mygdx.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * Created by user on 9/19/15.
 */
public class NavMesh implements Graph {

	public Vector3[] vertices;
	public Array<Triangle> triangles = new Array<Triangle>();
	public Array<Connection> edges = new Array<Connection>();

	public class Edge implements Connection<Triangle> {

		public Triangle fromNode;
		public Triangle toNode;

		@Override
		public float getCost() {
			return 0;
		}

		@Override
		public Triangle getFromNode() {
			return fromNode;
		}

		@Override
		public Triangle getToNode() {
			return toNode;
		}
	}

	public class Triangle {
		public Vector3 a;
		public Vector3 b;
		public Vector3 c;
		public Vector3 center;

		public Triangle(Vector3 a, Vector3 b, Vector3 c) {
			this.a = a;
			this.b = b;
			this.c = c;
			center = new Vector3(a).add(b).add(c).scl(1f / 3f);
		}

	}

	@Override
	public Array<Connection> getConnections(Object fromNode) {
		return edges;
	}

	public NavMesh(Mesh mesh) {
		FloatBuffer vertBuf = mesh.getVerticesBuffer();
		VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
		int posOffset = posAttr.offset / 4;
		int numIndices = mesh.getNumIndices();
		short[] indices = new short[numIndices];
		mesh.getIndices(indices);

		vertices = new Vector3[numIndices];
		for (short i : indices) {
			int j = i * mesh.getVertexSize() / 4 + posOffset;
			float x = vertBuf.get(j++);
			float y = vertBuf.get(j++);
			float z = vertBuf.get(j++);
			Vector3 vertex = new Vector3(x, z, -y);
			vertices[i] = vertex;
		}

		int[][][] triConn = new int[numIndices][numIndices][2];
		for (int i = 0; i < triConn.length; i++) {
			for (int j = 0; j < triConn[i].length; j++) {
				triConn[i][j][0] = -1;
				triConn[i][j][1] = -1;
			}
		}
		System.out.println(Arrays.toString(indices));
		triangles.ordered = true;
		int a = -1;
		int b = -1;
		int c = -1;

		for (int vIndex = 0; vIndex < numIndices; vIndex++) {
			short i = indices[vIndex];
			if (vIndex == numIndices - 1) {
				addTriangle(a, b, i, triConn);
				break;
			}
			if (a == -1) {
				a = i;
			} else if (b == -1) {
				b = i;
			} else if (c == -1) {
				c = i;
			} else {
				addTriangle(a, b, c, triConn);
				a = i;
				b = -1;
				c = -1;
			}
		}

		for (int i = 0; i < triConn.length; i++) {
			for (int j = 0; j < triConn[i].length; j++) {
				int tri0Index = triConn[i][j][0];
				int tri1Index = triConn[i][j][1];
				if (tri0Index != -1 && tri1Index != -1) {
					System.out.println(tri0Index + " " + tri1Index);
					Vector3[] pair = new Vector3[]{triangles.get(tri0Index).center, triangles.get(tri1Index).center};
					triConnections.add(pair);
				}else {
					System.out.println();
				}
			}
		}
	}

	public Array<Vector3[]> triConnections = new Array<Vector3[]>();

	private void addTriangle(int a, int b, int c, int[][][] triConn) {
		Triangle tri = new Triangle(vertices[a], vertices[b], vertices[c]);
		triangles.add(tri);
		int triIndex = triangles.lastIndexOf(tri, true);
		int[] abPair = triConn[Math.min(a, b)][Math.max(a, b)];
		int[] bcPair = triConn[Math.min(b, c)][Math.max(b, c)];
		int[] caPair = triConn[Math.min(c, a)][Math.max(c, a)];


		if (abPair[0] == -1) {
			abPair[0] = triIndex;
		} else if (abPair[1] == -1) {
			abPair[1] = triIndex;
		} else {
			System.out.println("abPair error!!");
		}
		if (bcPair[0] == -1) {
			bcPair[0] = triIndex;
		} else if (bcPair[1] == -1) {
			bcPair[1] = triIndex;
		} else {
			System.out.println("bcPair error!!");
		}
		if (caPair[0] == -1) {
			caPair[0] = triIndex;
		} else if (caPair[1] == -1) {
			caPair[1] = triIndex;
		} else {
			System.out.println("caPair error!!");
		}
	}
}
