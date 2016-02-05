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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Bits;

import java.nio.FloatBuffer;

/**
 * Creates a bidirectional graph over the triangles in the model, which can be used for A* pathfinding.
 * All meshes in the model must be indexed (Mesh.getNumIndices()} > 0). The mesh can be divided into
 * multiple MeshParts (useful for ignoring certain parts of the navmesh or perhaps triggering an event).
 * All MeshParts should share some edge(s) with another in order for the mesh to be fully traversable.
 * <p/>
 * All meshes should be made up of one or more triangles, and should not have any isolated edges or vertices.
 * <p/>
 * Each vertex which has a unique position is stored in a Vector3. Triangle objects map which of these vertices
 * form a triangle according to the winding order in the mesh indices buffer. The winding order is assumed to
 * be the same for each triangle and is read from left to right in the indices buffer.
 * <p/>
 * Each triangle A which shares an edge with another triangle B is associated with an Edge/Connection object. In
 * this object, triangle A is stored as the fromNode, B as toNode. The object also stores the vertices which
 * makes up this edge, in the same winding order as triangle A. Additionally, since B is also connected to A,
 * a mirrored edge is also stored along with B, where the edge has the same winding order as B.
 * <p/>
 * The reason the winding order is important is because each edge in the triangle must have the correct vertices
 * defined as left/right in order for path smoothing to work correctly. Left/right is defined from the perspective
 * of the centroid of the triangle when "looking" at the edge.
 *
 * @author jsjolund
 */
public class NavMeshGraph implements IndexedGraph<Triangle> {

	/**
	 * Class for storing the edge connection data between two adjacent triangles.
	 */
	private static class IndexConnection {
		// The vertex indices which makes up the edge shared between two triangles.
		short edgeVertexIndex1;
		short edgeVertexIndex2;
		// The indices of the two triangles sharing this edge.
		short fromTriIndex;
		short toTriIndex;

		public IndexConnection(short sharedEdgeVertex1Index, short edgeVertexIndex2,
							   short fromTriIndex, short toTriIndex) {
			this.edgeVertexIndex1 = sharedEdgeVertex1Index;
			this.edgeVertexIndex2 = edgeVertexIndex2;
			this.fromTriIndex = fromTriIndex;
			this.toTriIndex = toTriIndex;
		}
	}

	private static final String TAG = "NavMeshGraph";

	private final ArrayMap<Triangle, Array<Edge>> sharedEdges;
	private final ArrayMap<Triangle, Array<Edge>> isolatedEdgesMap;

	private int[] meshPartTriIndexOffsets;
	private int[] meshPartTriCounts;

	private int numDisconnectedEdges;
	private int numConnectedEdges;
	private int numTotalEdges;


	public NavMeshGraph(Model model) {
		short[] indices = getUniquePositionVertexIndices(model.meshes.first());
		Array<IndexConnection> indexConnections = getIndexConnections(indices);
		Vector3[] vertexVectors = createVertexVectors(model.meshes.first(), indices);

		// The triangle graph uses consecutive indices for each unique triangle in the whole model.
		// The Bullet raycast uses MeshPart index combined with triangle index inside the MeshPart,
		// so we need to be able to convert between them.
		int[] meshPartIndexOffsets = new int[model.meshParts.size];
		meshPartTriIndexOffsets = new int[model.meshParts.size];
		for (int i = 0; i < model.meshParts.size; i++) {
			MeshPart meshPart = model.meshParts.get(i);
			meshPartIndexOffsets[i] = meshPart.offset;
			meshPartTriIndexOffsets[i] = meshPart.offset / 3;
		}
		meshPartTriCounts = new int[model.meshParts.size];
		Array<Triangle> triangles = createTriangles(vertexVectors, indices,
				meshPartIndexOffsets, meshPartTriCounts);

		sharedEdges = createSharedEdgesMap(indexConnections, triangles, vertexVectors);
		isolatedEdgesMap = createIsolatedEdgesMap(sharedEdges);

		// Count edges of different types
		for (Array<Edge> edges : isolatedEdgesMap.values()) {
			numDisconnectedEdges += edges.size;
		}
		for (Array<Edge> edges : sharedEdges.values()) {
			numConnectedEdges += edges.size;
		}
		numConnectedEdges /= 2;
		numTotalEdges = numConnectedEdges + numDisconnectedEdges;
		Gdx.app.debug(TAG, "MeshParts: total=" + getMeshPartCount() + ", Triangles: total=" + getNodeCount()
			+ ", Edges: connected=" + getEdgeCountShared() + ", disconnected=" + getEdgeCountIsolated() + ", total=%s"
			+ getEdgeCountTotal());
	}


	/**
	 * Map the isolated edges for each triangle which does not have all three edges connected to other triangles.
	 *
	 * @param connectionMap
	 * @return
	 */
	private static ArrayMap<Triangle, Array<Edge>> createIsolatedEdgesMap(ArrayMap<Triangle, Array<Edge>> connectionMap) {

		ArrayMap<Triangle, Array<Edge>> disconnectionMap = new ArrayMap<Triangle, Array<Edge>>();

		for (int i = 0; i < connectionMap.size; i++) {
			Triangle tri = connectionMap.getKeyAt(i);
			Array<Edge> connectedEdges = connectionMap.getValueAt(i);

			Array<Edge> disconnectedEdges = new Array<Edge>();
			disconnectionMap.put(tri, disconnectedEdges);

			if (connectedEdges.size < 3) {
				// This triangle does not have all edges connected to other triangles
				boolean ab = true;
				boolean bc = true;
				boolean ca = true;
				for (Edge edge : connectedEdges) {
					if (edge.rightVertex == tri.a && edge.leftVertex == tri.b) ab = false;
					else if (edge.rightVertex == tri.b && edge.leftVertex == tri.c) bc = false;
					else if (edge.rightVertex == tri.c && edge.leftVertex == tri.a) ca = false;
				}
				if (ab) disconnectedEdges.add(new Edge(tri, null, tri.a, tri.b));
				if (bc) disconnectedEdges.add(new Edge(tri, null, tri.b, tri.c));
				if (ca) disconnectedEdges.add(new Edge(tri, null, tri.c, tri.a));
			}
			int totalEdges = (connectedEdges.size + disconnectedEdges.size);
			if (totalEdges != 3) {
				Gdx.app.debug(TAG, "Wrong number of edges (" + totalEdges + ") in triangle " + tri.getIndex());
			}
		}
		return disconnectionMap;
	}


	/**
	 * Creates a map over each triangle and its Edge connections to other triangles. Each edge must follow the
	 * vertex winding order of the triangle associated with it. Since all triangles are assumed to have the same
	 * winding order, this means if two triangles connect, each must have its own edge connection data, where the
	 * edge follows the same winding order as the triangle which owns the edge data.
	 *
	 * @param indexConnections
	 * @param triangles
	 * @param vertexVectors
	 * @return
	 */
	private static ArrayMap<Triangle, Array<Edge>> createSharedEdgesMap(
			Array<IndexConnection> indexConnections, Array<Triangle> triangles, Vector3[] vertexVectors) {

		ArrayMap<Triangle, Array<Edge>> connectionMap = new ArrayMap<Triangle, Array<Edge>>();
		connectionMap.ordered = true;

		for (Triangle tri : triangles) {
			connectionMap.put(tri, new Array<Edge>());
		}

		for (IndexConnection i : indexConnections) {
			Triangle fromNode = triangles.get(i.fromTriIndex);
			Triangle toNode = triangles.get(i.toTriIndex);
			Vector3 edgeVertexA = vertexVectors[i.edgeVertexIndex1];
			Vector3 edgeVertexB = vertexVectors[i.edgeVertexIndex2];

			Edge edge = new Edge(fromNode, toNode, edgeVertexA, edgeVertexB);
			connectionMap.get(fromNode).add(edge);
			fromNode.connections.add(edge);
		}
		return connectionMap;
	}

	/**
	 * Get an array of the vertex indices from the mesh. Any vertices which share the same position will be counted
	 * as a single vertex and share the same index. That is, position duplicates will be filtered out.
	 *
	 * @param mesh
	 * @return
	 */
	private static short[] getUniquePositionVertexIndices(Mesh mesh) {
		FloatBuffer verticesBuffer = mesh.getVerticesBuffer();
		int positionOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;
		// Number of array elements which make up a vertex
		int vertexSize = mesh.getVertexSize() / 4;
		// The indices tell us which vertices are part of a triangle.
		short[] indices = new short[mesh.getNumIndices()];
		mesh.getIndices(indices);
		// Marks true if an index has already been compared to avoid unnecessary comparisons
		Bits handledIndices = new Bits(mesh.getNumIndices());

		for (int i = 0; i < indices.length; i++) {
			short indexI = indices[i];
			if (handledIndices.get(indexI)) {
				// Index handled in an earlier iteration
				continue;
			}
			int vBufIndexI = indexI * vertexSize + positionOffset;
			float xi = verticesBuffer.get(vBufIndexI++);
			float yi = verticesBuffer.get(vBufIndexI++);
			float zi = verticesBuffer.get(vBufIndexI++);
			for (int j = i + 1; j < indices.length; j++) {
				short indexJ = indices[j];
				int vBufIndexJ = indexJ * vertexSize + positionOffset;
				float xj = verticesBuffer.get(vBufIndexJ++);
				float yj = verticesBuffer.get(vBufIndexJ++);
				float zj = verticesBuffer.get(vBufIndexJ++);
				if (xi == xj && yi == yj && zi == zj) {
					indices[j] = indexI;
				}
			}
			handledIndices.set(indexI);
		}
		return indices;
	}


	/**
	 * Creates triangle objects according to the index array, using Vector3 objects from the provided vector array.
	 *
	 * @param vertexVectors
	 * @param indices
	 * @param meshPartIndexOffsets
	 * @param meshPartTriCounts
	 * @return
	 */
	private static Array<Triangle> createTriangles(Vector3[] vertexVectors, short[] indices,
												   int[] meshPartIndexOffsets, int[] meshPartTriCounts) {
		Array<Triangle> triangles = new Array<Triangle>();
		triangles.ordered = true;
		short i = 0;
		short j = 0;
		int triIndex = 0;
		int meshPartIndex = -1;
		while (i < indices.length) {
			if (j < meshPartIndexOffsets.length && i >= meshPartIndexOffsets[j]) {
				meshPartIndex++;
				j++;
			}
			triangles.add(new Triangle(
					vertexVectors[indices[i++]],
					vertexVectors[indices[i++]],
					vertexVectors[indices[i++]],
					triIndex, meshPartIndex));
			meshPartTriCounts[meshPartIndex]++;
			triIndex++;
		}
		return triangles;
	}

	/**
	 * Creates Vector3 objects from the vertices of the mesh. The resulting array follows the ordering of the provided
	 * index array.
	 *
	 * @param mesh
	 * @param indices
	 * @return
	 */
	private static Vector3[] createVertexVectors(Mesh mesh, short[] indices) {
		FloatBuffer verticesBuffer = mesh.getVerticesBuffer();
		int positionOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;
		int vertexSize = mesh.getVertexSize() / 4;
		Vector3[] vertexVectors = new Vector3[mesh.getNumIndices()];
		for (int i = 0; i < indices.length; i++) {
			short index = indices[i];
			int a = index * vertexSize + positionOffset;
			float x = verticesBuffer.get(a++);
			float y = verticesBuffer.get(a++);
			float z = verticesBuffer.get(a);
			vertexVectors[index] = new Vector3(x, y, z);
		}
		return vertexVectors;
	}

	/**
	 * Loops through each triangle among the indices and searches for edges shared with other triangles.
	 * TODO: Can be optimized by removing triangles which have all their sides connected at some point.
	 *
	 * @param indices
	 * @return
	 */
	private static Array<IndexConnection> getIndexConnections(short[] indices) {
		Array<IndexConnection> indexConnections = new Array<IndexConnection>();
		indexConnections.ordered = true;
		short[] edge = {-1, -1};
		short i = 0;
		short j, a0, a1, a2, b0, b1, b2, triAIndex, triBIndex;
		while (i < indices.length) {
			triAIndex = (short) (i / 3);
			a0 = indices[i++];
			a1 = indices[i++];
			a2 = indices[i++];
			j = i;
			while (j < indices.length) {
				triBIndex = (short) (j / 3);
				b0 = indices[j++];
				b1 = indices[j++];
				b2 = indices[j++];
				if (hasSharedEdgeIndices(a0, a1, a2, b0, b1, b2, edge)) {
					indexConnections.add(new IndexConnection(edge[0], edge[1], triAIndex, triBIndex));
					indexConnections.add(new IndexConnection(edge[1], edge[0], triBIndex, triAIndex));
					edge[0] = -1;
					edge[1] = -1;
				}
			}
		}
		return indexConnections;
	}

	/**
	 * Checks if the two triangles have shared vertex indices. The edge will always follow the vertex winding order
	 * of the triangle A. Since all triangles have the same winding order, triangle A should have the opposite
	 * edge direction to triangle B.
	 *
	 * @param a0   Vertex index on triangle A
	 * @param a1
	 * @param a2
	 * @param b0   Vertex index on triangle B
	 * @param b1
	 * @param b2
	 * @param edge Output, the indices of the shared vertices in the winding order of triangle A.
	 * @return True if the triangles share an edge.
	 */
	private static boolean hasSharedEdgeIndices(short a0, short a1, short a2,
												short b0, short b1, short b2, short[] edge) {
		boolean match0 = (a0 == b0 || a0 == b1 || a0 == b2);
		boolean match1 = (a1 == b0 || a1 == b1 || a1 == b2);
		if (!match0 && !match1) {
			return false;
		} else if (match0 && match1) {
			edge[0] = a0;
			edge[1] = a1;
			return true;
		}
		boolean match2 = (a2 == b0 || a2 == b1 || a2 == b2);
		if (match0 && match2) {
			edge[0] = a2;
			edge[1] = a0;
			return true;
		} else if (match1 && match2) {
			edge[0] = a1;
			edge[1] = a2;
			return true;
		}
		return false;
	}

	@Override
	public int getNodeCount() {
		return sharedEdges.size;
	}

	/**
	 * The number of triangles in specified MeshPart.
	 *
	 * @param meshPartIndex
	 * @return
	 */
	public int getTriangleCount(int meshPartIndex) {
		return meshPartTriCounts[meshPartIndex];
	}

	@Override
	public int getIndex (Triangle node) {
		return node.getIndex();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Array<Connection<Triangle>> getConnections(Triangle fromNode) {
		return (Array<Connection<Triangle>>) (Array<?>) sharedEdges.getValueAt(fromNode.triIndex);
	}

	/**
	 * Get triangle edges which do not connect to another triangle.
	 *
	 * @param triIndex
	 * @return
	 */
	public Array<Edge> getIsolatedEdges(int triIndex) {
		return isolatedEdgesMap.getValueAt(triIndex);
	}

	/**
	 *
	 * Get triangle edges which are connected to two triangles
	 *
	 * @param triIndex
	 * @return
	 */
	public Array<Edge> getSharedEdges(int triIndex) {
		return sharedEdges.getValueAt(triIndex);
	}


	/**
	 * Get a triangle using its index in the pathfinding graph.
	 *
	 * @param graphTriIndex
	 * @return
	 */
	public Triangle getTriangleFromGraphIndex(int graphTriIndex) {
		return sharedEdges.getKeyAt(graphTriIndex);
	}

	/**
	 * Get a triangle using the index of a MeshPart and triangle index inside the Meshpart.
	 *
	 * @param meshPartIndex
	 * @param meshPartTriIndex
	 * @return
	 */
	public Triangle getTriangleFromMeshPart(int meshPartIndex, int meshPartTriIndex) {
		return sharedEdges.getKeyAt(meshPartTriIndexOffsets[meshPartIndex] + meshPartTriIndex);
	}

	/**
	 * The number of MeshParts in the navigation mesh.
	 *
	 * @return
	 */
	public int getMeshPartCount() {
		return meshPartTriIndexOffsets.length;
	}

	/**
	 * The total number of edges in the navigation mesh.
	 *
	 * @return
	 */
	public int getEdgeCountTotal() {
		return numTotalEdges;
	}

	/**
	 * The number of (unique) edges which are connected to two triangles.
	 *
	 * @return
	 */
	public int getEdgeCountShared() {
		return numConnectedEdges;
	}

	/**
	 * The number of edges which are only connected to one triangle.
	 *
	 * @return
	 */
	public int getEdgeCountIsolated() {
		return numDisconnectedEdges;
	}


}
