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

package com.mygdx.game.objects;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;

/**
 * @author jsjolund
 */
public abstract class GameCharacter extends SteerableBody {

	public final Vector3 selectionMarkerOffset;

	public GameCharacter(Model model, String name,
						 Vector3 location, Vector3 rotation, Vector3 scale,
						 btCollisionShape shape, float mass,
						 short belongsToFlag, short collidesWithFlag,
						 boolean callback, boolean noDeactivate,
						 SteerSettings steerSettings) {

		super(model, name, location, rotation, scale, shape, mass, belongsToFlag, collidesWithFlag, callback,
				noDeactivate, steerSettings);
		selectionMarkerOffset = new Vector3(0, -halfExtents.y * 0.95f, 0);
	}
}
