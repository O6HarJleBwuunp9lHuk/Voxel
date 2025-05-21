package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ContactListener;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import static com.badlogic.gdx.tests.g3d.voxel.VoxelWorld.PROJECTILE_MARKER;
import static com.badlogic.gdx.tests.g3d.voxel.VoxelWorld.VOXEL_MARKER;

public class VoxelContactListener extends ContactListener {
    private final VoxelWorld world;

    public VoxelContactListener(VoxelWorld world) {
        this.world = world;
    }

    @Override
    public void onContactStarted(int userValue0, int userValue1) {
        // Получаем тела
        btRigidBody body0 = findBody(userValue0);
        btRigidBody body1 = findBody(userValue1);

        if (body0 == null || body1 == null) return;

        // Проверяем, что одно тело - снаряд, другое - воксель
        if ((body0.getUserValue() == PROJECTILE_MARKER && body1.getUserValue() == VOXEL_MARKER) ||
            (body1.getUserValue() == PROJECTILE_MARKER && body0.getUserValue() == VOXEL_MARKER)) {

            btRigidBody projectile = (body0.getUserValue() == PROJECTILE_MARKER) ? body0 : body1;
            btRigidBody voxel = (projectile == body0) ? body1 : body0;

            // Получаем позицию вокселя
            Matrix4 transform = new Matrix4();
            voxel.getWorldTransform(transform);
            Vector3 pos = transform.getTranslation(new Vector3());
            int x = (int)pos.x;
            int y = (int)pos.y;
            int z = (int)pos.z;

            // Разрушаем воксель
            world.breakVoxel(x, y, z, pos, 10f);

            // Удаляем снаряд
            // world.removeBody(projectile);
        }
    }

    private btRigidBody findBody(int userValue) {
        for (btRigidBody body : world.physicsBodies) {
            if (body.getUserPointer() == userValue) {
                return body;
            }
        }
        return null;
    }
}
