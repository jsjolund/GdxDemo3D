package com.mygdx.game.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.game.MyGdxGame;

public class DesktopLauncher {
	public static void main(String[] arg) {

//		Json json = new Json();
//		BlenderModelComponent cmp = new BlenderModelComponent();
//		try {
//			cmp.javaClass = Class.forName(BlenderModelComponent.class.getName());
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
//		cmp.position = new Vector3(1,2,3);
//		cmp.modelFileName = "test";
//		String out = json.toJson(cmp);
//		System.out.println(out);
//
//		JsonValue v = new JsonValue(out);
//		System.out.println(v);
//		System.out.println(v.get("javaClass"));

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(new MyGdxGame(), config);
	}
}
