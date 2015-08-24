package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Created by user on 8/21/15.
 */
public class WidgetScreen implements Screen {
	@Override
	public void show() {

	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, false);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {

	}

	private Viewport viewport;
	private Camera camera;
	private Stage stage;

	public WidgetScreen(int reqWidth, int reqHeight) {
//		super(reqWidth, reqHeight);
		camera = new OrthographicCamera(reqWidth, reqHeight);
		camera.position.set(reqWidth / 2, reqHeight / 2, 0);
		camera.update();
//		camera = new PerspectiveCamera();
		viewport = new FitViewport(reqWidth, reqHeight, camera);
		stage = new Stage(viewport);
		Gdx.input.setInputProcessor(stage);

		Skin skin = new Skin(Gdx.files.internal("skin/uiskin.json"));
		TextButton btn = new TextButton("test", skin);
		btn.setPosition(100, 100);
		btn.setSize(300, 100);

		stage.addActor(btn);
	}

	@Override
	public void render(float delta) {
		Gdx.graphics.getGL20().glClearColor(0, 0, 0, 0.5f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
//		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//		shapeRenderer.setColor(viewportBackgroundColor);
//		shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
//		shapeRenderer.end();


		stage.act(delta);
		stage.draw();

	}

	@Override
	public void dispose() {
		stage.dispose();
	}
}