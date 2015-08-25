package com.mygdx.game.systems;

/**
 * Created by user on 8/25/15.
 */
public class ModelSelectionSystem {
	//	private Ray ray = new Ray();
//	private final Vector3 tmp = new Vector3();
//	@Override
//	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//		for (Entity entity : entities) {
//			IntentComponent inputCmp = inputCmps.get(entity);
//			inputCmp.mouse.put(pointer, new MouseClickData(screenX, screenY, button));
//			System.moveDirection.println("clicked " + button);
//		}
//		ray.set(viewport.getPickRay(screenX, screenY));
//		float distance = 100;
//		Gdx.app.debug(tag, String.format(
//				"Mouse: %s, %s, Pick ray: origin: %s, direction: %s.",
//				screenX, screenY, ray.origin, ray.direction));
//
//		Entity hitEntity = phySys.rayTest(ray, tmp,
//				(short) (PhysicsSystem.GROUND_FLAG
//						| PhysicsSystem.OBJECT_FLAG),
//				distance);
//
//		if (hitEntity != null) {
//			Gdx.app.debug(tag, "Hit entity: " + hitEntity);
//
//			// Deselect previous
//			for (Entity selectable : entities) {
//				SelectableComponent selCmp = selectable.getComponent(SelectableComponent.class);
//				if (selCmp != null) {
//					selCmp.isSelected = false;
//				}
//			}
//
//			SelectableComponent selCmp = hitEntity.getComponent(SelectableComponent.class);
//			if (selCmp != null) {
//				selCmp.isSelected = true;
//				selCmp.hasCameraFocus = false;
//			}
//		}
//		return true;
//	}

}
