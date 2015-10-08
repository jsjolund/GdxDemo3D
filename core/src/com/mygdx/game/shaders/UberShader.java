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
 * Created by user on 8/29/15.
 */
public class UberShader extends DefaultShader {

	public static final String tag = "UberShader";

	public Renderable renderable;

	protected final int u_hue = register(new Uniform("u_hue"));
	protected final int u_saturation = register(new Uniform("u_saturation"));
	protected final int u_value = register(new Uniform("u_value"));
	protected final int u_specOpacity = register(new Uniform("u_specOpacity"));
	protected final int u_lightIntensity = register(new Uniform("u_lightIntensity"));
	protected final int u_ambient = register(new Uniform("u_ambient"));


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
