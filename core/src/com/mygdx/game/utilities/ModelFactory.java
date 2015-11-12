/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.utilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import java.nio.FloatBuffer;

/**
 * Created by user on 8/3/15.
 */
public class ModelFactory {

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

	public static void createOutlineModel(Model model, Color outlineColor, float fattenAmount) {
		fatten(model, fattenAmount);
		for (Material m : model.materials) {
			m.clear();
			m.set(new IntAttribute(IntAttribute.CullFace, Gdx.gl.GL_FRONT));
			m.set(ColorAttribute.createDiffuse(outlineColor));
		}
	}

	public static Model buildPlaneModel(final float width,
										final float height, final Material material, final float u1,
										final float v1, final float u2, final float v2) {

		ModelBuilder modelBuilder = new ModelBuilder();
		modelBuilder.begin();
		MeshPartBuilder bPartBuilder = modelBuilder.part("rect", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates, material);
		bPartBuilder.setUVRange(u1, v1, u2, v2);
		bPartBuilder.rect(-(width * 0.5f), -(height * 0.5f), 0, (width * 0.5f),
				-(height * 0.5f), 0, (width * 0.5f), (height * 0.5f), 0,
				-(width * 0.5f), (height * 0.5f), 0, 0, 0, -1);

		return (modelBuilder.end());
	}

	public static Model buildBillboardModel(Texture texture, float width, float height) {
		TextureRegion textureRegion = new TextureRegion(texture, texture.getWidth(), texture.getHeight());
		Material material = new Material();
		material.set(new TextureAttribute(TextureAttribute.Diffuse, textureRegion));
		material.set(new ColorAttribute(ColorAttribute.AmbientLight, Color.WHITE));
		material.set(new BlendingAttribute());
		return ModelFactory.buildPlaneModel(width, height, material, 0, 0, 1, 1);
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
