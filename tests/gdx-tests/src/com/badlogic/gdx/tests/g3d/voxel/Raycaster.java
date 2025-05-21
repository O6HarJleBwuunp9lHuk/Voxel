package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class Raycaster {
    public static Ray getPickRay(PerspectiveCamera camera, int screenX, int screenY) {
        Vector3 origin = camera.position.cpy();
        Vector3 direction = camera.unproject(new Vector3(screenX, screenY, 0))
            .sub(camera.position).nor();
        return new Ray(origin, direction);
    }
}
