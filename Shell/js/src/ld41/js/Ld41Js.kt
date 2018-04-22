/*
 * Copyright 2015 Nicholas Bilyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ld41.js

import com.acornui.component.stage
import com.acornui.core.AppConfig
import com.acornui.core.GlConfig
import com.acornui.core.WindowConfig
import com.acornui.js.gl.WebGlApplication
import ld41.Ld41

fun main(args: Array<String>) {
	WebGlApplication("ld41Root").start(AppConfig(frameRate = 60, window = WindowConfig(title = "Ld41"), gl = GlConfig(antialias = true))) {
		stage.addElement(Ld41(this)) // Ignore the inspection error, this is because we're mixing module types.
	}
}


