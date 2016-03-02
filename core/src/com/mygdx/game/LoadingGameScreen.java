/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygdx.game.gdxkit.LoadableGdxScreen;
import com.mygdx.game.gdxkit.LoadingGdxScreen;

/** @author davebaol */
public class LoadingGameScreen extends LoadingGdxScreen<GdxDemo3D> {

	private static final float PROGRESS_BAR_WIDTH = GdxDemo3D.WIDTH / 3f;
	private static final float PROGRESS_BAR_HEIGHT = GdxDemo3D.HEIGHT / 20f;
	
	private ShapeRenderer shapeRenderer;
	private Viewport viewport;
	private Camera camera;

	public LoadingGameScreen (GdxDemo3D game, LoadableGdxScreen<GdxDemo3D> nextScreen) {
		super(game, nextScreen);
	}

	@Override
	public void resize (int width, int height) {
		viewport.update(width, height);
	}

	@Override
	public void show () {
		camera = new OrthographicCamera();
		camera.position.set(GdxDemo3D.WIDTH * .5f, GdxDemo3D.HEIGHT * .5f, 0);
		camera.update();
		viewport = new FitViewport(GdxDemo3D.WIDTH, GdxDemo3D.HEIGHT, camera);
		shapeRenderer = new ShapeRenderer();
	}

	@Override
	public void renderProgress (float delta, float progress) {
		Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.setProjectionMatrix(camera.projection);
		shapeRenderer.setTransformMatrix(camera.view);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		float x = (GdxDemo3D.WIDTH - PROGRESS_BAR_WIDTH) / 2;
		float y = (GdxDemo3D.HEIGHT - PROGRESS_BAR_HEIGHT) / 2;
		float k = 4;
		shapeRenderer.setColor(Color.WHITE);
		shapeRenderer.rect(x - k, y - k, PROGRESS_BAR_WIDTH + k * 2, PROGRESS_BAR_HEIGHT + k * 2);
		shapeRenderer.setColor(Color.BLUE);
		shapeRenderer.rect(x, y, PROGRESS_BAR_WIDTH * progress, PROGRESS_BAR_HEIGHT);
		shapeRenderer.end();
	}

	@Override
	public void dispose () {
		shapeRenderer.dispose();
	}

}
