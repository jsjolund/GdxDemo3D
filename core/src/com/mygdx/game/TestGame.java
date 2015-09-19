package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.HashSet;
import java.util.Set;


public class TestGame extends Game {

	public PerspectiveCamera cam;
	public CameraInputController camController;
	public Environment environment;
	ShapeRenderer shapeRenderer;

	@Override
	public void create() {

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(2f, 2f, 2f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);


		shapeRenderer = new ShapeRenderer();
	}

	@Override
	public void render() {
		camController.update();

		float c = 0.5f;
		Gdx.gl.glClearColor(c, c, c, 1f);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

//		shapeRenderer.setProjectionMatrix(cam.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.CYAN);
		shapeRenderer.circle(100, 300, 50);
		shapeRenderer.circle(1100, 300, 50);
		shapeRenderer.end();

		Pixmap map = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Set colors = new HashSet<Color>();
        for(int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Color referencePixelColor = new Color(map.getPixel(x, y));
//				System.out.println(referencePixelColor);
				colors.add(referencePixelColor);
				if (referencePixelColor.toIntBits() == Color.CYAN.toIntBits()) {
                    Gdx.app.log("alert", "found yellow");
                }
            }
        }
		System.out.println(colors);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
