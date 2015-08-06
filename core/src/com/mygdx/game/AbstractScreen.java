package com.mygdx.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.nio.ByteBuffer;

/**
 * Created by user on 8/1/15.
 */
public class AbstractScreen implements Screen {

	public static final String tag = "AbstractScreen";

	protected Viewport viewport;
	protected Camera camera;
	protected SpriteBatch spriteBatch;
	protected Matrix4 uiMatrix;
	protected Color viewportBorderColor = Color.BLACK;
	protected Color viewportBackgroundColor = Color.LIGHT_GRAY;
	private Vector3 screenCenter = new Vector3();
	private int reqHeight;
	private int reqWidth;
	private ShapeRenderer shapeRenderer;

	public AbstractScreen(int reqWidth, int reqHeight) {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		this.reqHeight = reqHeight;
		this.reqWidth = reqWidth;

		spriteBatch = new SpriteBatch();

		camera = new OrthographicCamera(reqWidth, reqHeight);
		camera.position.set(reqWidth / 2, reqHeight / 2, 0);
		camera.update();

		viewport = new FitViewport(reqWidth, reqHeight, camera);
		viewport.apply();

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, getViewportWidth(), getViewportHeight());

		shapeRenderer = new ShapeRenderer();
	}

	public static Pixmap getScreenshot(int x, int y, int w, int h, boolean yDown) {
		final Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(x, y, w, h);

		if (yDown) {
			// Flip the pixmap upside down
			ByteBuffer pixels = pixmap.getPixels();
			int numBytes = w * h * 4;
			byte[] lines = new byte[numBytes];
			int numBytesPerLine = w * 4;
			for (int i = 0; i < h; i++) {
				pixels.position((h - i - 1) * numBytesPerLine);
				pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
			}
			pixels.clear();
			pixels.put(lines);
			pixels.clear();
		}

		return pixmap;
	}

	@Override
	public void dispose() {
		spriteBatch.dispose();
	}

	public int getLeftGutterWidth() {
		return viewport.getLeftGutterWidth();
	}

	public int getBottomGutterWidth() {
		return viewport.getBottomGutterHeight();
	}

	public int getViewportHeight() {
		return viewport.getScreenHeight();
	}

	public int getViewportWidth() {
		return viewport.getScreenWidth();
	}

	@Override
	public void show() {

	}

	@Override
	public void render(float delta) {
		Gdx.graphics.getGL20().glClearColor(viewportBorderColor.r,
				viewportBorderColor.g, viewportBorderColor.b, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		shapeRenderer.setProjectionMatrix(uiMatrix);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(viewportBackgroundColor);
		shapeRenderer.rect(0, 0, getViewportWidth(), getViewportHeight());
		shapeRenderer.end();

	}

	@Override
	public void resize(int width, int height) {

		Vector2 size = Scaling.fit.apply(reqWidth, reqHeight, width, height);
		int viewportX = (int) (width - size.x) / 2;
		int viewportY = (int) (height - size.y) / 2;
		int viewportWidth = (int) size.x;
		int viewportHeight = (int) size.y;

		Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
		viewport.setWorldSize(width, height);
		viewport.setScreenSize(viewportWidth, viewportHeight);
		viewport.setScreenPosition(viewportX, viewportY);

		viewport.apply();

		screenCenter.set(width / 2, height / 2, 1);

		uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, viewportWidth, viewportHeight);

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

	public Vector3 getScreenCenter(Vector3 out) {
		out.set(screenCenter);
		return out;
	}

	public Vector3 screenPointToViewport(Vector3 screen) {
		screen.x = screenXtoViewportX(screen.x);
		screen.y = screenYtoViewportY(screen.y);
		return screen;
	}

	public float screenXtoViewportX(float screenX) {
		return screenX - viewport.getRightGutterWidth();
	}

	public float screenYtoViewportY(float screenY) {
		return viewport.getWorldHeight() - viewport.getBottomGutterHeight() - screenY;
	}
}
