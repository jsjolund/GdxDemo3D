package com.mygdx.game.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Created by user on 8/3/15.
 */
public class BillboardTextureComponent extends Component {

	public TextureRegion textureRegion;
	public static final int TEX_WIDTH = 1024;
	public static final int TEX_HEIGHT = 1024;

	public BillboardTextureComponent(Pixmap pixmap) {
		textureRegion = new TextureRegion(new Texture(pixmap), pixmap.getWidth(), pixmap.getHeight());
	}

	public BillboardTextureComponent() {
		textureRegion = new TextureRegion(new Texture(TEX_WIDTH, TEX_HEIGHT,
				Pixmap.Format.RGBA8888));
	}


	public BillboardTextureComponent(String msg, Color textColor, Color bkgColor,
									 BitmapFont font) {
		SpriteBatch spriteBatch = new SpriteBatch();
		FrameBuffer fbo = null;
		// Draw string on texture
		try {
			fbo = new FrameBuffer(Pixmap.Format.RGBA8888, TEX_WIDTH, TEX_HEIGHT, false);
		} catch (Exception e) {
			System.out.println("Failed to create framebuffer.");
			e.printStackTrace();
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(bkgColor.r, bkgColor.g, bkgColor.b,
				bkgColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, TEX_WIDTH,
				TEX_HEIGHT);
		spriteBatch.begin();
		font.setColor(textColor);
		font.draw(spriteBatch, msg, 10, TEX_HEIGHT);
		textureRegion = new TextureRegion(fbo.getColorBufferTexture(), 0, 0,
				TEX_WIDTH, TEX_HEIGHT);
		textureRegion.flip(false, true);
		spriteBatch.end();
		fbo.end();
	}
}
