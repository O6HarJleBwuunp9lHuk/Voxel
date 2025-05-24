package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import static com.badlogic.gdx.tests.g3d.voxel.VoxelWorld.*;

public class VoxelContactListener extends ContactListener {
    private final VoxelWorld world;

    public VoxelContactListener(VoxelWorld world) {
        this.world = world;
    }

    @Override
    public void onContactStarted(int userValue0, int userValue1) {
        btCollisionObject obj0 = findBody(userValue0);
        btCollisionObject obj1 = findBody(userValue1);
        System.out.println("Contact between: " + userValue0 + " and " + userValue1);

        if (obj0 == null || obj1 == null) return;

        // Определяем, кто снаряд, а кто воксель
        btCollisionObject projectile = null;
        btCollisionObject voxel = null;

        if (obj0.getUserValue() == PROJECTILE_MARKER && obj1.getUserValue() == VOXEL_MARKER) {
            projectile = obj0;
            voxel = obj1;
            System.out.println("123");
        }
        else if (obj1.getUserValue() == PROJECTILE_MARKER && obj0.getUserValue() == VOXEL_MARKER) {
            projectile = obj1;
            voxel = obj0;
            System.out.println("345");
        }

        if (projectile != null && voxel != null) {
            // Получаем позицию вокселя в мировых координатах
            Matrix4 transform = new Matrix4();
            voxel.getWorldTransform(transform);
            Vector3 hitPos = transform.getTranslation(new Vector3());
            System.out.println(hitPos.x + " vbuvuv");

            // Преобразуем в координаты вокселя
            int x = (int)(hitPos.x / WORLD_SCALE);
            int y = (int)(hitPos.y / WORLD_SCALE);
            int z = (int)(hitPos.z / WORLD_SCALE);

            // Удаляем воксель
            world.breakVoxel(x, y, z, hitPos, 10f);

            // Удаляем снаряд
            // world.removeBody((btRigidBody)projectile);
        }
    }

    private btRigidBody findBody(int userValue) {
        for (btRigidBody body : world.physicsBodies) {
            if (body.getUserValue() == userValue) {
                return body;
            }
        }
        return null;
    }

}
