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

package com.badlogic.gdx.utils;

public class GdxNativesLoader {
	static public boolean disableNativesLoading = false;

	static private boolean nativesLoaded;

	/** Loads the libgdx native libraries if they have not already been loaded. */
	static public synchronized void load () {
		if (nativesLoaded) return;

		if (disableNativesLoading) return;

		new SharedLibraryLoader().load("gdx");
		nativesLoaded = true;
	}
}
