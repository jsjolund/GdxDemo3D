package com.mygdx.game.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

import java.nio.FloatBuffer;

/**
 * Created by Johannes Sjolund on 9/27/15.
 */
public class NavMeshGraph implements IndexedGraph<Triangle> {

	private ArrayMap<Triangle, Array<Connection<Triangle>>> map;
	private int[] meshPartTriIndexOffsets;

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
	 * @param model
	 * @param transform
	 */
	public NavMeshGraph(Model model) {
		short[] indices = getUniquePositionVertexIndices(model.meshes.first());
		Array<IndexConnection> indexConnections = getIndexConnections(indices);

		int[] meshPartIndexOffsets = new int[model.meshParts.size];
		meshPartTriIndexOffsets = new int[model.meshParts.size];
		for (int i = 0; i < model.meshParts.size; i++) {
			MeshPart meshPart = model.meshParts.get(i);
			meshPartIndexOffsets[i] = meshPart.offset;
			meshPartTriIndexOffsets[i] = meshPart.offset / 3;
		}

		Vector3[] vertexVectors = createVertexVectors(model.meshes.first(), indices);
		Array<Triangle> triangles = createTriangles(vertexVectors, indices, meshPartIndexOffsets);

		map = createConnections(indexConnections, triangles, vertexVectors);
	}

	/**
	 * Get an array of the vertex indices from the mesh. Any vertices which share the same position will be counted
	 * as a single vertex and share the same index. That is, position duplicates will be filtered out.
	 * <p/>
	 * TODO: This can probably be optimized further
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

		for (int i = 0; i < indices.length; i++) {
			short indexI = indices[i];
			int a = indexI * vertexSize + positionOffset;
			float xi = verticesBuffer.get(a++);
			float yi = verticesBuffer.get(a++);
			float zi = verticesBuffer.get(a++);
			for (int j = i + 1; j < indices.length; j++) {
				short indexJ = indices[j];
				int b = indexJ * vertexSize + positionOffset;
				float xj = verticesBuffer.get(b++);
				float yj = verticesBuffer.get(b++);
				float zj = verticesBuffer.get(b++);
				if (xi == xj && yi == yj && zi == zj) {
					indices[j] = indexI;
				}
			}
		}
		return indices;
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
	private static ArrayMap<Triangle, Array<Connection<Triangle>>> createConnections(
			Array<IndexConnection> indexConnections, Array<Triangle> triangles, Vector3[] vertexVectors) {

		ArrayMap<Triangle, Array<Connection<Triangle>>> connectionMap =
				new ArrayMap<Triangle, Array<Connection<Triangle>>>();
		connectionMap.ordered = true;

		for (Triangle tri : triangles) {
			connectionMap.put(tri, new Array<Connection<Triangle>>());
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
	 * Creates triangle objects according to the index array, using Vector3 objects from the provided vector array.
	 *
	 * @param vertexVectors
	 * @param indices
	 * @param meshPartIndexOffsets
	 * @return
	 */
	private static Array<Triangle> createTriangles(Vector3[] vertexVectors, short[] indices, int[] meshPartIndexOffsets) {
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
	 * TODO: Can be optimized a lot by removing triangles which have all their sides connected at some point.
	 *
	 * @param indices
	 * @return
	 */
	private static Array<IndexConnection> getIndexConnections(short[] indices) {
		Array<IndexConnection> indexConnections = new Array<IndexConnection>();
		indexConnections.ordered = true;
		short[] triA = new short[3];
		short[] triB = new short[3];
		short[] edgeA = new short[2];
		short[] edgeB = new short[2];
		short i = 0;

		while (i < indices.length) {
			short triAIndex = (short) (i / 3);
			triA[0] = indices[i++];
			triA[1] = indices[i++];
			triA[2] = indices[i++];
			short j = i;
			while (j < indices.length) {
				short triBIndex = (short) (j / 3);
				triB[0] = indices[j++];
				triB[1] = indices[j++];
				triB[2] = indices[j++];
				if (hasSharedEdgeIndices(triA, triB, edgeA, edgeB, false)) {
					indexConnections.add(new IndexConnection(
							edgeA[0], edgeA[1],
							triAIndex, triBIndex));
					indexConnections.add(new IndexConnection(
							edgeB[0], edgeB[1],
							triBIndex, triAIndex));
				}
			}
		}
		return indexConnections;
	}

	/**
	 * Checks if the two triangles have shared vertex indices. The edges will always follow the vertex winding orders
	 * of the two triangles. Assuming all triangles have the same winding order, triangle A should have the opposite
	 * edge direction to triangle B.
	 *
	 * @param triA
	 * @param triB
	 * @param edgeA
	 * @param edgeB
	 * @param useZeroLengthEdges If true, then one shared vertex will count as a zero length shared edge.
	 * @return True if the triangles share an edge.
	 */
	private static boolean hasSharedEdgeIndices(short[] triA, short[] triB,
												short[] edgeA, short[] edgeB,
												boolean useZeroLengthEdges) {
		int shared = 0;
		int sharedI = 0;
		for (int i = 0; i < 3; i++) {
			short triAIndex = triA[i];
			if (triAIndex == triB[0] || triAIndex == triB[1] || triAIndex == triB[2]) {
				if (shared == 0) {
					// Shared index found
					edgeA[0] = triAIndex;
					sharedI = i;
					shared++;
				} else {
					// One is already shared
					if (i == 2 && sharedI == 0) {
						edgeA[1] = edgeA[0];
						edgeA[0] = triAIndex;
					} else {
						edgeA[1] = triAIndex;
					}
					shared++;
					break;
				}
			}
		}
		if (useZeroLengthEdges && shared == 1) {
			edgeA[1] = edgeA[0];
			edgeB[0] = edgeA[0];
			edgeB[1] = edgeA[0];
			return true;
		} else if (shared == 2) {
			edgeB[1] = edgeA[0];
			edgeB[0] = edgeA[1];
			return true;
		}
		return false;
	}

	@Override
	public int getNodeCount() {
		return map.size;
	}

	@Override
	public Array<Connection<Triangle>> getConnections(Triangle fromNode) {
		return map.getValueAt(fromNode.triIndex);
	}

	public Triangle getTriangleFromGraphIndex(int graphTriIndex) {
		return map.getKeyAt(graphTriIndex);
	}

	public Triangle getTriangleFromMeshPart(int meshPartIndex, int triIndex) {
		return map.getKeyAt(meshPartTriIndexOffsets[meshPartIndex] + triIndex);
	}

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

}
