package com.mygdx.game;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Created by user on 8/24/15.
 */
public class GameStage extends Stage {

	private final Viewport viewport;
	Camera cameraUI;
	Camera camera3D;
	private final Batch batch;
	public TextButton btn;

	public GameStage(Viewport viewport) {
		super(viewport);

		this.viewport = viewport;

		camera3D = viewport.getCamera();
		cameraUI = new OrthographicCamera(viewport.getScreenWidth(), viewport.getScreenHeight());
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();

		batch = new SpriteBatch();

		Skin skin = new Skin(Gdx.files.internal("skin/uiskin.json"));

		btn = new TextButton("test", skin);
		btn.setPosition(0, 0);
		btn.setSize(100, 50);
		addActor(btn);

	}

	public void resize(int width, int height) {
		getViewport().update(width, height, false);
		cameraUI.viewportWidth = viewport.getScreenWidth();
		cameraUI.viewportHeight = viewport.getScreenHeight();
		cameraUI.position.set(viewport.getScreenWidth() / 2, viewport.getScreenHeight() / 2, 0);
		cameraUI.update();
		batch.setProjectionMatrix(cameraUI.combined);
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		viewport.setCamera(cameraUI);
		boolean result = super.touchDown(screenX, screenY, pointer, button);
		viewport.setCamera(camera3D);
		return result;
	}

	@Override
	public void draw() {
		batch.begin();
		btn.draw(batch, 1);
		batch.end();
	}
}
