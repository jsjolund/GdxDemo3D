package com.mygdx.game;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import java.nio.FloatBuffer;

/**
 * Created by user on 9/19/15.
 */
public class NavMesh implements Graph {

	public Array<Triangle> triangles = new Array<Triangle>();
	public Array<Connection> edges = new Array<Connection>();

	// For debug
	public Array<Vector3[]> triConnections = new Array<Vector3[]>();

	public class Edge implements Connection<Triangle> {
		public Triangle fromNode;
		public Triangle toNode;

		public Edge(Triangle fromNode, Triangle toNode) {
			this.fromNode = fromNode;
			this.toNode = toNode;
		}

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
		public Vector3 centroid;

		public Triangle(Vector3 a, Vector3 b, Vector3 c) {
			this.a = a;
			this.b = b;
			this.c = c;
			centroid = new Vector3(a).add(b).add(c).scl(1f / 3f);
		}

	}

	@Override
	public Array<Connection> getConnections(Object fromNode) {
		return edges;
	}

	public NavMesh(Mesh mesh) {
		Vector3[] vertices;

		FloatBuffer vertBuf = mesh.getVerticesBuffer();
		VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
		int posOffset = posAttr.offset / 4;
		int numIndices = mesh.getNumIndices();
		short[] indices = new short[numIndices];
		mesh.getIndices(indices);

		// A unique index does not always point ot a unique vertex. Remove any duplicate vertices by setting
		vertices = new Vector3[numIndices];
		for (int i = 0; i < numIndices; i++) {
			short index = indices[i];
			int j = index * mesh.getVertexSize() / 4 + posOffset;
			float x = vertBuf.get(j++);
			float y = vertBuf.get(j++);
			float z = vertBuf.get(j++);
			Vector3 newVertex = new Vector3(x, z, -y);
			// Check if this vertex already exists. Inefficient...
			boolean dupFound = false;
			for (int vi = 0; vi < numIndices; vi++) {
				Vector3 oldVertex = vertices[vi];
				if (oldVertex != null
						&& oldVertex.x == newVertex.x
						&& oldVertex.y == newVertex.y
						&& oldVertex.z == newVertex.z) {
					// Duplicate vertex found
					dupFound = true;
					indices[i] = (short) vi;
					break;
				}
			}
			if (!dupFound) {
				vertices[index] = newVertex;
			}
		}

		// Edge connection map. First two are the vertex indices belonging to a triangle. In the third
		// will later be stored which indices in the triangle array connect to these two vertices.
		int[][][] triConn = new int[numIndices][numIndices][2];
		for (int i = 0; i < triConn.length; i++) {
			for (int j = 0; j < triConn[i].length; j++) {
				triConn[i][j][0] = -1;
				triConn[i][j][1] = -1;
			}
		}
		// Loop through the indices and connect their vertices into triangles.
		triangles.ordered = true;
		int a = -1;
		int b = -1;
		int c = -1;
		for (int vIndex = 0; vIndex < numIndices; vIndex++) {
			short i = indices[vIndex];
			if (vIndex == numIndices - 1) {
				addTriangle(a, b, i, vertices, triConn);
				break;
			}
			if (a == -1) {
				a = i;
			} else if (b == -1) {
				b = i;
			} else if (c == -1) {
				c = i;
			} else {
				addTriangle(a, b, c, vertices, triConn);
				a = i;
				b = -1;
				c = -1;
			}
		}

		// Create the edge connections between triangles
		for (int i = 0; i < triConn.length; i++) {
			for (int j = 0; j < triConn[i].length; j++) {
				int tri0Index = triConn[i][j][0];
				int tri1Index = triConn[i][j][1];
				if (tri0Index != -1 && tri1Index != -1) {
					// Debug which connections are established
					Vector3[] pair = new Vector3[]{triangles.get(tri0Index).centroid, triangles.get(tri1Index).centroid};
					triConnections.add(pair);
					// Add Connection
					edges.add(new Edge(triangles.get(tri0Index), triangles.get(tri1Index)));
					edges.add(new Edge(triangles.get(tri1Index), triangles.get(tri0Index)));
				}
			}
		}
	}

	/**
	 * Takes three vertex indices, creates a new triangle, maps the edges of the triangle to later create connection
	 * data (in the form of mesh edges).
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @param vertices
	 * @param triConn
	 */
	private void addTriangle(int a, int b, int c, Vector3[] vertices, int[][][] triConn) {
		Triangle tri = new Triangle(vertices[a], vertices[b], vertices[c]);
		triangles.add(tri);
		int triIndex = triangles.lastIndexOf(tri, false);

		int[] abPair = triConn[Math.min(a, b)][Math.max(a, b)];
		int[] bcPair = triConn[Math.min(b, c)][Math.max(b, c)];
		int[] caPair = triConn[Math.min(c, a)][Math.max(c, a)];
		if (abPair[0] == -1) {
			abPair[0] = triIndex;
		} else if (abPair[1] == -1) {
			abPair[1] = triIndex;
		}
		if (bcPair[0] == -1) {
			bcPair[0] = triIndex;
		} else if (bcPair[1] == -1) {
			bcPair[1] = triIndex;
		}
		if (caPair[0] == -1) {
			caPair[0] = triIndex;
		} else if (caPair[1] == -1) {
			caPair[1] = triIndex;
		}

	}
}
