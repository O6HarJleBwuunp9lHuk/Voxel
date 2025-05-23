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

package com.badlogic.gdx.assets;

import com.badlogic.gdx.assets.loaders.AssetLoader;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.AsyncResult;
import com.badlogic.gdx.utils.async.AsyncTask;

class AssetLoadingTask implements AsyncTask<Void> {
	AssetManager manager;
	final AssetDescriptor assetDesc;
	final AssetLoader loader;
	final AsyncExecutor executor;
	final long startTime;

	volatile boolean asyncDone;
	volatile boolean dependenciesLoaded;
	volatile Array<AssetDescriptor> dependencies;
	volatile AsyncResult<Void> depsFuture;
	volatile AsyncResult<Void> loadFuture;
	volatile Object asset;

	volatile boolean cancel;

	public AssetLoadingTask (AssetManager manager, AssetDescriptor assetDesc, AssetLoader loader, AsyncExecutor threadPool) {
		this.manager = manager;
		this.assetDesc = assetDesc;
		this.loader = loader;
		this.executor = threadPool;
		startTime = manager.log.getLevel() == Logger.DEBUG ? TimeUtils.nanoTime() : 0;
	}

	@Override
	public Void call () throws Exception {
		if (cancel) return null;
		AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader)loader;
		if (!dependenciesLoaded) {
			dependencies = asyncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			if (dependencies != null) {
				removeDuplicates(dependencies);
				manager.injectDependencies(assetDesc.fileName, dependencies);
			} else {
				// if we have no dependencies, we load the async part of the task immediately.
				asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
				asyncDone = true;
			}
		} else {
			asyncLoader.loadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			asyncDone = true;
		}
		return null;
	}

	public boolean update () {
		if (loader instanceof SynchronousAssetLoader)
			handleSyncLoader();
		else
			handleAsyncLoader();
		return asset != null;
	}

	private void handleSyncLoader () {
		SynchronousAssetLoader syncLoader = (SynchronousAssetLoader)loader;
		if (!dependenciesLoaded) {
			dependenciesLoaded = true;
			dependencies = syncLoader.getDependencies(assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			if (dependencies == null) {
				asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
				return;
			}
			removeDuplicates(dependencies);
			manager.injectDependencies(assetDesc.fileName, dependencies);
		} else
			asset = syncLoader.load(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
	}

	private void handleAsyncLoader () {
		AsynchronousAssetLoader asyncLoader = (AsynchronousAssetLoader)loader;
		if (!dependenciesLoaded) {
			if (depsFuture == null)
				depsFuture = executor.submit(this);
			else if (depsFuture.isDone()) {
				try {
					depsFuture.get();
				} catch (Exception e) {
					throw new GdxRuntimeException("Couldn't load dependencies of asset: " + assetDesc.fileName, e);
				}
				dependenciesLoaded = true;
				if (asyncDone)
					asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
			}
		} else if (loadFuture == null && !asyncDone)
			loadFuture = executor.submit(this);
		else if (asyncDone)
			asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
		else if (loadFuture.isDone()) {
			try {
				loadFuture.get();
			} catch (Exception e) {
				throw new GdxRuntimeException("Couldn't load asset: " + assetDesc.fileName, e);
			}
			asset = asyncLoader.loadSync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
		}
	}

	public void unload () {
		if (loader instanceof AsynchronousAssetLoader)
			((AsynchronousAssetLoader)loader).unloadAsync(manager, assetDesc.fileName, resolve(loader, assetDesc), assetDesc.params);
	}

	private FileHandle resolve (AssetLoader loader, AssetDescriptor assetDesc) {
		if (assetDesc.file == null) assetDesc.file = loader.resolve(assetDesc.fileName);
		return assetDesc.file;
	}

	private void removeDuplicates (Array<AssetDescriptor> array) {
		boolean ordered = array.ordered;
		array.ordered = true;
		for (int i = 0; i < array.size; ++i) {
			final String fn = array.get(i).fileName;
			final Class type = array.get(i).type;
			for (int j = array.size - 1; j > i; --j)
				if (type == array.get(j).type && fn.equals(array.get(j).fileName)) array.removeIndex(j);
		}
		array.ordered = ordered;
	}
}
