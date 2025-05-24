package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

public class VoxelPhysicsBody {
    public byte voxelType;
    public int color;
    public Vector3 position;
    public btRigidBody body;
    private boolean isDynamic;
    private boolean isActive = false;

    public VoxelPhysicsBody(Vector3 position, byte voxelType, int color, VoxelWorld world) {
        this.position = position;
        this.voxelType = voxelType;
        this.color = color;
        this.isDynamic = false;
        makeStatic();

        btCollisionShape shape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f));
        btMotionState motionState = new btDefaultMotionState();
        btRigidBody.btRigidBodyConstructionInfo constructionInfo =
            new btRigidBody.btRigidBodyConstructionInfo(1.0f, motionState, shape);

        this.body = new btRigidBody(constructionInfo);
        Matrix4 transform = new Matrix4();
        transform.setToTranslation(position);
        this.body.setWorldTransform(transform);
        this.body.setActivationState(Collision.DISABLE_DEACTIVATION);

        world.addPhysicsBody(this.body);
    }

    public void makeDynamic() {
        if (!isDynamic) {
            body.setMassProps(0.5f, new Vector3()); // Устанавливаем массу
            body.setActivationState(Collision.DISABLE_DEACTIVATION);
            isDynamic = true;
        }
    }

    public void makeActive() {
        if (!isActive) {
            // Делаем тело динамическим только при активации (попадании)
            body.setMassProps(0.5f, new Vector3());
            body.setActivationState(Collision.DISABLE_DEACTIVATION);
            isActive = true;
        }
    }

    public void makeStatic() {
        if (isActive) {
            body.setMassProps(0f, new Vector3());
            isActive = false;
        }
    }
}

