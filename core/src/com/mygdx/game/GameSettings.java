package com.mygdx.game;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by user on 8/1/15.
 */
public class GameSettings {

	public static final int CROSSHAIR = 5;
	public static final float MOUSE_SENSITIVITY = 0.01f;
	public static final Vector3 GRAVITY = new Vector3(0, -9.81f * 2, 0);
	public static final float CAMERA_FOV = 60;

	public static final float PLAYER_WALK_SPEED = 8f;
	public static final float PLAYER_RUN_SPEED = 16f;
	public static final float PLAYER_JUMP_FORCE = 6000f;
	public static final float PLAYER_MAX_JUMP_PRESS_TIME = 0.2f;
	public static final float PLAYER_STOP_DOWNSCALE = 0.9f;

	public static final float PLAYER_GRAPPLE_TELEPORT_TIME = 0.25f;
	public static final float PLAYER_GRAPPLE_MISS_TIME = 0.5f;

	public static final float PLAYER_HEIGHT = 2f;
	public static final float PLAYER_RADIUS = 0.5f;
	public static final float PLAYER_EYE_HEIGHT = 1.8f;

	public static final Vector3 PLAYER_START_DIR = (new Vector3(1, 0, 1f))
			.nor();

	public static final int SHOOT = Input.Buttons.LEFT;
	public static final int HOOK = Input.Buttons.RIGHT;

	public static int FORWARD = Input.Keys.W;
	public static int STRAFE_LEFT = Input.Keys.A;
	public static int BACKWARD = Input.Keys.S;
	public static int STRAFE_RIGHT = Input.Keys.D;
	public static int JUMP = Input.Keys.SPACE;
	public static int RUN = Input.Keys.SHIFT_LEFT;
	public static int USE = Input.Keys.E;
	public static int MASK = Input.Keys.TAB;

	public static float SOUND_VOLUME = 1f;
	public static float MUSIC_VOLUME = 1f;
}
