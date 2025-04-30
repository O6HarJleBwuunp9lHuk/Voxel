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

package com.badlogic.gdx.assets.loaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

public class ShaderProgramLoader extends AsynchronousAssetLoader<ShaderProgram, ShaderProgramLoader.ShaderProgramParameter> {

	private String vertexFileSuffix = ".vert";
	private String fragmentFileSuffix = ".frag";

	public ShaderProgramLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	public ShaderProgramLoader (FileHandleResolver resolver, String vertexFileSuffix, String fragmentFileSuffix) {
		super(resolver);
		this.vertexFileSuffix = vertexFileSuffix;
		this.fragmentFileSuffix = fragmentFileSuffix;
	}

	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, ShaderProgramParameter parameter) {
		return null;
	}

	@Override
	public void loadAsync (AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
	}

	@Override
	public ShaderProgram loadSync (AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
		String vertFileName = null, fragFileName = null;
		if (parameter != null) {
			if (parameter.vertexFile != null) vertFileName = parameter.vertexFile;
			if (parameter.fragmentFile != null) fragFileName = parameter.fragmentFile;
		}
		if (vertFileName == null && fileName.endsWith(fragmentFileSuffix)) {
			vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix;
		}
		if (fragFileName == null && fileName.endsWith(vertexFileSuffix)) {
			fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix;
		}
		FileHandle vertexFile = vertFileName == null ? file : resolve(vertFileName);
		FileHandle fragmentFile = fragFileName == null ? file : resolve(fragFileName);
		String vertexCode = vertexFile.readString();
		String fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : fragmentFile.readString();
		if (parameter != null) {
			if (parameter.prependVertexCode != null) vertexCode = parameter.prependVertexCode + vertexCode;
			if (parameter.prependFragmentCode != null) fragmentCode = parameter.prependFragmentCode + fragmentCode;
		}

		ShaderProgram shaderProgram = new ShaderProgram(vertexCode, fragmentCode);
		if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
			manager.getLogger().error("ShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog());
		}

		return shaderProgram;
	}

	static public class ShaderProgramParameter extends AssetLoaderParameters<ShaderProgram> {

		public String vertexFile;
		public String fragmentFile;
		public boolean logOnCompileFailure = true;
		public String prependVertexCode;
		public String prependFragmentCode;
	}
}
