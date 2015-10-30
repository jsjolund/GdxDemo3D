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

package com.mygdx.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.mygdx.game.settings.ShaderSettings;

/**
 * @author jsjolund
 */
public class UberShader extends DefaultShader {

	public static final String tag = "UberShader";
	protected final int u_hue = register(new Uniform("u_hue"));
	protected final int u_saturation = register(new Uniform("u_saturation"));
	protected final int u_value = register(new Uniform("u_value"));
	protected final int u_specOpacity = register(new Uniform("u_specOpacity"));
	protected final int u_lightIntensity = register(new Uniform("u_lightIntensity"));
	protected final int u_ambient = register(new Uniform("u_ambient"));
	public Renderable renderable;


	public UberShader(Renderable renderable, Config config) {
		super(renderable, config);
		this.renderable = renderable;
		String prefix = DefaultShader.createPrefix(renderable, config);
		program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
		if (!program.isCompiled()) {
			Gdx.app.debug(tag, program.getLog());
		}
	}

	@Override
	public void begin(final Camera camera, final RenderContext context) {
		super.begin(camera, context);
		context.setDepthTest(GL20.GL_LEQUAL);
		program.begin();
		set(u_hue, ShaderSettings.hue);
		set(u_saturation, ShaderSettings.saturation);
		set(u_value, ShaderSettings.value);
		set(u_specOpacity, ShaderSettings.specOpacity);
		set(u_lightIntensity, ShaderSettings.lightIntensity);
		set(u_ambient, ShaderSettings.ambient);
	}

	@Override
	public void end() {
		program.end();
		super.end();
	}

	@Override
	public void render(final Renderable renderable, final Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);
	}

}
