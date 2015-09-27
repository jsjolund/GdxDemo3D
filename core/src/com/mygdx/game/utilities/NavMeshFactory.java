package com.mygdx.game.utilities;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

import java.nio.FloatBuffer;

/**
 * Created by Johannes Sjolund on 9/24/15.
 */
public class NavMeshFactory {

	private static class IndexConnection {
		short sharedEdgeVertex1Index;
		short sharedEdgeVertex2Index;
		short triAIndex;
		short triBIndex;

		public IndexConnection(short sharedEdgeVertex1Index, short sharedEdgeVertex2Index,
							   short triAIndex, short triBIndex) {
			this.sharedEdgeVertex1Index = sharedEdgeVertex1Index;
			this.sharedEdgeVertex2Index = sharedEdgeVertex2Index;
			this.triAIndex = triAIndex;
			this.triBIndex = triBIndex;
		}
	}

	private static final boolean useZeroWidthEdges = false;

	public static ArrayMap<Triangle, Array<Connection<Triangle>>> createNavMeshConnections(Mesh mesh) {
		short[] indices = getUniquePositionVertexIndices(mesh);
		Array<IndexConnection> indexConnections = getIndexConnections(indices);
		Vector3[] vertexVectors = createVertexVectors(mesh, indices);
		Array<Triangle> triangles = createTriangles(vertexVectors, indices);
		return createConnections(indexConnections, triangles, vertexVectors);
	}

	private static short[] getUniquePositionVertexIndices(Mesh mesh) {
		FloatBuffer verticesBuffer = mesh.getVerticesBuffer();
		int positionOffset = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position).offset / 4;
		// Number of array elements which make up a vertex
		int vertexSize = mesh.getVertexSize() / 4;
		// The indices tell us which vertices are part of a triangle.
		short[] indices = new short[mesh.getNumIndices()];
		mesh.getIndices(indices);

		for (int i = 0; i < indices.length; i++) {
			short index = indices[i];
			int a = index * vertexSize + positionOffset;
			float xi = verticesBuffer.get(a++);
			float yi = verticesBuffer.get(a++);
			float zi = verticesBuffer.get(a++);
			for (int j = i + 1; j < indices.length; j++) {
				short jndex = indices[j];
				int b = jndex * vertexSize + positionOffset;
				float xj = verticesBuffer.get(b++);
				float yj = verticesBuffer.get(b++);
				float zj = verticesBuffer.get(b++);
				if (xi == xj && yi == yj && zi == zj) {
					indices[j] = index;
				}
			}
		}
		return indices;
	}

	private static ArrayMap<Triangle, Array<Connection<Triangle>>> createConnections(
			Array<IndexConnection> indexConnections, Array<Triangle> triangles, Vector3[] vertexVectors) {

		ArrayMap<Triangle, Array<Connection<Triangle>>> connectionMap =
				new ArrayMap<Triangle, Array<Connection<Triangle>>>();
		connectionMap.ordered = true;

		for (Triangle tri : triangles) {
			connectionMap.put(tri, new Array<Connection<Triangle>>());
		}

		for (IndexConnection i : indexConnections) {
			Triangle fromNode = triangles.get(i.triAIndex);
			Triangle toNode = triangles.get(i.triBIndex);
			Vector3 edgeVertexA = vertexVectors[i.sharedEdgeVertex1Index];
			Vector3 edgeVertexB = vertexVectors[i.sharedEdgeVertex2Index];

			Edge edge = new Edge(fromNode, toNode, edgeVertexA, edgeVertexB);
			System.out.println(edge);

			connectionMap.get(fromNode).add(edge);
			fromNode.connections.add(edge);
		}
		return connectionMap;
	}

	private static Array<Triangle> createTriangles(Vector3[] vertexVectors, short[] indices) {
		Array<Triangle> triangles = new Array<Triangle>();
		triangles.ordered = true;
		short i = 0;
		int triIndex = 0;
		while (i < indices.length) {
			triangles.add(new Triangle(
					vertexVectors[indices[i++]],
					vertexVectors[indices[i++]],
					vertexVectors[indices[i++]],
					triIndex));
			triIndex++;
		}
		return triangles;
	}

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

	private static Array<IndexConnection> getIndexConnections(short[] indices) {
		Array<IndexConnection> indexConnections = new Array<IndexConnection>();
		indexConnections.ordered = true;
		short[] triA = new short[3];
		short[] triB = new short[3];
		short[] sharedIndices = new short[2];
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
				if (hasSharedEdgeIndices(triA, triB, sharedIndices)) {
					indexConnections.add(new IndexConnection(
							sharedIndices[0], sharedIndices[1],
							triAIndex, triBIndex));
					indexConnections.add(new IndexConnection(
							sharedIndices[1], sharedIndices[0],
							triBIndex, triAIndex));
				}
			}
		}
		return indexConnections;
	}

	private static boolean hasSharedEdgeIndices(short[] triA, short[] triB, short[] sharedIndices) {
		boolean oneShared = false;
		boolean twoShared = false;
		for (int i = 0; i < 3; i++) {
			short triAIndex = triA[i];
			if (triAIndex == triB[0] || triAIndex == triB[1] || triAIndex == triB[2]) {
				if (oneShared) {
					sharedIndices[1] = triAIndex;
					twoShared = true;
					break;
				} else {
					sharedIndices[0] = triAIndex;
					oneShared = true;
				}
			}
		}
		if (twoShared) {
			return true;
		} else if (useZeroWidthEdges && oneShared) {
			sharedIndices[1] = sharedIndices[0];
			return true;
		} else {
			return false;
		}
	}

}
