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

package ld41

import com.acornui.async.launch
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.component.image
import com.acornui.component.layout.algorithm.CanvasLayoutContainer
import com.acornui.core.assets.cachedGroup
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.graphics.atlas
import com.acornui.core.graphics.perspectiveCamera
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.interaction.click
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.time.onTick
import com.acornui.math.MinMax
import com.acornui.math.MathUtils
import com.acornui.math.Vector2
import com.acornui.math.Vector3
import com.esotericsoftware.spine.component.*
import ld41.model.TargetVo

class HuntView(owned: Owned) : CanvasLayoutContainer(owned) {

	val dataBind = DataBinding<TargetVo>()

	private val world: ElementContainerImpl<UiComponent>

	private val snipeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 32f * MathUtils.degRad
		far = 5000f
		setPosition(0f, 0f, -2000f)
	}

	private val windowResizedHandler = { newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		snipeCam.setViewport(newWidth, newHeight)
	}

	private lateinit var scope: UiComponent

	private val cursorOffset = Vector2(0f, 0f)
	private val camLookAt = Vector2(0f, 0f)

	private val tmpVec = Vector2()

	init {
		focusEnabled = true

		window.sizeChanged.add(windowResizedHandler)
		windowResizedHandler(window.width, window.height, false)

		cursor(StandardCursors.NONE)


//		+text {
//			dataBind.bind {
//				text = "${if (it.killed) "Dead" else "Hunt"} ${it.name}"
//			}
//		}
//
//		+button("Killed") {
//			click().add {
//				val target = dataBind.get()
//				if (target != null)
//					invokeCommand(KillCommand(target))
//
//			}
//		}
//
//		+button("Missed") {
//			click().add {
//				val target = dataBind.get()
//				if (target != null)
//					invokeCommand(MissCommand(target))
//			}
//		}


		world = +World(this).apply {
			cameraOverride = snipeCam
		} layout { center() }

		+image {
			scope = +atlas("assets/hunt.json", "Scope")
		} layout { center(); fill() }

		click().add {
			world.testHit()
		}

		onTick {
			scope.mousePosition(tmpVec)
			tmpVec.scl(SCOPE_SIZE_INV, SCOPE_SIZE_INV).sub(0.5f, 0.5f).scl(2f, 2f).sub(cursorOffset)
			val r = minOf(1f, tmpVec.len())
			if (r > DEAD_ZONE) {
				var scl = (r - DEAD_ZONE) / (1f - DEAD_ZONE)
				//scl *= scl
				tmpVec.nor().scl(scl * SCOPE_MOVE_SPEED)
				//snipeCam.pointToLookAt()

				camLookAt.add(tmpVec.x, tmpVec.y)
				camMoveWindow.clampPoint(camLookAt)
				snipeCam.pointToLookAt(camLookAt.x, camLookAt.y, 0f)
				snipeCam.setUp(Vector3.NEG_Y)
			}

		}

	}

	override fun onActivated() {
		super.onActivated()
		scope.mousePosition(tmpVec)
		tmpVec.scl(SCOPE_SIZE_INV, SCOPE_SIZE_INV).sub(0.5f, 0.5f).scl(2f, 2f)
		cursorOffset.set(tmpVec)
	}

	override fun dispose() {
		super.dispose()
		window.sizeChanged.remove(windowResizedHandler)
	}

	companion object {
		private const val SCOPE_SIZE = 512f
		private const val SCOPE_SIZE_INV = 1f / SCOPE_SIZE
		private const val SCOPE_MOVE_SPEED = 30f
		private const val DEAD_ZONE = 0.02f
		private val camMoveWindow = MinMax(-1000f, 1000f, -1000f, 1000f)
	}


}

class KillCommand(val target: TargetVo) : Command {

	override val type: CommandType<out Command> = Companion

	companion object : CommandType<KillCommand>
}

class MissCommand(val target: TargetVo) : Command {

	override val type: CommandType<out Command> = Companion

	companion object : CommandType<MissCommand>
}


private class World(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {
	private var skeletonC: SkeletonComponent? = null

	private val scene: SpineScene

	init {
		scene = +spineScene {
			launch {
				val skeleton = loadSkeleton("assets/personSkel.json", "assets/person.json", null, cachedGroup()).await()
				skeletonC = +skeletonComponent(skeleton) {
					animationState.data.defaultMix = 0.25f
					animationState.setAnimation(0, "walk", loop = true)
				}
			}
		}
	}

	override fun updateConcatenatedTransform() {
		super.updateConcatenatedTransform()
		_concatenatedTransform.setTranslation(0f, 0f, 0f)
		//println("_concatenatedTransform $_concatenatedTransform")
	}

	private val tmp = Vector2()

	fun testHit() {
		val skeletonC = skeletonC ?: return
		scene.mousePosition(tmp)
		val slot = skeletonC.getSlotAtPosition(tmp.x, tmp.y)
		println("Slot $slot")
	}
}