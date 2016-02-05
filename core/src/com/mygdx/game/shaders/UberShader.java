/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.mygdx.game.settings.ShaderSettings;
import com.mygdx.game.utilities.VertexColorTextureBlend;

/**
 * @author jsjolund
 */
public class UberShader extends DefaultShader {

	private static final String TAG = "UberShader";

	protected final int u_hue = register(new Uniform("u_hue"));
	protected final int u_saturation = register(new Uniform("u_saturation"));
	protected final int u_value = register(new Uniform("u_value"));
	protected final int u_specOpacity = register(new Uniform("u_specOpacity"));
	protected final int u_lightIntensity = register(new Uniform("u_lightIntensity"));
	protected final int u_ambient = register(new Uniform("u_ambient"));

	protected final int u_vcoltex_red = register(new Uniform(VertexColorTextureBlend.RedAlias));
	protected final int u_vcoltex_green = register(new Uniform(VertexColorTextureBlend.GreenAlias));
	protected final int u_vcoltex_blue = register(new Uniform(VertexColorTextureBlend.BlueAlias));


	public UberShader(Renderable renderable, Config config) {
		super(renderable, config);

		String prefix = DefaultShader.createPrefix(renderable, config);

		if (renderable.material.has(VertexColorTextureBlend.Red)) {
			prefix += "#define redVertexColorBlendFlag\n";
		}
		if (renderable.material.has(VertexColorTextureBlend.Green)) {
			prefix += "#define greenVertexColorBlendFlag\n";
		}
		if (renderable.material.has(VertexColorTextureBlend.Blue)) {
			prefix += "#define blueVertexColorBlendFlag\n";
		}

		program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
		if (!program.isCompiled()) {
			Gdx.app.debug(TAG, program.getLog());
		}
	}

	@Override
	public void render(final Renderable renderable, final Attributes combinedAttributes) {
		// General shader settings
		set(u_hue, ShaderSettings.hue);
		set(u_saturation, ShaderSettings.saturation);
		set(u_value, ShaderSettings.value);
		set(u_specOpacity, ShaderSettings.specOpacity);
		set(u_lightIntensity, ShaderSettings.lightIntensity);
		set(u_ambient, ShaderSettings.ambient);

		// Texture blending by vertex color
		if (renderable.material.has(VertexColorTextureBlend.Red)) {
			TextureDescriptor<Texture> td = ((TextureAttribute)
					renderable.material.get(VertexColorTextureBlend.Red)).textureDescription;
			set(u_vcoltex_red, context.textureBinder.bind(td));
		}
		if (renderable.material.has(VertexColorTextureBlend.Green)) {
			TextureDescriptor<Texture> td = ((TextureAttribute)
					renderable.material.get(VertexColorTextureBlend.Green)).textureDescription;
			set(u_vcoltex_green, context.textureBinder.bind(td));
		}
		if (renderable.material.has(VertexColorTextureBlend.Blue)) {
			TextureDescriptor<Texture> td = ((TextureAttribute)
					renderable.material.get(VertexColorTextureBlend.Blue)).textureDescription;
			set(u_vcoltex_blue, context.textureBinder.bind(td));
		}

		super.render(renderable, combinedAttributes);
	}

	@Override
	public void begin(final Camera camera, final RenderContext context) {
		super.begin(camera, context);
		context.setDepthTest(GL20.GL_LEQUAL);
		program.begin();
	}

	@Override
	public void end() {
		program.end();
		super.end();
		Gdx.gl20.glActiveTexture(Gdx.gl.GL_TEXTURE0);
	}

}


