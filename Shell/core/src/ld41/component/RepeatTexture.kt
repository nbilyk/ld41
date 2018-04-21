/*
 * Copyright 2018 Nicholas Bilyk
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

package ld41.component


import com.acornui.core.di.Owned
import com.acornui.core.graphics.Texture
import com.acornui.gl.component.GlTextureComponent
import com.acornui.gl.core.TextureMinFilter
import com.acornui.gl.core.TextureWrapMode
import com.acornui.math.Bounds

/**
 * @author nbilyk
 */
class RepeatTexture(
		owner: Owned
) : GlTextureComponent(owner) {

	override fun _setTexture(value: Texture?) {
		if (value != null) {
			value.filterMin = TextureMinFilter.LINEAR_MIPMAP_LINEAR
			value.wrapS = TextureWrapMode.REPEAT
			value.wrapT = TextureWrapMode.REPEAT
		}
		super._setTexture(value)
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		val t = texture ?: return
		val tW = t.width.toFloat()
		val tH = t.height.toFloat()
		val w = explicitWidth ?: tW
		val h = explicitHeight ?: tH
		setUV(0f, 0f, w / tW, h / tH)
		super.updateLayout(explicitWidth, explicitHeight, out)
	}
}

fun Owned.repeatTexture(path: String, init: RepeatTexture.() -> Unit = {}): RepeatTexture {
	val g = RepeatTexture(this)
	g.path = path
	g.init()
	return g
}
