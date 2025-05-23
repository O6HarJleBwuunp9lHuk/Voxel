package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FlushablePool;
import com.badlogic.gdx.utils.GdxRuntimeException;



public class ModelBatch implements Disposable {
	protected static class RenderablePool extends FlushablePool<Renderable> {
		@Override
		protected Renderable newObject () {
			return new Renderable();
		}

		@Override
		public Renderable obtain () {
			Renderable renderable = super.obtain();
			renderable.environment = null;
			renderable.material = null;
			renderable.meshPart.set("", null, 0, 0, 0);
			renderable.shader = null;
			renderable.userData = null;
			return renderable;
		}
	}

	protected Camera camera;
	protected final RenderablePool renderablesPool = new RenderablePool();
	protected final Array<Renderable> renderables = new Array<Renderable>();
	protected final RenderContext context;
	private final boolean ownContext;
	protected final ShaderProvider shaderProvider;
	protected final RenderableSorter sorter;

	public ModelBatch (final RenderContext context, final ShaderProvider shaderProvider, final RenderableSorter sorter) {
		this.sorter = (sorter == null) ? new DefaultRenderableSorter() : sorter;
		this.ownContext = (context == null);
		this.context = (context == null) ? new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1)) : context;
		this.shaderProvider = (shaderProvider == null) ? new DefaultShaderProvider() : shaderProvider;
	}

	public ModelBatch (final RenderContext context, final ShaderProvider shaderProvider) {
		this(context, shaderProvider, null);
	}

	public ModelBatch (final RenderContext context, final RenderableSorter sorter) {
		this(context, null, sorter);
	}

	public ModelBatch (final RenderContext context) {
		this(context, null, null);
	}

	public ModelBatch (final ShaderProvider shaderProvider, final RenderableSorter sorter) {
		this(null, shaderProvider, sorter);
	}

	public ModelBatch (final RenderableSorter sorter) {
		this(null, null, sorter);
	}

	public ModelBatch (final ShaderProvider shaderProvider) {
		this(null, shaderProvider, null);
	}

	public ModelBatch (final FileHandle vertexShader, final FileHandle fragmentShader) {
		this(null, new DefaultShaderProvider(vertexShader, fragmentShader), null);
	}

	public ModelBatch (final String vertexShader, final String fragmentShader) {
		this(null, new DefaultShaderProvider(vertexShader, fragmentShader), null);
	}

	public ModelBatch () {
		this(null, null, null);
	}

	public void begin (final Camera cam) {
		if (camera != null) throw new GdxRuntimeException("Call end() first.");
		camera = cam;
		if (ownContext) context.begin();
	}

	public void setCamera (final Camera cam) {
		if (camera == null) throw new GdxRuntimeException("Call begin() first.");
		if (renderables.size > 0) flush();
		camera = cam;
	}

	public Camera getCamera () {
		return camera;
	}

	public boolean ownsRenderContext () {
		return ownContext;
	}

	public RenderContext getRenderContext () {
		return context;
	}

	public ShaderProvider getShaderProvider () {
		return shaderProvider;
	}

	public RenderableSorter getRenderableSorter () {
		return sorter;
	}

	public void flush () {
		sorter.sort(camera, renderables);
		Shader currentShader = null;
		for (int i = 0; i < renderables.size; i++) {
			final Renderable renderable = renderables.get(i);
			if (currentShader != renderable.shader) {
				if (currentShader != null) currentShader.end();
				currentShader = renderable.shader;
				currentShader.begin(camera, context);
			}
			currentShader.render(renderable);
		}
		if (currentShader != null) currentShader.end();
		renderablesPool.flush();
		renderables.clear();
	}

	public void end () {
		flush();
		if (ownContext) context.end();
		camera = null;
	}

	public void render (final Renderable renderable) {
		renderable.shader = shaderProvider.getShader(renderable);
		renderables.add(renderable);
	}


	public void render (final RenderableProvider renderableProvider) {
		final int offset = renderables.size;
		renderableProvider.getRenderables(renderables, renderablesPool);
		for (int i = offset; i < renderables.size; i++) {
			Renderable renderable = renderables.get(i);
			renderable.shader = shaderProvider.getShader(renderable);
		}
	}

	public <T extends RenderableProvider> void render (final Iterable<T> renderableProviders) {
		for (final RenderableProvider renderableProvider : renderableProviders)
			render(renderableProvider);
	}

	public void render (final RenderableProvider renderableProvider, final Environment environment) {
		final int offset = renderables.size;
		renderableProvider.getRenderables(renderables, renderablesPool);
		for (int i = offset; i < renderables.size; i++) {
			Renderable renderable = renderables.get(i);
			renderable.environment = environment;
			renderable.shader = shaderProvider.getShader(renderable);
		}
	}

	public <T extends RenderableProvider> void render (final Iterable<T> renderableProviders, final Environment environment) {
		for (final RenderableProvider renderableProvider : renderableProviders)
			render(renderableProvider, environment);
	}

	public void render (final RenderableProvider renderableProvider, final Shader shader) {
		final int offset = renderables.size;
		renderableProvider.getRenderables(renderables, renderablesPool);
		for (int i = offset; i < renderables.size; i++) {
			Renderable renderable = renderables.get(i);
			renderable.shader = shader;
			renderable.shader = shaderProvider.getShader(renderable);
		}
	}

	public <T extends RenderableProvider> void render (final Iterable<T> renderableProviders, final Shader shader) {
		for (final RenderableProvider renderableProvider : renderableProviders)
			render(renderableProvider, shader);
	}

	public void render (final RenderableProvider renderableProvider, final Environment environment, final Shader shader) {
		final int offset = renderables.size;
		renderableProvider.getRenderables(renderables, renderablesPool);
		for (int i = offset; i < renderables.size; i++) {
			Renderable renderable = renderables.get(i);
			renderable.environment = environment;
			renderable.shader = shader;
			renderable.shader = shaderProvider.getShader(renderable);
		}
	}

	public <T extends RenderableProvider> void render (final Iterable<T> renderableProviders, final Environment environment,
		final Shader shader) {
		for (final RenderableProvider renderableProvider : renderableProviders)
			render(renderableProvider, environment, shader);
	}

	@Override
	public void dispose () {
		shaderProvider.dispose();
	}
}
