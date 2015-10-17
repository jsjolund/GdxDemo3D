package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.AbstractList;

/**
 * Created by user on 8/3/15.
 */
public class ModelFactory {

	public static Mesh mergeMeshes(AbstractList<Mesh> meshes, AbstractList<Matrix4> transformations) {
		if (meshes.size() == 0) return null;

		int vertexArrayTotalSize = 0;
		int indexArrayTotalSize = 0;

		VertexAttributes va = meshes.get(0).getVertexAttributes();
		int vaA[] = new int[va.size()];
		for (int i = 0; i < va.size(); i++) {
			vaA[i] = va.get(i).usage;
		}

		for (int i = 0; i < meshes.size(); i++) {
			Mesh mesh = meshes.get(i);
			if (mesh.getVertexAttributes().size() != va.size()) {
				meshes.set(i, copyMesh(mesh, true, false, vaA));
			}

			vertexArrayTotalSize += mesh.getNumVertices() * mesh.getVertexSize() / 4;
			indexArrayTotalSize += mesh.getNumIndices();
		}

		final float vertices[] = new float[vertexArrayTotalSize];
		final short indices[] = new short[indexArrayTotalSize];

		int indexOffset = 0;
		int vertexOffset = 0;
		int vertexSizeOffset = 0;
		int vertexSize = 0;

		for (int i = 0; i < meshes.size(); i++) {
			Mesh mesh = meshes.get(i);

			int numIndices = mesh.getNumIndices();
			int numVertices = mesh.getNumVertices();
			vertexSize = mesh.getVertexSize() / 4;
			int baseSize = numVertices * vertexSize;
			VertexAttribute posAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Position);
			int offset = posAttr.offset / 4;
			int numComponents = posAttr.numComponents;

			if (mesh.getVertexSize() == 48) {
				System.out.println("Break");
			}

			{ //uzupelnianie tablicy indeksow
				mesh.getIndices(indices, indexOffset);
				for (int c = indexOffset; c < (indexOffset + numIndices); c++) {
					indices[c] += vertexOffset;
				}
				indexOffset += numIndices;
			}

			mesh.getVertices(0, baseSize, vertices, vertexSizeOffset);
			Mesh.transform(transformations.get(i), vertices, vertexSize, offset, numComponents, vertexOffset, numVertices);
			vertexOffset += numVertices;
			vertexSizeOffset += baseSize;
		}

		Mesh result = new Mesh(true, vertexOffset, indices.length, meshes.get(0).getVertexAttributes());
		result.setVertices(vertices);
		result.setIndices(indices);
		return result;
	}

	public static Mesh copyMesh(Mesh meshToCopy, boolean isStatic, boolean removeDuplicates, final int[] usage) {
		// TODO move this to a copy constructor?
		// TODO duplicate the buffers without double copying the data if possible.
		// TODO perhaps move this code to JNI if it turns out being too slow.
		final int vertexSize = meshToCopy.getVertexSize() / 4;
		int numVertices = meshToCopy.getNumVertices();
		float[] vertices = new float[numVertices * vertexSize];
		meshToCopy.getVertices(0, vertices.length, vertices);
		short[] checks = null;
		VertexAttribute[] attrs = null;
		int newVertexSize = 0;
		if (usage != null) {
			int size = 0;
			int as = 0;
			for (int i = 0; i < usage.length; i++)
				if (meshToCopy.getVertexAttribute(usage[i]) != null) {
					size += meshToCopy.getVertexAttribute(usage[i]).numComponents;
					as++;
				}
			if (size > 0) {
				attrs = new VertexAttribute[as];
				checks = new short[size];
				int idx = -1;
				int ai = -1;
				for (int i = 0; i < usage.length; i++) {
					VertexAttribute a = meshToCopy.getVertexAttribute(usage[i]);
					if (a == null)
						continue;
					for (int j = 0; j < a.numComponents; j++)
						checks[++idx] = (short) (a.offset / 4 + j);
					attrs[++ai] = new VertexAttribute(a.usage, a.numComponents, a.alias);
					newVertexSize += a.numComponents;
				}
			}
		}
		if (checks == null) {
			checks = new short[vertexSize];
			for (short i = 0; i < vertexSize; i++)
				checks[i] = i;
			newVertexSize = vertexSize;
		}

		int numIndices = meshToCopy.getNumIndices();
		short[] indices = null;
		if (numIndices > 0) {
			indices = new short[numIndices];
			meshToCopy.getIndices(indices);
			if (removeDuplicates || newVertexSize != vertexSize) {
				float[] tmp = new float[vertices.length];
				int size = 0;
				for (int i = 0; i < numIndices; i++) {
					final int idx1 = indices[i] * vertexSize;
					short newIndex = -1;
					if (removeDuplicates) {
						for (short j = 0; j < size && newIndex < 0; j++) {
							final int idx2 = j * newVertexSize;
							boolean found = true;
							for (int k = 0; k < checks.length && found; k++) {
								if (tmp[idx2 + k] != vertices[idx1 + checks[k]])
									found = false;
							}
							if (found)
								newIndex = j;
						}
					}
					if (newIndex > 0)
						indices[i] = newIndex;
					else {
						final int idx = size * newVertexSize;
						for (int j = 0; j < checks.length; j++)
							tmp[idx + j] = vertices[idx1 + checks[j]];
						indices[i] = (short) size;
						size++;
					}
				}
				vertices = tmp;
				numVertices = size;
			}
		}

		Mesh result;
		if (attrs == null)
			result = new Mesh(isStatic, numVertices, indices == null ? 0 : indices.length, meshToCopy.getVertexAttributes());
		else
			result = new Mesh(isStatic, numVertices, indices == null ? 0 : indices.length, attrs);
		result.setVertices(vertices, 0, numVertices * newVertexSize);
		result.setIndices(indices);
		return result;
	}

	/**
	 * Translate each vertex along its normal by specified amount.
	 *
	 * @param model
	 * @param amount
	 */
	public static void fatten(Model model, float amount) {
		Vector3 pos = new Vector3();
		Vector3 nor = new Vector3();
		for (Node node : model.nodes) {
			for (NodePart n : node.parts) {
				Mesh mesh = n.meshPart.mesh;
				FloatBuffer buf = mesh.getVerticesBuffer();
				int lastFloat = mesh.getNumVertices() * mesh.getVertexSize() / 4;
				int vertexFloats = (mesh.getVertexSize() / 4);
				VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
				VertexAttribute norAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Normal);
				if (posAttr == null || norAttr == null) {
					throw new IllegalArgumentException("Position/normal vertex attribute not found");
				}
				int pOff = posAttr.offset / 4;
				int nOff = norAttr.offset / 4;

				for (int i = 0; i < lastFloat; i += vertexFloats) {
					pos.x = buf.get(pOff + i);
					pos.y = buf.get(pOff + i + 1);
					pos.z = buf.get(pOff + i + 2);

					nor.x = buf.get(nOff + i);
					nor.y = buf.get(nOff + i + 1);
					nor.z = buf.get(nOff + i + 2);

					nor.nor().scl(amount);

					buf.put(pOff + i, pos.x + nor.x);
					buf.put(pOff + i + 1, pos.y + nor.y);
					buf.put(pOff + i + 2, pos.z + nor.z);
				}
			}
		}
	}

	public static FloatBuffer createBlenderToGdxFloatBuffer(Mesh mesh) {
		Vector3 pos = new Vector3();
		FloatBuffer buf = mesh.getVerticesBuffer();
		FloatBuffer bufNew = BufferUtils.newFloatBuffer(buf.capacity());
		int lastFloat = mesh.getNumVertices() * mesh.getVertexSize() / 4;
		int vertexFloats = (mesh.getVertexSize() / 4);
		VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
		int pOff = posAttr.offset / 4;
		for (int i = pOff; i < lastFloat; i += vertexFloats) {
			pos.x = buf.get(pOff + i);
			pos.y = buf.get(pOff + i + 1);
			pos.z = buf.get(pOff + i + 2);
			bufNew.put(pOff + i, pos.x);
			bufNew.put(pOff + i + 1, pos.z);
			bufNew.put(pOff + i + 2, -pos.y);
		}
		return bufNew;
	}

	public static FloatBuffer setBlenderToGdxFloatBuffer(Mesh mesh) {
		Vector3 pos = new Vector3();
		Vector3 nor = new Vector3();
		FloatBuffer buf = mesh.getVerticesBuffer();
		int lastFloat = mesh.getNumVertices() * mesh.getVertexSize() / 4;
		int vertexFloats = (mesh.getVertexSize() / 4);
		VertexAttribute posAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Position);
		VertexAttribute norAttr = mesh.getVertexAttributes().findByUsage(VertexAttributes.Usage.Normal);
		int pOff = posAttr.offset / 4;
		int nOff = norAttr.offset / 4;
		for (int i = 0; i < lastFloat; i += vertexFloats) {
			pos.x = buf.get(pOff + i);
			pos.y = buf.get(pOff + i + 1);
			pos.z = buf.get(pOff + i + 2);
			buf.put(pOff + i, pos.x);
			buf.put(pOff + i + 1, pos.z);
			buf.put(pOff + i + 2, -pos.y);

			nor.x = buf.get(nOff + i);
			nor.y = buf.get(nOff + i + 1);
			nor.z = buf.get(nOff + i + 2);
			buf.put(nOff + i, nor.x);
			buf.put(nOff + i + 1, nor.z);
			buf.put(nOff + i + 2, -nor.y);
		}
		return buf;
	}

	public static void createOutlineModel(Model model, Color outlineColor, float fattenAmount) {
		fatten(model, fattenAmount);
		for (Material m : model.materials) {
			m.clear();
			m.set(new IntAttribute(IntAttribute.CullFace, Gdx.gl.GL_FRONT));
			m.set(ColorAttribute.createDiffuse(outlineColor));
		}
	}

	public static void flipFaces(ModelInstance modelInstance) {
		for (Node node : modelInstance.nodes) {
			for (NodePart n : node.parts) {
				Mesh mesh = n.meshPart.mesh;
				int numIndices = mesh.getNumIndices();
				ShortBuffer buf = mesh.getIndicesBuffer();
				for (int i = 0; i < numIndices; i += 3) {
					short a = buf.get(i);
					short c = buf.get(i + 2);
					buf.put(i, c);
					buf.put(i + 2, a);
				}
			}
		}
	}

	public static Model buildPlaneModel(final float width,
										final float height, final Material material, final float u1,
										final float v1, final float u2, final float v2) {

		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder bPartBuilder = modelBuilder.part("rect",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates,
				material);
		// NOTE ON TEXTURE REGION, MAY FILL OTHER REGIONS, USE GET region.getU()
		// and so on
		bPartBuilder.setUVRange(u1, v1, u2, v2);
		bPartBuilder.rect(-(width * 0.5f), -(height * 0.5f), 0, (width * 0.5f),
				-(height * 0.5f), 0, (width * 0.5f), (height * 0.5f), 0,
				-(width * 0.5f), (height * 0.5f), 0, 0, 0, -1);

		return (modelBuilder.end());
	}

	public static Model buildCompassModel() {
		float compassScale = 5;
		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(Vector3.Zero,
				Vector3.Y.cpy().scl(compassScale), null,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		modelBuilder.begin();

		Mesh zArrow = arrow.meshes.first().copy(false);
		zArrow.transform(new Matrix4().rotate(Vector3.X, 90));
		modelBuilder.part("part1", zArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.BLUE)));

		modelBuilder.node();
		Mesh yArrow = arrow.meshes.first().copy(false);
		modelBuilder.part("part2", yArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.GREEN)));

		modelBuilder.node();
		Mesh xArrow = arrow.meshes.first().copy(false);
		xArrow.transform(new Matrix4().rotate(Vector3.Z, -90));
		modelBuilder.part("part3", xArrow, GL20.GL_TRIANGLES,
				new Material(ColorAttribute.createDiffuse(Color.RED)));

		arrow.dispose();
		return modelBuilder.end();
	}
}
