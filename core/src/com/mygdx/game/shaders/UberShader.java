package com.mygdx.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.mygdx.game.systems.RenderSystem;

/**
 * Created by user on 8/29/15.
 */
public class UberShader extends DefaultShader {

	public static final String tag = "UberShader";

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
		program.setUniformf("u_hue", UberShaderSettings.u_hue);
		program.setUniformf("u_saturation", UberShaderSettings.u_saturation);
		program.setUniformf("u_value", UberShaderSettings.u_value);
		program.setUniformf("u_specOpacity", UberShaderSettings.u_specOpacity);
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

	public static class UberShaderSettings {
		public static float u_hue = 1f;
		public static float u_saturation = 0.6f;
		public static float u_value = 1.5f;
		public static float u_specOpacity = 0.4f;
	}

}
