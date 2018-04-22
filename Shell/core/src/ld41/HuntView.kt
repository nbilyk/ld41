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
import com.acornui.component.container
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
import com.acornui.gl.component.StencilUtil
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphics.Color
import com.acornui.graphics.LightingRenderer
import com.acornui.graphics.lighting.PointLight
import com.acornui.graphics.lighting.ambientLight
import com.acornui.graphics.lighting.directionalLight
import com.acornui.math.*
import com.esotericsoftware.spine.component.*
import ld41.component.repeatTexture
import ld41.model.TargetVo

class HuntView(owned: Owned) : CanvasLayoutContainer(owned) {

	val dataBind = DataBinding<TargetVo?>()

	private val world: World

	private val scopeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 16f * MathUtils.degRad
		setPosition(0f, -1000f, -16000f)
		far = -position.z + 2000f
	}

	private val noScopeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 67f * MathUtils.degRad
		setPosition(0f, -1000f, -16000f)
		far = -position.z + 2000f
	}

	private val windowResizedHandler = { newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		scopeCam.setViewport(newWidth, newHeight)
	}

	private lateinit var scope: UiComponent

	private val camLookAt = Vector2(0f, 0f)

	private val tmpVec = Vector2()

	private var gunShot: SoundFactory? = null
	private var chamber: SoundFactory? = null

	private val bullet = Bullet()

	private var shotRequested: Boolean = false

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
		assets.load("assets/sounds/sniperRifle.mp3", AssetType.SOUND).then { gunShot = it }
		assets.load("assets/sounds/chamber.mp3", AssetType.SOUND).then { chamber = it }

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
			cameraOverride = noScopeCam
		} layout { center() }



		+image {
			scope = +atlas("assets/hunt.json", "Scope")
		} layout { center(); fill() }

		click().add {
			shotRequested = true
		}

		onTick {
			scope.mousePosition(tmpVec)
			tmpVec.scl(SCOPE_SIZE_INV, SCOPE_SIZE_INV).sub(0.5f, 0.5f).scl(2f, 2f)
			val r = minOf(1f, tmpVec.len())
			if (r > DEAD_ZONE) {
				val scl = (r - DEAD_ZONE) / (1f - DEAD_ZONE) + DEAD_ZONE
				tmpVec.nor().scl(scl * SCOPE_MOVE_SPEED)
				//scopeCam.pointToLookAt()

				camLookAt.add(tmpVec.x, tmpVec.y)
				camMoveWindow.clampPoint(camLookAt)
				scopeCam.pointToLookAt(camLookAt.x, camLookAt.y, 0f)
				scopeCam.setUp(Vector3.NEG_Y)

				noScopeCam.pointToLookAt(camLookAt.x, camLookAt.y, 0f)
				noScopeCam.setUp(Vector3.NEG_Y)
			}

			if (shotRequested) {
				shotRequested = false
				if (!bullet.isActive) {
					bullet.isActive = true
					resetBullet(bullet)
					world.bulletView.visible = true
					gunShot?.createInstance()?.start()
				}
			}

			if (bullet.isActive) {
				val previousP = bullet.position.z
				bullet.update()
				if (bullet.position.z > 0f && previousP < 0f) println("hit")
				//println("bullet.position ${bullet.position}")
				world.bulletView.setPosition(bullet.position)
				if (bullet.life == 40) {
					chamber?.createInstance()?.start()
				}
				if (bullet.life < 0) {
					bullet.isActive = false
				}
			}
		}

		resetBullet(bullet)

	}

	private fun resetBullet(bullet: Bullet) {
		world.bulletView.visible = false

		bullet.life = 150
		bullet.position.set(scopeCam.position)
		bullet.velocity.set(camLookAt).sub(bullet.position).nor().scl(200f)
	}

	private val gl = inject(Gl20)
	private val glState = inject(GlState)

	override fun draw() {
		lightingRenderer.directionalLightCamera.setClipSpaceFromWorld(camLookAt.x - 1200f, camLookAt.y + 1200f, camLookAt.y - 1200f, camLookAt.y + 1200f, -100f, 2000f, scopeCam)
		lightingRenderer.render(camera, ambientLight, directionalLight, pointLights, {
			world.drawOcclusion()
		}, {
			val batch = glState.batch
			batch.flush(true)
			gl.clear(Gl20.STENCIL_BUFFER_BIT)
			gl.enable(Gl20.STENCIL_TEST)
			gl.colorMask(false, false, false, false)
			gl.stencilFunc(Gl20.ALWAYS, 1, 0.inv())
			gl.stencilOp(Gl20.REPLACE, Gl20.REPLACE, Gl20.REPLACE)
			scope.render()
			batch.flush(true)

			gl.colorMask(true, true, true, true)
			gl.stencilFunc(Gl20.NOTEQUAL, 1, 0.inv())
			gl.stencilOp(Gl20.KEEP, Gl20.KEEP, Gl20.KEEP)

			world.cameraOverride = noScopeCam
			world.render()
			batch.flush(true)

			gl.stencilFunc(Gl20.EQUAL, 1, 0.inv())

			world.cameraOverride = scopeCam
			world.render()
			batch.flush(true)

			gl.clear(Gl20.STENCIL_BUFFER_BIT)
			gl.disable(Gl20.STENCIL_TEST)

			scope.render()
		})
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

class KillCommand(val target: TargetVo?) : Command {

	override val type: CommandType<out Command> = Companion

	companion object : CommandType<KillCommand>
}

class MissCommand(val target: TargetVo?) : Command {

	override val type: CommandType<out Command> = Companion

	companion object : CommandType<MissCommand>
}


private class World(
		owner: Owned,
		private val camLookAt: Vector2Ro
) : ElementContainerImpl<UiComponent>(owner) {
	private var skeletonC: SkeletonComponent? = null

	private val scene: SpineScene

	val bulletView: UiComponent

	init {
		+repeatTexture("assets/grass.jpg") {
			setSize(8000f, 20000f)
			setPosition(-4000f, 0f, -16000f)
			rotationX = PI * 0.5f
		}

		+container {
			+BuildingView(this).apply {
				moveTo(-BuildingView.WIDTH - 400f, 0f, 1000f)
			}

			+BuildingView(this).apply {
				moveTo(0f, 0f, 1000f)
			}

			+BuildingView(this).apply {
				moveTo(BuildingView.WIDTH + 400f, 0f, 1000f)
			}
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

		+container {
			+BuildingView(this).apply {
				moveTo(-BuildingView.WIDTH - 400f, 0f, 500f)
			}

			+BuildingView(this).apply {
				moveTo(BuildingView.WIDTH + 400f, 0f, 500f)
			}
		}

		bulletView = +atlas("assets/hunt.json", "Bullet") {
			visible = false
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

	fun drawOcclusion() {
		scene.render()
	}
}

private class Bullet {

	var isActive = false

	val position = Vector3()
	val velocity = Vector3()
	val acceleration = GRAVITY
	var life: Int = 0

	fun update() {
		position.add(velocity)
		velocity.add(acceleration)
		life--
	}

}

private class BuildingView(owner: Owned) : ElementContainerImpl<UiComponent>(owner) {

	init {
		+atlas("assets/hunt.json", "Building") {
			setScaling(10f, 10f)
			rotationY = -PI / 2f
			setPosition(WIDTH, -HEIGHT)
		}

		+atlas("assets/hunt.json", "Building") {
			setScaling(10f, 10f)
			rotationY = PI / 2f
			setPosition(0f, -HEIGHT, WIDTH)
		}

		+atlas("assets/hunt.json", "Building") {
			setScaling(10f, 10f)
			setPosition(0f, -HEIGHT)
		}
	}

	companion object {
		const val HEIGHT = 3000f
		const val WIDTH = 1500f
	}

}

interface HitTarget {

	fun hitCheck(x: Float, y: Float): Boolean
}

private val GRAVITY = Vector3(0f, 0.2f, 0f)