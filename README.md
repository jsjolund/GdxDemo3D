# GdxDemo3D
GdxDemo3D is a game project built with [libGDX](https://github.com/libgdx), demonstrating various aspects of the game library, as well as its extensions, such as the physics engine [Bullet](http://bulletphysics.org/) wrapper and the artificial intelligence library [gdx-ai](https://github.com/libgdx/gdx-ai).

![alt tag](http://i.imgur.com/PWFlcWhl.png)  
A simple 3D world is included, with a multi storey house and various other items.

![alt tag](http://i.imgur.com/phUkbRGl.png)  
Animated human and dog characters are used as actors in the world. Ragdoll physics allows for dynamic character animation when a human falls down.

![alt tag](http://i.imgur.com/Vqr1s9wl.png)  
The [gdx-ai pathfinding](https://github.com/libgdx/gdx-ai/wiki/Pathfinding) module in conjunction with a navigation mesh handles pathfinding, the [gdx-ai steering behaviors](https://github.com/libgdx/gdx-ai/wiki/Steering-Behaviors) module handles character movement while [state machines](https://github.com/libgdx/gdx-ai/wiki/State-Machine) and [behavior trees](https://github.com/libgdx/gdx-ai/wiki/Behavior-Trees) are used to model human and dog brains respectively.

![alt tag](http://i.imgur.com/P0e1FHVl.png)  
The Bullet physics library is used to handle collisions and forces between objects.

![alt tag](http://i.imgur.com/Sq903YGl.png)  
Objects in the world can be modified directly in Blender and exported into the game with an automated script.
