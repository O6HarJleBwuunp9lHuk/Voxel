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

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.*;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.async.ThreadUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class AssetManager implements Disposable {
	final ObjectMap<Class, ObjectMap<String, RefCountedContainer>> assets = new ObjectMap();
	final ObjectMap<String, Class> assetTypes = new ObjectMap();
	final ObjectMap<String, Array<String>> assetDependencies = new ObjectMap();
	final ObjectSet<String> injected = new ObjectSet();

	final ObjectMap<Class, ObjectMap<String, AssetLoader>> loaders = new ObjectMap();
	final Array<AssetDescriptor> loadQueue = new Array();
	final AsyncExecutor executor;

	final Array<AssetLoadingTask> tasks = new Array();
	AssetErrorListener listener;
	int loaded;
	int toLoad;
	int peakTasks;

	final FileHandleResolver resolver;

	Logger log = new Logger("AssetManager", Application.LOG_NONE);

	public AssetManager () {
		this(new InternalFileHandleResolver());
	}

	public AssetManager (FileHandleResolver resolver) {
		this(resolver, true);
	}

	public AssetManager (FileHandleResolver resolver, boolean defaultLoaders) {
		this.resolver = resolver;
		if (defaultLoaders) {
			setLoader(PolygonRegion.class, new PolygonRegionLoader(resolver));
			setLoader(I18NBundle.class, new I18NBundleLoader(resolver));
			setLoader(ShaderProgram.class, new ShaderProgramLoader(resolver));
		}
		executor = new AsyncExecutor(1, "AssetManager");
	}

	public synchronized <T> T get (String fileName) {
		return get(fileName, true);
	}

	public synchronized <T> T get (String fileName, Class<T> type) {
		return get(fileName, type, true);
	}

	public synchronized @Null <T> T get (String fileName, boolean required) {
		Class<T> type = assetTypes.get(fileName);
		if (type != null) {
			ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
			if (assetsByType != null) {
				RefCountedContainer assetContainer = assetsByType.get(fileName);
				if (assetContainer != null) return (T)assetContainer.object;
			}
		}
		if (required) throw new GdxRuntimeException("Asset not loaded: " + fileName);
		return null;
	}

	public synchronized @Null <T> T get (String fileName, Class<T> type, boolean required) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType != null) {
			RefCountedContainer assetContainer = assetsByType.get(fileName);
			if (assetContainer != null) return (T)assetContainer.object;
		}
		if (required) throw new GdxRuntimeException("Asset not loaded: " + fileName);
		return null;
	}

	public synchronized <T> T get (AssetDescriptor<T> assetDescriptor) {
		return get(assetDescriptor.fileName, assetDescriptor.type, true);
	}

	public synchronized <T> Array<T> getAll (Class<T> type, Array<T> out) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType != null) {
			for (RefCountedContainer assetRef : assetsByType.values())
				out.add((T)assetRef.object);
		}
		return out;
	}

	public synchronized boolean contains (String fileName) {
		if (tasks.size > 0 && tasks.first().assetDesc.fileName.equals(fileName)) return true;

		for (int i = 0; i < loadQueue.size; i++)
			if (loadQueue.get(i).fileName.equals(fileName)) return true;

		return isLoaded(fileName);
	}

	public synchronized boolean contains (String fileName, Class type) {
		if (tasks.size > 0) {
			AssetDescriptor assetDesc = tasks.first().assetDesc;
			if (assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
		}

		for (int i = 0; i < loadQueue.size; i++) {
			AssetDescriptor assetDesc = loadQueue.get(i);
			if (assetDesc.type == type && assetDesc.fileName.equals(fileName)) return true;
		}

		return isLoaded(fileName, type);
	}

	public synchronized void unload (String fileName) {
		if (tasks.size > 0) {
			AssetLoadingTask currentTask = tasks.first();
			if (currentTask.assetDesc.fileName.equals(fileName)) {
				log.info("Unload (from tasks): " + fileName);
				currentTask.cancel = true;
				currentTask.unload();
				return;
			}
		}

		Class type = assetTypes.get(fileName);

		int foundIndex = -1;
		for (int i = 0; i < loadQueue.size; i++) {
			if (loadQueue.get(i).fileName.equals(fileName)) {
				foundIndex = i;
				break;
			}
		}
		if (foundIndex != -1) {
			toLoad--;
			AssetDescriptor desc = loadQueue.removeIndex(foundIndex);
			log.info("Unload (from queue): " + fileName);
			if (type != null && desc.params != null && desc.params.loadedCallback != null)
				desc.params.loadedCallback.finishedLoading(this, desc.fileName, desc.type);
			return;
		}

		if (type == null) throw new GdxRuntimeException("Asset not loaded: " + fileName);

		RefCountedContainer assetRef = assets.get(type).get(fileName);
		assetRef.refCount--;
		if (assetRef.refCount <= 0) {
			log.info("Unload (dispose): " + fileName);
			if (assetRef.object instanceof Disposable) ((Disposable)assetRef.object).dispose();
			assetTypes.remove(fileName);
			assets.get(type).remove(fileName);
		} else
			log.info("Unload (decrement): " + fileName);

		Array<String> dependencies = assetDependencies.get(fileName);
		if (dependencies != null) {
			for (String dependency : dependencies)
				if (isLoaded(dependency)) unload(dependency);
		}
		if (assetRef.refCount <= 0) assetDependencies.remove(fileName);
	}


	public synchronized <T> String getAssetFileName (T asset) {
		for (Class assetType : assets.keys()) {
			ObjectMap<String, RefCountedContainer> assetsByType = assets.get(assetType);
			for (Entry<String, RefCountedContainer> entry : assetsByType) {
				Object object = entry.value.object;
				if (object == asset || asset.equals(object)) return entry.key;
			}
		}
		return null;
	}

	public synchronized boolean isLoaded (String fileName) {
		if (fileName == null) return false;
		return assetTypes.containsKey(fileName);
	}

	public synchronized boolean isLoaded (String fileName, Class type) {
		ObjectMap<String, RefCountedContainer> assetsByType = assets.get(type);
		if (assetsByType == null) return false;
		return assetsByType.get(fileName) != null;
	}

	public <T> AssetLoader getLoader (final Class<T> type, final String fileName) {
		ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
		if (loaders == null || loaders.size < 1) return null;
		if (fileName == null) return loaders.get("");
		AssetLoader result = null;
		int length = -1;
		for (Entry<String, AssetLoader> entry : loaders.entries()) {
			if (entry.key.length() > length && fileName.endsWith(entry.key)) {
				result = entry.value;
				length = entry.key.length();
			}
		}
		return result;
	}

	public synchronized <T> void load (String fileName, Class<T> type, AssetLoaderParameters<T> parameter) {
		AssetLoader loader = getLoader(type, fileName);
		if (loader == null) throw new GdxRuntimeException("No loader for type: " + ClassReflection.getSimpleName(type));

		// reset stats
		if (loadQueue.size == 0) {
			loaded = 0;
			toLoad = 0;
			peakTasks = 0;
		}

		for (int i = 0; i < loadQueue.size; i++) {
			AssetDescriptor desc = loadQueue.get(i);
			if (desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new GdxRuntimeException(
				"Asset with name '" + fileName + "' already in preload queue, but has different type (expected: "
					+ ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(desc.type) + ")");
		}

		for (int i = 0; i < tasks.size; i++) {
			AssetDescriptor desc = tasks.get(i).assetDesc;
			if (desc.fileName.equals(fileName) && !desc.type.equals(type)) throw new GdxRuntimeException(
				"Asset with name '" + fileName + "' already in task list, but has different type (expected: "
					+ ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(desc.type) + ")");
		}

		Class otherType = assetTypes.get(fileName);
		if (otherType != null && !otherType.equals(type))
			throw new GdxRuntimeException("Asset with name '" + fileName + "' already loaded, but has different type (expected: "
				+ ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(otherType) + ")");

		toLoad++;
		AssetDescriptor assetDesc = new AssetDescriptor(fileName, type, parameter);
		loadQueue.add(assetDesc);
		log.debug("Queued: " + assetDesc);
	}

	public synchronized void load (AssetDescriptor desc) {
		load(desc.fileName, desc.type, desc.params);
	}

	public synchronized boolean update () {
		try {
			if (tasks.size == 0) {
				while (loadQueue.size != 0 && tasks.size == 0)
					nextTask();
				if (tasks.size == 0) return true;
			}
			return updateTask() && loadQueue.size == 0 && tasks.size == 0;
		} catch (Throwable t) {
			handleTaskError(t);
			return loadQueue.size == 0;
		}
	}

	public boolean update (int millis) {
		if (Gdx.app.getType() == Application.ApplicationType.WebGL) return update();
		long endTime = TimeUtils.millis() + millis;
		while (true) {
			boolean done = update();
			if (done || TimeUtils.millis() > endTime) return done;
			ThreadUtils.yield();
		}
	}

	public void finishLoading () {
		log.debug("Waiting for loading to complete...");
		while (!update())
			ThreadUtils.yield();
		log.debug("Loading complete.");
	}

	synchronized void injectDependencies (String parentAssetFilename, Array<AssetDescriptor> dependendAssetDescs) {
		ObjectSet<String> injected = this.injected;
		for (AssetDescriptor desc : dependendAssetDescs) {
			if (injected.contains(desc.fileName)) continue; // Ignore subsequent dependencies if there are duplicates.
			injected.add(desc.fileName);
			injectDependency(parentAssetFilename, desc);
		}
		injected.clear(32);
	}

	private synchronized void injectDependency (String parentAssetFilename, AssetDescriptor dependendAssetDesc) {
		Array<String> dependencies = assetDependencies.get(parentAssetFilename);
		if (dependencies == null) {
			dependencies = new Array();
			assetDependencies.put(parentAssetFilename, dependencies);
		}
		dependencies.add(dependendAssetDesc.fileName);

		if (isLoaded(dependendAssetDesc.fileName)) {
			log.debug("Dependency already loaded: " + dependendAssetDesc);
			Class type = assetTypes.get(dependendAssetDesc.fileName);
			RefCountedContainer assetRef = assets.get(type).get(dependendAssetDesc.fileName);
			assetRef.refCount++;
			incrementRefCountedDependencies(dependendAssetDesc.fileName);
		} else {
			log.info("Loading dependency: " + dependendAssetDesc);
			addTask(dependendAssetDesc);
		}
	}

	private void nextTask () {
		AssetDescriptor assetDesc = loadQueue.removeIndex(0);
		if (isLoaded(assetDesc.fileName)) {
			log.debug("Already loaded: " + assetDesc);
			Class type = assetTypes.get(assetDesc.fileName);
			RefCountedContainer assetRef = assets.get(type).get(assetDesc.fileName);
			assetRef.refCount++;
			incrementRefCountedDependencies(assetDesc.fileName);
			if (assetDesc.params != null && assetDesc.params.loadedCallback != null)
				assetDesc.params.loadedCallback.finishedLoading(this, assetDesc.fileName, assetDesc.type);
			loaded++;
		} else {
			log.info("Loading: " + assetDesc);
			addTask(assetDesc);
		}
	}

	private void addTask (AssetDescriptor assetDesc) {
		AssetLoader loader = getLoader(assetDesc.type, assetDesc.fileName);
		if (loader == null) throw new GdxRuntimeException("No loader for type: " + ClassReflection.getSimpleName(assetDesc.type));
		tasks.add(new AssetLoadingTask(this, assetDesc, loader, executor));
		peakTasks++;
	}

	protected <T> void addAsset (final String fileName, Class<T> type, T asset) {
		assetTypes.put(fileName, type);

		ObjectMap<String, RefCountedContainer> typeToAssets = assets.get(type);
		if (typeToAssets == null) {
			typeToAssets = new ObjectMap<String, RefCountedContainer>();
			assets.put(type, typeToAssets);
		}
		RefCountedContainer assetRef = new RefCountedContainer();
		assetRef.object = asset;
		typeToAssets.put(fileName, assetRef);
	}

	private boolean updateTask () {
		AssetLoadingTask task = tasks.peek();

		boolean complete = true;
		try {
			complete = task.cancel || task.update();
		} catch (RuntimeException ex) {
			task.cancel = true;
			taskFailed(task.assetDesc, ex);
		}

		if (complete) {
			if (tasks.size == 1) {
				loaded++;
				peakTasks = 0;
			}
			tasks.pop();

			if (task.cancel) return true;

			addAsset(task.assetDesc.fileName, task.assetDesc.type, task.asset);

			if (task.assetDesc.params != null && task.assetDesc.params.loadedCallback != null)
				task.assetDesc.params.loadedCallback.finishedLoading(this, task.assetDesc.fileName, task.assetDesc.type);

			long endTime = TimeUtils.nanoTime();
			log.debug("Loaded: " + (endTime - task.startTime) / 1000000f + "ms " + task.assetDesc);

			return true;
		}
		return false;
	}

	protected void taskFailed (AssetDescriptor assetDesc, RuntimeException ex) {
		throw ex;
	}

	private void incrementRefCountedDependencies (String parent) {
		Array<String> dependencies = assetDependencies.get(parent);
		if (dependencies == null) return;

		for (String dependency : dependencies) {
			Class type = assetTypes.get(dependency);
			RefCountedContainer assetRef = assets.get(type).get(dependency);
			assetRef.refCount++;
			incrementRefCountedDependencies(dependency);
		}
	}

	private void handleTaskError (Throwable t) {
		log.error("Error loading asset.", t);

		if (tasks.isEmpty()) throw new GdxRuntimeException(t);
		AssetLoadingTask task = tasks.pop();
		AssetDescriptor assetDesc = task.assetDesc;

		if (task.dependenciesLoaded && task.dependencies != null) {
			for (AssetDescriptor desc : task.dependencies)
				unload(desc.fileName);
		}

		tasks.clear();

		if (listener != null)
			listener.error(assetDesc, t);
		else
			throw new GdxRuntimeException(t);
	}

	public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader (Class<T> type, AssetLoader<T, P> loader) {
		setLoader(type, null, loader);
	}

	public synchronized <T, P extends AssetLoaderParameters<T>> void setLoader (Class<T> type, String suffix,
		AssetLoader<T, P> loader) {
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		if (loader == null) throw new IllegalArgumentException("loader cannot be null.");
		log.debug("Loader set: " + ClassReflection.getSimpleName(type) + " -> " + ClassReflection.getSimpleName(loader.getClass()));
		ObjectMap<String, AssetLoader> loaders = this.loaders.get(type);
		if (loaders == null) this.loaders.put(type, loaders = new ObjectMap<String, AssetLoader>());
		loaders.put(suffix == null ? "" : suffix, loader);
	}

	@Override
	public void dispose () {
		log.debug("Disposing.");
		clear();
		executor.dispose();
	}

	public void clear () {
		synchronized (this) {
			loadQueue.clear();
		}

		finishLoading();

		synchronized (this) {
			ObjectIntMap<String> dependencyCount = new ObjectIntMap<String>();
			while (assetTypes.size > 0) {
				dependencyCount.clear(51);
				Array<String> assets = assetTypes.keys().toArray();
				for (String asset : assets) {
					Array<String> dependencies = assetDependencies.get(asset);
					if (dependencies == null) continue;
					for (String dependency : dependencies)
						dependencyCount.getAndIncrement(dependency, 0, 1);
				}

				for (String asset : assets)
					if (dependencyCount.get(asset, 0) == 0) unload(asset);
			}

			this.assets.clear(51);
			this.assetTypes.clear(51);
			this.assetDependencies.clear(51);
			this.loaded = 0;
			this.toLoad = 0;
			this.peakTasks = 0;
			this.loadQueue.clear();
			this.tasks.clear();
		}
	}

	public Logger getLogger () {
		return log;
	}

	public synchronized int getReferenceCount (String fileName) {
		Class type = assetTypes.get(fileName);
		if (type == null) throw new GdxRuntimeException("Asset not loaded: " + fileName);
		return assets.get(type).get(fileName).refCount;
	}

	public synchronized void setReferenceCount (String fileName, int refCount) {
		Class type = assetTypes.get(fileName);
		if (type == null) throw new GdxRuntimeException("Asset not loaded: " + fileName);
		assets.get(type).get(fileName).refCount = refCount;
	}

	public synchronized Array<String> getDependencies (String fileName) {
		return assetDependencies.get(fileName);
	}

	static class RefCountedContainer {
		Object object;
		int refCount = 1;
	}
}
