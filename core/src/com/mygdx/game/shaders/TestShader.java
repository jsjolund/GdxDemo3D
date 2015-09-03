package com.mygdx.game.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by user on 9/3/15.
 */
public class TestShader extends DefaultShader {

	public static final String tag = "TestShader";

	public TestShader(Renderable renderable, Config config) {
		super(renderable, config);
		String prefix = DefaultShader.createPrefix(renderable, config);
		program = new ShaderProgram(prefix + config.vertexShader, prefix + config.fragmentShader);
		if (!program.isCompiled()) {
			Gdx.app.debug(tag, program.getLog());
		}
	}


	@Override
	public void begin(final Camera camera, final RenderContext context) {
//		context.setDepthTest(GL20.GL_LEQUAL);
//		context.setCullFace(GL20.GL_FRONT);

		super.begin(camera, context);
	}

	@Override
	public void end() {
		super.end();
	}

	@Override
	public void render(final Renderable renderable, final Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);
	}

}
