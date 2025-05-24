/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.tests.g3d.voxel;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObjectArray;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;

import static com.badlogic.gdx.tests.g3d.voxel.VoxelLoader.loadFromVoxFile;
import static com.badlogic.gdx.tests.g3d.voxel.VoxelWorld.*;


public class VoxelTest extends InputAdapter implements ApplicationListener {
	//SpriteBatch spriteBatch;
	BitmapFont font;
	ModelBatch modelBatch;
	PerspectiveCamera camera;
	Environment lights;
	FirstPersonCameraController controller;
	VoxelWorld voxelWorld;
    private ShapeRenderer shapeRenderer;
    private Array<VoxelPhysicsBody> activePhysicsBodies = new Array<>();

	public void create () {
		//spriteBatch = new SpriteBatch();
		font = new BitmapFont();
		modelBatch = new ModelBatch();
		DefaultShader.defaultCullFace = GL20.GL_FRONT;
		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 0.5f;
		camera.far = 1000;
		controller = new FirstPersonCameraController(camera);
        //Gdx.input.setInputProcessor(controller);
        Gdx.graphics.setVSync(true);
        shapeRenderer = new ShapeRenderer();

// Настраиваем точность буфера глубины
        Gdx.gl20.glClearDepthf(1.0f);
        Gdx.gl.glDepthMask(true);
		MathUtils.random.setSeed(0);
        try {
            voxelWorld = loadFromVoxFile("C:\\final_all\\new\\final\\libgdx-crykn-patch-1\\tests\\gdx-tests\\src\\com\\badlogic\\gdx\\tests\\g3d\\voxel\\droid_one.vox", 10, 10, 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

		float camX = voxelWorld.voxelsX / 4f;
		float camZ = voxelWorld.voxelsZ / 4f;
		float camY = voxelWorld.getHighest(camX, camZ);
		camera.position.set(1f, 1f, 1f);
		camera.lookAt(80,32,80);

        Gdx.input.setInputProcessor(new InputMultiplexer(controller, new InputAdapter() {
//            @Override
//            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//                if (button == Input.Buttons.LEFT) {
//                    // Получаем луч из камеры в точку клика
//                    Ray ray = Raycaster.getPickRay(camera, screenX, screenY);
//                    System.out.println("buubg");
//                    // Проверяем пересечение с вокселями (простой вариант)
//                    for (float t = 0; t < 20; t += 0.5f) {
//                        Vector3 point = new Vector3(ray.origin).mulAdd(ray.direction, t);
//                        int x = (int)point.x;
//                        int y = (int)point.y;
//                        int z = (int)point.z;
//
//                        if (voxelWorld.get(x, y, z) > 0) {
//                            voxelWorld.breakVoxel(x, y, z, point, 10f);
//                            return true;
//                        }
//                    }
//                }
//                return false;
//            }
@Override
public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    if (button == Input.Buttons.LEFT) {
        throwObject();
        voxelWorld.setMass(1f);
        System.out.println("vyfyfy");
        return true;
    }
    return false;
}
        }));

	}

    private void throwObject() {
        Vector3 direction = camera.direction.cpy().nor().scl(50); // Умеренная скорость
        Vector3 position = camera.position.cpy().add(camera.direction.cpy().nor().scl(2));

        // Увеличиваем радиус снаряда для лучшего обнаружения столкновений
        btCollisionShape shape = new btSphereShape(0.5f);
        btMotionState motionState = new btDefaultMotionState();

        // Увеличиваем массу для большего воздействия
        btRigidBody.btRigidBodyConstructionInfo constructionInfo =
            new btRigidBody.btRigidBodyConstructionInfo(3.0f, motionState, shape);

        btRigidBody projectile = new btRigidBody(constructionInfo);
        projectile.setWorldTransform(new Matrix4().setToTranslation(position));
        projectile.setLinearVelocity(direction);
        projectile.setRestitution(0.5f);
        projectile.setUserValue(PROJECTILE_MARKER);

        // Важно: включаем CCD (Continuous Collision Detection)
        projectile.setCcdMotionThreshold(0.1f);
        projectile.setCcdSweptSphereRadius(0.5f);

        // Правильные маски коллизий
        voxelWorld.addBody(projectile, PROJECTILE_GROUP,
            (short)(VOXEL_MASK));
    }

    float accumulator = 0;
    float step = 1/60f;

	public void render () {
        ScreenUtils.clear(0.4f, 0.4f, 0.4f, 1f, true);
        float delta = Gdx.graphics.getDeltaTime();

		modelBatch.begin(camera);
		modelBatch.render(voxelWorld, lights);
		modelBatch.end();
		controller.update();
        voxelWorld.update(Gdx.graphics.getDeltaTime());
        //ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        // Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glEnable(GL20.GL_POLYGON_OFFSET_FILL);
        Gdx.gl.glPolygonOffset(1f, 1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        voxelWorld.dynamicsWorld.stepSimulation(delta);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

       // Gdx.gl.glEnable(GL20.GL_BLEND);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        btCollisionObjectArray objects = voxelWorld.dynamicsWorld.getCollisionObjectArray();
        for (int i = 0; i < objects.size(); i++) {
            btCollisionObject obj = objects.atConst(i);

            Matrix4 transform = new Matrix4();
            obj.getWorldTransform(transform);
            Vector3 worldPos = transform.getTranslation(new Vector3());

            // Преобразуем в координаты вокселей (учитывая WORLD_SCALE)
            int voxelX = Math.round((worldPos.x / WORLD_SCALE) - 0.5f);
            int voxelY = Math.round((worldPos.y / WORLD_SCALE) - 0.5f);
            int voxelZ = Math.round((worldPos.z / WORLD_SCALE) - 0.5f);

            // Проверка границ
            if (voxelX < 0 || voxelY < 0 || voxelZ < 0 ||
                voxelX >= voxelWorld.voxelsX ||
                voxelY >= voxelWorld.voxelsY ||
                voxelZ >= voxelWorld.voxelsZ) {
                continue;
            }

            // Получаем цвет (формат: 0xRRGGBB)
            int colorValue = 0;
            if (obj.getUserValue() == VOXEL_MARKER) {
                VoxelData data = voxelWorld.decodeVoxelData(obj.getUserPointer());
                colorValue = data.color;
            } else {
                colorValue = obj.getUserValue();
            }




            // Преобразуем в объект Color
            Color color = new Color();
            color.r = ((colorValue >> 12) & 0xF) / 15f;
            color.g = ((colorValue >> 8) & 0xF) / 15f;
            color.b = ((colorValue >> 4)  & 0xF) / 15f;
            color.a = (colorValue & 0xF) / 15f; // Полная непрозрачность

            float offset = 0.001f * (obj.getUserPointer() % 100) / 100f;

            shapeRenderer.setColor(color);
            shapeRenderer.box(
                worldPos.x - 0.5f * WORLD_SCALE + offset,
                worldPos.y - 0.5f * WORLD_SCALE + offset,
                worldPos.z - 0.5f * WORLD_SCALE + offset,
                WORLD_SCALE - 0.002f, WORLD_SCALE - 0.002f, WORLD_SCALE - 0.002f
            );
        }

        Gdx.gl.glDisable(GL20.GL_POLYGON_OFFSET_FILL);
        shapeRenderer.end();
	}



	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}


	public void resize (int width, int height) {
        //spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
		camera.viewportWidth = width;
		camera.viewportHeight = height;
		camera.update();
	}
}
