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
import com.acornui.collection.*
import com.acornui.component.*
import com.acornui.component.layout.algorithm.CanvasLayoutContainer
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetType
import com.acornui.core.assets.cachedGroup
import com.acornui.core.audio.SoundFactory
import com.acornui.core.cursor.StandardCursors
import com.acornui.core.cursor.cursor
import com.acornui.core.di.*
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.atlas
import com.acornui.core.graphics.perspectiveCamera
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.interaction.click
import com.acornui.core.input.wheel
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.time.onTick
import com.acornui.gl.core.Gl20
import com.acornui.gl.core.GlState
import com.acornui.graphics.Color
import com.acornui.graphics.LightingRenderer
import com.acornui.graphics.lighting.PointLight
import com.acornui.graphics.lighting.ambientLight
import com.acornui.graphics.lighting.directionalLight
import com.acornui.math.*
import com.acornui.math.MathUtils.clamp
import com.acornui.math.MathUtils.random
import com.acornui.math.MathUtils.randomBoolean
import com.esotericsoftware.spine.component.*
import ld41.PersonView.PersonState.*
import ld41.component.repeatTexture
import ld41.model.TargetVo
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class HuntView(owned: Owned) : CanvasLayoutContainer(owned) {

	val dataBind = DataBinding<TargetVo?>()

	private val sky: Rect
	private val world: World
	private val darken: Rect

	private val scopeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 24f * MathUtils.degRad
		setPosition(0f, -BuildingView.HEIGHT, -16000f)
		far = -position.z + 10000f
	}

	private val noScopeCam = perspectiveCamera(autoCenter = false) {
		fieldOfView = 67f * MathUtils.degRad
		setPosition(0f, -BuildingView.HEIGHT, -16000f)
		far = -position.z + 15000f
	}

	private val windowResizedHandler = { newWidth: Float, newHeight: Float, isUserInteraction: Boolean ->
		scopeCam.setViewport(newWidth * window.scaleX, newHeight * window.scaleY)
		noScopeCam.setViewport(scopeCam.viewportWidth, scopeCam.viewportHeight)
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

	private val pointLights = ArrayList(lightingRenderer.numPointLights, {
		PointLight()
	})

	private var reloadCooldown: Int = 0

	init {
		wheel().add {
			if (it.deltaY != 0f) {
				val diff = if (it.deltaY > 0f) {
					10f
				} else {
					-10f
				}
				scopeCam.fieldOfView = clamp(scopeCam.fieldOfView + diff * MathUtils.degRad, MIN_ANGLE, MAX_ANGLE)
			}
		}


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

		sky = +rect {
			style.backgroundColor = Color(0xD9E8FDFF)
		} layout { fill() }

		world = +World(this, noScopeCam).apply {
		} layout { center() }


		+image {
			scope = +atlas("assets/hunt.json", "Scope")
		} layout { center(); fill() }

		darken = +rect {
			style.backgroundColor = Color(0x00000066)
		} layout { fill() }

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
				if (reloadCooldown <= 0) {
					bullet.isActive = true
					resetBullet(bullet)
					reloadCooldown = 150
					world.bulletView.visible = true
					gunShot?.createInstance()?.start()
				}
			}

			if (bullet.isActive) {

				val p = bullet.position
				val v = bullet.velocity

				// Collision detection
				for (i in 0..world.hitTargets.lastIndex) {
					val target = world.hitTargets[i]
					if (p.z < target.hitZ && p.z + v.z > target.hitZ) {
						val percent = (target.hitZ - p.z) / (v.z)
						val bulletX = p.x + percent * v.x
						val bulletY = p.y + percent * v.y
						if (p.y > 0f || target.hitCheck(bulletX, bulletY)) {
							bullet.isActive = false
							world.bulletView.visible = false

							for (j in 0..world.people.lastIndex) {
								val person = world.people[j]
								val skel = person.skeletonC.skeleton
								println(MathUtils.sqrt(Vector3.dst2(skel.x, skel.y, person.scene.z, bulletX, bulletY, p.z)))
								if (Vector3.dst2(skel.x, skel.y, person.scene.z, bulletX, bulletY, p.z) < 800f * 800f) {
									person.scare()
								}
							}
						}
					}
				}

				bullet.update()

				//if (bullet.position.z > 0f && previousP < 0f) println("hit")
				//println("bullet.position ${bullet.position}")
				world.bulletView.setPosition(bullet.position)
				if (bullet.life < 0) {
					bullet.isActive = false
					world.bulletView.visible = false
				}
			}
			if (reloadCooldown > 0 && reloadCooldown-- == 45) {
				chamber?.createInstance()?.start()
			}
		}

		resetBullet(bullet)

	}

	private fun resetBullet(bullet: Bullet) {
		bullet.life = 150
		bullet.position.set(scopeCam.position)
		bullet.velocity.set(camLookAt).sub(bullet.position).nor().scl(200f)
	}

	private val gl = inject(Gl20)
	private val glState = inject(GlState)

	override fun draw() {
		lightingRenderer.directionalLightCamera.setClipSpaceFromWorld(-4000f, 4000f, -2000f, 2000f, -5000f, 1000f, scopeCam)
		lightingRenderer.render(scopeCam, ambientLight, directionalLight, pointLights, {
			world.drawOcclusion()
		}, {
			sky.render()

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
			darken.render()
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
		private const val SCOPE_MOVE_SPEED = 150f
		private const val DEAD_ZONE = 0.01f
		private val MIN_ANGLE = 16f * MathUtils.degRad
		private val MAX_ANGLE = 50f * MathUtils.degRad

		private val camMoveWindow = MinMax(-10000f, 10000f, -8000f, 1800f)
	}


}

private class World(
		owner: Owned,
		private val cam: Camera
) : ElementContainerImpl<UiComponent>(owner) {

	private val scene: SpineScene

	val bulletView: UiComponent
	val ground: UiComponent


	private val _hitTargets = ArrayList<HitTarget>()
	val hitTargets: List<HitTarget>
		get() = _hitTargets

	private val _people = ArrayList<PersonView>()
	val people: List<PersonView>
		get() = _people


	private val bulletHoleContainer = container()

	private var occlusionStart: Int
	private var occlusionEnd: Int

	private val sounds = HuntSounds(injector)


	init {
		+Clouds(this).apply {
			setSize(800f, 600f)
			setScaling(30f, 30f)
			setPosition(-(explicitWidth!! * scaleX) * 0.5f, -15000f, 4000f)
		}

		ground = +repeatTexture("assets/grass.jpg") {
			setSize(18000f, 20000f)
			setPosition(-explicitWidth!! * 0.5f, 0f, cam.position.z)
			rotationX = PI * 0.5f
		}

		occlusionStart = elements.size

		+container {
			+building(-BuildingView.WIDTH - 400f, 1000f)
			+building(0f, 1000f)
			+building(BuildingView.WIDTH + 400f, 1000f)
		}


		scene = +spineScene {
			launch {
				val loadedSkeleton = loadSkeleton("assets/personSkel.json", "assets/person.json", null, cachedGroup()).await()
				val person = PersonView(injector, this, sounds, loadedSkeleton)
				_people.add(person)
				_hitTargets.add(person)
			}
		}

		+container {
			+building(-BuildingView.WIDTH - 400f, -5000f)
			+building(BuildingView.WIDTH + 400f, -5000f)
		}

		+bulletHoleContainer

		occlusionEnd = elements.size

		bulletView = +atlas("assets/hunt.json", "Bullet") {
			setOrigin(4f, 4f)
			setScaling(3f, 3f)
			visible = false
		}
	}

	private fun building(x: Float, z: Float): UiComponent {
		return BuildingView(this, bulletHoleContainer).apply {
			moveTo(x, 0f, z)
			_hitTargets.add(this)
		}
	}

	override fun updateConcatenatedTransform() {
		super.updateConcatenatedTransform()
		_concatenatedTransform.setTranslation(0f, 0f, 0f)
		//println("_concatenatedTransform $_concatenatedTransform")
	}

	fun drawOcclusion() {
		for (i in occlusionStart..occlusionEnd - 1) {
			_children[i].render()
		}
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

private class BuildingView(owner: Owned, private val bulletHoleContainer: ElementContainerImpl<UiComponent>) : ElementContainerImpl<UiComponent>(owner), HitTarget {

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

	override val hitZ: Float
		get() = this.z

	override fun hitCheck(hitX: Float, hitY: Float): Boolean {
		val hit = hitX > x && hitY < y && hitX < x + WIDTH && hitY > -HEIGHT
		if (hit) {
			bulletHoleContainer.addElement(atlas("assets/hunt.json", "BulletHole") {
				setOrigin(17f, 8f)
				setScaling(3f, 3f)
				setPosition(hitX, hitY, hitZ)
			})

		}
		return hit
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.clear()
	}

	companion object {
		const val HEIGHT = 3000f
		const val WIDTH = 1500f
	}

}

interface HitTarget {

	val hitZ: Float

	/**
	 * Processes a hit check. If true, handle it.
	 */
	fun hitCheck(hitX: Float, hitY: Float): Boolean
}

class Clouds(owner: Owned) : ContainerImpl(owner) {

	private val cloudPool = ArrayList(20, { Cloud(this) })
	private val activeClouds = ArrayList<Cloud>()

	private val cloudIterator = MutableListIteratorImpl(activeClouds)

	private val desiredCloudCount = 10
	private val accumulatorRate = 1f / 10f
	private var accumulator: Float = 0f

	init {
		for (i in 0..desiredCloudCount) {
			val cloud = createCloud()
			cloud.life = random(0f, cloud.lifeSpan)
		}

		onTick { stepTime ->
			cloudIterator.clear()
			while (cloudIterator.hasNext()) {
				val cloud = cloudIterator.next()
				cloud.life += stepTime

				if (cloud.life > cloud.lifeSpan) {
					removeChild(cloud)
					cloudIterator.remove()
					cloudPool.add(cloud)
				}
			}
			if (activeClouds.size < desiredCloudCount) {
				accumulator += accumulatorRate * stepTime
				if (accumulator > 1f) {
					accumulator--
					createCloud()
				}
			}

			layoutClouds(width, height)
		}
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		layoutClouds(explicitWidth ?: 0f, explicitHeight ?: 0f)
	}

	private fun layoutClouds(w: Float, h: Float) {
		for (i in 0..activeClouds.lastIndex) {
			val cloud = activeClouds[i]
			var xP = cloud.life / cloud.lifeSpan
			if (cloud.direction) xP = 1f - xP
			cloud.x = xP * (w + cloud.width * 2f) - cloud.width
		}
	}

	private fun createCloud(): Cloud {
		val newCloud = cloudPool.removeAt(random(0, cloudPool.lastIndex))
		newCloud.lifeSpan = random(60f, 120f)
		newCloud.life = 0f
		newCloud.y = random(-60f, 60f)
		newCloud.direction = randomBoolean()
		//newCloud.y = random(0f, 500f)
		addChild(newCloud)
		activeClouds.add(newCloud)
		return newCloud
	}

}

class Cloud(owner: Owned) : ContainerImpl(owner) {

	var life: Float = 0f
	var lifeSpan: Float = 0f
	var direction: Boolean = false

	private val cloud: UiComponent

	init {
		val r = random(1, 3)
		cloud = addChild(atlas("assets/hunt.json", "Cloud$r") {
			scaleX = random(1f, 2f)
			scaleY = scaleX

			if (randomBoolean()) {
				scaleX *= -1f
			}
		})
	}

	override fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
		out.set(cloud.width * cloud.scaleX, cloud.height * cloud.scaleY)
	}
}

class HuntSounds(override val injector: Injector) : Scoped {

	var torture: SoundFactory? = null

	private val assets = inject(AssetManager)

	init {
		assets.load("assets/sounds/torture.mp3", AssetType.SOUND).then { torture = it }

	}
}

private val GRAVITY = Vector3(0f, 0.2f, 0f)

private class PersonView(override val injector: Injector, val scene: SpineScene, val sounds: HuntSounds, loadedSkeleton: LoadedSkeleton) : Scoped, HitTarget {

	val skeletonC: SkeletonComponent = skeletonComponent(loadedSkeleton) {
		animationState.data.defaultMix = 0.25f
	}

	private var state: PersonState by observable(WALKING) {
		val animationState = skeletonC.animationState
		when (it) {
			DEAD -> animationState.setAnimation(0, "death", loop = false)
			IDLE -> animationState.setAnimation(0, "idle", loop = true)
			STARTLED -> animationState.setAnimation(0, "startled", loop = false)
			WALKING -> animationState.setAnimation(0, "walk", loop = true)
			RUNNING -> animationState.setAnimation(0, "run", loop = true)
		}
	}

	private var flipX: Boolean by observable(randomBoolean()) {
		skeletonC.skeleton.flipX = it
	}

	private var startledTime: Float = 0f

	fun scare() {
		startledTime = 2f
		state = STARTLED
	}

	init {
		scene.addChild(skeletonC)

		scene.onTick {
			stepTime ->
			skeletonC.skeleton.x += velocityX * stepTime

			if (state == WALKING) {
				if (flipX && skeletonC.skeleton.x < -4000f) {
					flipX = !flipX
				} else if (!flipX && skeletonC.skeleton.x > 4000f) {
					flipX = !flipX
				}
			}
			if (state == STARTLED) {
				startledTime -= stepTime
				if (startledTime <= 0f) {
					state = WALKING
				}
			}

		}

	}

	private inline fun <T> observable(initialValue: T, crossinline onChange: (newValue: T) -> Unit):
			ReadWriteProperty<Any?, T> {
		val prop = object : ObservableProperty<T>(initialValue) {
			override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(newValue)
		}
		onChange(initialValue)
		return prop
	}

	private val velocityX: Float
		get() = when (state) {
			DEAD, IDLE, STARTLED -> 0f
			WALKING -> 10f
			RUNNING -> 20f
		} * 30f * if (flipX) -1f else 1f

	override val hitZ: Float
		get() = scene.z

	private val tmp = Vector3()

	override fun hitCheck(hitX: Float, hitY: Float): Boolean {
		if (state == DEAD)
			return false
		scene.globalToLocal(tmp.set(hitX, hitY, hitZ))
		val slot = skeletonC.getSlotAtPosition(tmp.x, tmp.y)
		return if (slot != null) {
			state = DEAD
			onHit(hitX, hitY)
			true
		} else {
			false
		}
	}

	private fun onHit(x: Float, y: Float) {
		state = DEAD
		sounds.torture?.createInstance()?.start()
	}

	private enum class PersonState {
		DEAD,
		IDLE,
		STARTLED,
		WALKING,
		RUNNING
	}
}
