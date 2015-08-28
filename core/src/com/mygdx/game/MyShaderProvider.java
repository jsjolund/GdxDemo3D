package com.mygdx.game;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by user on 8/28/15.
 */
public class MyShaderProvider extends DefaultShaderProvider {

	ShaderProgram program;

	public MyShaderProvider(final DefaultShader.Config config) {
		super(config);
		ShaderProgram.pedantic = false;
	}

	@Override
	protected Shader createShader(final Renderable renderable) {
		if (renderable.material.has(TextureAttribute.Normal)) {
			if (program == null) {
				String prefix = DefaultShader.createPrefix(renderable, config);
				program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
			}
			return new DefaultShader(renderable, config, program);
		} else {
			return super.createShader(renderable);
		}

	}

	@Override
	public void dispose() {
		program.dispose();
	}
}
