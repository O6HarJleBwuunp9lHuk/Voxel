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

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;

import static com.badlogic.gdx.tests.g3d.voxel.VoxelLoader.loadFromVoxFile;


public class VoxelTest extends InputAdapter implements ApplicationListener {
	//SpriteBatch spriteBatch;
	BitmapFont font;
	ModelBatch modelBatch;
	PerspectiveCamera camera;
	Environment lights;
	FirstPersonCameraController controller;
	VoxelWorld voxelWorld;


	public void create () {
		//spriteBatch = new SpriteBatch();
		font = new BitmapFont();
		modelBatch = new ModelBatch();
		DefaultShader.defaultCullFace = GL20.GL_FRONT;
		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 0.5f;
		camera.far = 1000;
		controller = new FirstPersonCameraController(camera);
		Gdx.input.setInputProcessor(controller);

		MathUtils.random.setSeed(0);
        try {
            voxelWorld = loadFromVoxFile("C:\\final_all\\new\\final\\libgdx-crykn-patch-1\\tests\\gdx-tests\\src\\com\\badlogic\\gdx\\tests\\g3d\\voxel\\droid_one.vox", 10, 10, 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

		float camX = voxelWorld.voxelsX / 4f;
		float camZ = voxelWorld.voxelsZ / 4f;
		float camY = voxelWorld.getHighest(camX, camZ);
		camera.position.set(8f, 5f, 15f);
		camera.lookAt(80,32,80);



	}


	public void render () {
		ScreenUtils.clear(0.4f, 0.4f, 0.4f, 1f, true);
		modelBatch.begin(camera);
		modelBatch.render(voxelWorld, lights);
		modelBatch.end();
		controller.update();

		//spriteBatch.begin();
		//font.draw(spriteBatch, "fps: " + Gdx.graphics.getFramesPerSecond(), 0, 20);
		//spriteBatch.end();
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
