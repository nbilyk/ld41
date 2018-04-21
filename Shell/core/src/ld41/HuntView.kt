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
import com.acornui.async.then
import com.acornui.component.ElementContainerImpl
import com.acornui.component.UiComponent
import com.acornui.component.image
import com.acornui.component.layout.algorithm.CanvasLayoutContainer
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.cachedGroup
import com.acornui.core.audio.SoundFactory
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.Owned
import com.acornui.core.di.inject
import com.acornui.core.di.own
import com.acornui.core.graphics.atlas
import com.acornui.core.graphics.perspectiveCamera
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.interaction.click
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.time.onTick
import com.acornui.gl.core.DEFAULT_SHADER_HEADER
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.ShaderProgramBase
import com.acornui.graphics.Color
import com.acornui.graphics.LightingRenderer
import com.acornui.graphics.UNPACK_FLOAT
import com.acornui.graphics.lighting.PointLight
import com.acornui.graphics.lighting.ambientLight
import com.acornui.graphics.lighting.directionalLight
import com.acornui.math.*
import com.esotericsoftware.spine.component.*
import ld41.component.repeatTexture
import ld41.model.TargetVo

class HuntView(owned: Owned) : CanvasLayoutContainer(owned) {

	val dataBind = DataBinding<TargetVo>()

	private val world: ElementContainerImpl<UiComponent>

	private val snipeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 32f * MathUtils.degRad
		far = 5000f
		setPosition(0f, -1000f, -4000f)
	}

	private val windowResizedHandler = { newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		snipeCam.setViewport(newWidth, newHeight)
	}

	private lateinit var scope: UiComponent

	private val camLookAt = Vector2(0f, 0f)

	private val tmpVec = Vector2()

	private var gunShot: SoundFactory? = null

	init {
		assets.load("assets/sounds/sniperRifle.mp3", AssetType.SOUND).then { gunShot = it }

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


		world = +World(this, camLookAt).apply {
			cameraOverride = snipeCam
		} layout { center() }

		+image {
			scope = +atlas("assets/hunt.json", "Scope")
		} layout { center(); fill() }

		click().add {
			world.testHit()
			gunShot?.createInstance()?.start()
		}

		onTick {
			scope.mousePosition(tmpVec)
			tmpVec.scl(SCOPE_SIZE_INV, SCOPE_SIZE_INV).sub(0.5f, 0.5f).scl(2f, 2f)
			val r = minOf(1f, tmpVec.len())
			if (r > DEAD_ZONE) {
				val scl = (r - DEAD_ZONE) / (1f - DEAD_ZONE) + DEAD_ZONE
				tmpVec.nor().scl(scl * SCOPE_MOVE_SPEED)
				//snipeCam.pointToLookAt()

				camLookAt.add(tmpVec.x, tmpVec.y)
				camMoveWindow.clampPoint(camLookAt)
				snipeCam.pointToLookAt(camLookAt.x, camLookAt.y, 0f)
				snipeCam.setUp(Vector3.NEG_Y)
			}

		}

	}

	override fun dispose() {
		super.dispose()
		window.sizeChanged.remove(windowResizedHandler)
	}

	companion object {
		private const val SCOPE_SIZE = 512f
		private const val SCOPE_SIZE_INV = 1f / SCOPE_SIZE
		private const val SCOPE_MOVE_SPEED = 90f
		private const val DEAD_ZONE = 0.01f
		private val camMoveWindow = MinMax(-2500f, 2500f, -2000f, 800f)
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


private class World(owner: Owned, private val camLookAt: Vector2Ro) : ElementContainerImpl<UiComponent>(owner) {
	private var skeletonC: SkeletonComponent? = null

	private val scene: SpineScene

	private val lightingRenderer = own(LightingRenderer(injector, numPointLights = 1, numShadowPointLights = 0, directionalShadowsResolution = 2048, useModel = false))

	private val ambientLight = ambientLight {
		color.set(Color(0.8f, 0.8f, 0.8f, 1f))
	}

	private val directionalLight = directionalLight {
		direction.set(0f, 1f, 1f)
		color.set(Color(1f, 1f, 1f, 1f))
	}

	private val pointLights = Array(lightingRenderer.numPointLights, {
		PointLight()
	}).toList()

	init {
		+repeatTexture("assets/grass.jpg") {
			setSize(2000f, 2000f)
			setPosition(-1000f, 0f, -1000f)
			rotationX = PI * 0.5f
		}

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

	private val gl = inject(Gl20)

	override fun draw() {
//		lightingRenderer.directionalLightCamera.setClipSpaceFromWorld(camLookAt.x - 1000f, camLookAt.y + 1000f, camLookAt.y - 1000f, camLookAt.y + 1000f, -100f, 500f, camera)
		lightingRenderer.render(camera, ambientLight, directionalLight, pointLights, {
			//gl.cullFace(Gl20.FRONT)
			scene.render()
		}, {
			//gl.cullFace(Gl20.BACK)
			_drawScene()
		})
//		super.draw()
	}

	private fun _drawScene() {
//		gl.enable(Gl20.DEPTH_TEST)
//		gl.depthFunc(Gl20.LESS)
		super.draw()
//		gl.disable(Gl20.DEPTH_TEST)
	}
}