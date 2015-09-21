package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.systems.MyTriangleRaycastCallback;

import java.nio.FloatBuffer;

/**
 * Created by Johannes Sjolund on 9/19/15.
 */
public class NavMesh implements Graph, Disposable {

	public static final String tag = "NavMesh";

	public Array<Triangle> triangles;
	public Array<Connection> edges;

	private final Vector3 rayFrom = new Vector3();
	private final Vector3 rayTo = new Vector3();
	private final MyTriangleRaycastCallback triangleRaycastCallback;
	private final btBvhTriangleMeshShape collisionObject;
	private final EdgeIndexConnectionMap edgeMap;

	// For debug
	public Array<Vector3[]> triConnections = new Array<Vector3[]>();

	public NavMesh(Mesh mesh, btBvhTriangleMeshShape collisionObject) {
		this.collisionObject = collisionObject;
		triangleRaycastCallback = new MyTriangleRaycastCallback(rayFrom, rayTo);
		triangles = new Array<Triangle>();
		edges = new Array<Connection>();

		FloatBuffer vertBuf = mesh.getVerticesBuffer();
		VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
		int posOffset = posAttr.offset / 4;
		int numIndices = mesh.getNumIndices();

		// The indices tell us which vertices are part of a triangle.
		short[] indices = new short[numIndices];
		mesh.getIndices(indices);

		// Number of array elements which make up a vertex
		int vertexSize = mesh.getVertexSize() / 4;

		// Vertices will be stored here
		Vector3[] vertices = new Vector3[numIndices];

		populateVertexArray(vertBuf, indices, vertices, vertexSize, posOffset);

		edgeMap = new EdgeIndexConnectionMap(numIndices);
		triangles.ordered = true;
		populateTriangleArrayEdgeMap(indices, vertices);

		edgeMap.build();
	}

	/**
	 * Loop through the indices and connect their vertices into triangles.
	 * Each edge of the triangle is mapped by vertex index in order to later
	 * find out which triangles share which edges.
	 *
	 * @param vertices
	 * @param indices
	 */
	private void populateTriangleArrayEdgeMap(short[] indices, Vector3[] vertices) {
		int a = -1;
		int b = -1;
		int c = -1;
		for (int vIndex = 0; vIndex < indices.length; vIndex++) {
			short i = indices[vIndex];
			if (vIndex == indices.length - 1) {
				Triangle tri = new Triangle(vertices[a], vertices[b], vertices[i]);
				triangles.add(tri);
				edgeMap.add(a, b, i, triangles.lastIndexOf(tri, false));
				break;
			}
			if (a == -1) {
				a = i;
			} else if (b == -1) {
				b = i;
			} else if (c == -1) {
				c = i;
			} else {
				Triangle tri = new Triangle(vertices[a], vertices[b], vertices[c]);
				triangles.add(tri);
				edgeMap.add(a, b, c, triangles.lastIndexOf(tri, false));
				a = i;
				b = -1;
				c = -1;
			}
		}
	}

	/**
	 * Populates the vertex vector array, and removes any duplicate non-unique indices from the index array.
	 * <p/>
	 * A unique index does not always point to a unique vertex. Remove any duplicate vertices
	 * by checking if an identical vertex vector was already created. If so, change the index
	 * to point to the old vertex.
	 *
	 * @param vertBuf    Vertex buffer from which the vertex positions will be read.
	 * @param indices    The indices of the mesh. Will be corrected when an index points to a duplicate vertex.
	 * @param vertices   Where the vertices will be stored
	 * @param vertexSize The size of a vertex in array elements.
	 * @param posOffset  Offset of the vertex position attributes for each vertex/index
	 */
	private static void populateVertexArray(FloatBuffer vertBuf,
											short[] indices, Vector3[] vertices,
											int vertexSize, int posOffset) {
		for (int i = 0; i < indices.length; i++) {
			short index = indices[i];
			int j = index * vertexSize + posOffset;
			float x = vertBuf.get(j++);
			float y = vertBuf.get(j++);
			float z = vertBuf.get(j++);
			Vector3 newVertex = new Vector3(x, y, z);
			// Check if this vertex already exists. TODO: Inefficient...
			boolean dupFound = false;
			for (int vi = 0; vi < indices.length; vi++) {
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
	}

	@Override
	public void dispose() {
		collisionObject.dispose();
		triangleRaycastCallback.dispose();
	}

	public void rayTest(Ray ray, float maxDistance) {
		rayFrom.set(ray.origin);
		rayTo.set(ray.direction).scl(maxDistance).add(rayFrom);
		triangleRaycastCallback.clear();
		triangleRaycastCallback.setFrom(rayFrom);
		triangleRaycastCallback.setTo(rayTo);
		collisionObject.performRaycast(triangleRaycastCallback, rayFrom, rayTo);
		if (triangleRaycastCallback.triangleIndex != -1) {
			Gdx.app.debug(tag, triangles.get(triangleRaycastCallback.triangleIndex).toString());
		}
	}

	@Override
	public Array<Connection> getConnections(Object fromNode) {
		return edges;
	}

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

	private class EdgeIndexConnectionMap {

		int[][][] edgeConnections;

		public EdgeIndexConnectionMap(int numIndices) {
			// Edge connection map. First two dimensions map two indices which forms an edge.
			// The third dimension takes the triangle array indices of the one or two triangles which share this edge.
			// TODO: This takes more memory than really needed
			edgeConnections = new int[numIndices][numIndices][2];
			for (int i = 0; i < edgeConnections.length; i++) {
				for (int j = 0; j < edgeConnections[i].length; j++) {
					edgeConnections[i][j][0] = -1;
					edgeConnections[i][j][1] = -1;
				}
			}
		}

		public void add(int a, int b, int c, int triIndex) {
			int[] abPair = edgeConnections[Math.min(a, b)][Math.max(a, b)];
			int[] bcPair = edgeConnections[Math.min(b, c)][Math.max(b, c)];
			int[] caPair = edgeConnections[Math.min(c, a)][Math.max(c, a)];
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

		public void build() {
			// Create the edge connections between triangles
			for (int i = 0; i < edgeConnections.length; i++) {
				for (int j = 0; j < edgeConnections[i].length; j++) {
					int tri0Index = edgeConnections[i][j][0];
					int tri1Index = edgeConnections[i][j][1];
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
	}


}
