package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;

public class CollisionPair {
    public btCollisionObject obj1;
    public btCollisionObject obj2;

    public CollisionPair(btCollisionObject obj1, btCollisionObject obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }
}
