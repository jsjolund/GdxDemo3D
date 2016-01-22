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

package com.mygdx.game.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * @author jsjolund
 */
public class GameSceneManager implements Disposable {

	public static final String tag = "GameSceneManager";

	private final ObjectMap<String, GameScene> sceneMap = new ObjectMap<String, GameScene>();
	private final ObjectMap<String, GameObjectBlueprint> sharedBlueprints = new ObjectMap<String, GameObjectBlueprint>();

	private final String modelPath;
	private final String modelExt;

	public GameSceneManager(String modelPath, String modelExt) {
		this.modelPath = modelPath;
		this.modelExt = modelExt;
	}

	public GameScene get(String sceneId) {
		if (!sceneMap.containsKey(sceneId)) {
			sceneMap.put(sceneId, new GameScene(modelPath, modelExt, sharedBlueprints));
			Gdx.app.debug(tag, "Added scene '" + sceneId + "'");
		}
		return sceneMap.get(sceneId);
	}

	public void addSharedBlueprint(String blueprintId, GameObjectBlueprint blueprint) {
		if (sharedBlueprints.containsKey(blueprintId)) {
			throw new GdxRuntimeException("Shared blueprint already exists '" + blueprintId + "'");
		}
		Gdx.app.debug(tag, "Added shared blueprint '" + blueprintId + "'");
		sharedBlueprints.put(blueprintId, blueprint);
	}

	public void dispose(String sceneId) {
		Gdx.app.debug(tag, "Disposing scene '" + sceneId + "'");
		sceneMap.get(sceneId).dispose();
	}

	@Override
	public void dispose() {
		for (GameScene scene : sceneMap.values()) {
			scene.dispose();
		}
		for (GameObjectBlueprint bp : sharedBlueprints.values()) {
			bp.dispose();
		}
	}

}
