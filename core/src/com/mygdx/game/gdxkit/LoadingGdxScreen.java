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

package com.mygdx.game.gdxkit;

/** @author davebaol */
public abstract class LoadingGdxScreen<G extends GdxGame> extends GdxScreen<G> {

	private final LoadableGdxScreen<G> nextScreen;

	public LoadingGdxScreen (G game, LoadableGdxScreen<G> nextScreen) {
		super(game);
		this.nextScreen = nextScreen;
	}

	public abstract void renderProgress (float delta, float progress);

	@Override
	public final void render (float delta) {
		if (getGame().getAssetManager().update()) {
			nextScreen.loadingFinished();
			getGame().setScreen(nextScreen);
		}

		renderProgress(delta, getGame().getAssetManager().getProgress());
	}

}
