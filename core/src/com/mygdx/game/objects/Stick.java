package com.mygdx.game.objects;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;

public class Stick extends GameModelBody {

	public HumanCharacter owner;
	public boolean hasLanded;

	/**
	 * Creates a model with rigid body
	 *
	 * @param model            Model to instantiate
	 * @param name             Name of model
	 * @param location         World position at which to place the model instance
	 * @param rotation         The rotation of the model instance in degrees
	 * @param scale            Scale of the model instance
	 * @param shape            Collision shape with which to construct a rigid body
	 * @param mass             Mass of the body
	 * @param belongsToFlag    Flag for which collision layers this body belongs to
	 * @param collidesWithFlag Flag for which collision layers this body collides with
	 * @param callback         If this body should trigger collision contact callbacks.
	 * @param noDeactivate     If this body should never 'sleep'
	 */
	public Stick(Model model, String name, Vector3 location, Vector3 rotation, Vector3 scale, btCollisionShape shape, float mass, short belongsToFlag, short collidesWithFlag, boolean callback, boolean noDeactivate) {
		super(model, name, location, rotation, scale, shape, mass, belongsToFlag, collidesWithFlag, callback, noDeactivate);
	}


}
