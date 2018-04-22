/*
 * Copyright 2017 Nicholas Bilyk
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

import com.acornui.collection.replace
import com.acornui.component.StackLayoutContainer
import com.acornui.component.UiComponent
import com.acornui.component.stage
import com.acornui.core.di.Owned
import com.acornui.core.di.own
import com.acornui.core.focus.focusFirst
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.Ascii
import com.acornui.core.input.keyDown
import com.acornui.core.mvc.commander
import com.acornui.core.mvc.invokeCommand
import com.acornui.core.tween.Tween
import com.acornui.skins.BasicUiSkin
import ld41.command.*
import ld41.model.Ld41Vo
import ld41.model.TargetVo
import ld41.data.targets
import ld41.data.emails
import ld41.data.flirts
import ld41.data.terrors as dTerrors
import ld41.data.initialFlirt
import ld41.model.FlirtVo

/**
 * @author nbilyk
 */
class Ld41(owner: Owned) : StackLayoutContainer(owner) {

	override var focusEnabled: Boolean = true

	private val dataBinding = DataBinding<Ld41Vo>()

	private val cmd = own(commander())

	private var introView: IntroView

	private var emailView: EmailView

	private var huntView: HuntView

	private var flirtView: FlirtView

	private var gameOverView: GameOverView

	private var _currentView: UiComponent? = null
	private var currentView: UiComponent?
		get() = _currentView
		set(value) {
			removeElement(_currentView)
			_currentView = value
			addOptionalElement(value)
			value?.focusFirst()
		}

	private var terrors: List<String> = dTerrors
	private var hasStartedHunt: Boolean = false

	init {
		Tween.prepare() // Make the very first tween smooth.
		BasicUiSkin(stage).apply()

		dataBinding.set(Ld41Vo(
				targets = targets,
				emails = emails,
				flirts = flirts)
		)

		introView = IntroView(this) layout { fill() }

		emailView = EmailView(this).apply {
			this@Ld41.dataBinding.bind {
				emails.set(it.emails)
			}
			emails.bind {
				newEmails ->
				this@Ld41.dataBinding {
					it.copy(emails = newEmails)
				}
			}
		} layout { fill() }

		huntView = HuntView(this).apply {
		} layout { fill() }

		flirtView = FlirtView(this) layout { fill() }

		gameOverView = GameOverView(this) layout { fill() }

		currentView = introView

		cmd.onCommandInvoked(CompleteIntro) {
			flirtView.dataBind.set(Triple(null, initialFlirt, false))
			currentView = flirtView
		}

		cmd.onCommandInvoked(HuntCommand) {
			event ->
			dataBinding {
				it.copy(lastTarget = event.targetId, huntCount = it.huntCount + 1)
			}
			huntView.dataBind.set(getTargetById(event.targetId))
			if (!this.hasStartedHunt)
				this.hasStartedHunt = true
			currentView = huntView
		}

		cmd.onCommandInvoked(SpurnedCommand) {
			dataBinding {
				if (!hasStartedHunt) it else
					it.copy(spurnedCount = it.spurnedCount + 1)
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(KillCommand) {
			event ->
			dataBinding {
				if (event.target != null) {
					val index = getTargetIndexById(event.target.id)
					it.copy(
							targets = it.targets.replace(index, event.target.copy(killed = true)),
							killCount = it.killCount + 1,
							targetKillCount = it.targetKillCount + 1
					)
				}
				else {
					it.copy(
							killCount = it.killCount + 1,
							innocentKillCount = it.innocentKillCount + 1
					)
				}
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(MissCommand) {
			event ->
			dataBinding {
				if (event.target != null) {
					val index = getTargetIndexById(event.target.id)
					it.copy(
							targets = it.targets.replace(index, event.target.copy(attemptedKills = event.target.attemptedKills + 1)),
							whiffCount = it.whiffCount + 1,
							targetWhiffCount = it.targetWhiffCount + 1
					)
				}
				else {
					it.copy(
							whiffCount = it.whiffCount + 1,
							innocentWhiffCount = it.innocentWhiffCount + 1
					)
				}
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(AcquiescedCommand) {
			gameOverView.dataBind.set(true)
			currentView = gameOverView
		}

		cmd.onCommandInvoked(FlirtCommand) {
			val lastTarget = getTargetById(dataBinding.get()!!.lastTarget)
			val flirt = when (lastTarget) {
				null -> if (!hasStartedHunt) initialFlirt else FlirtVo(fBody = popTerror())
				else -> getFlirtByTargetId(lastTarget.id)
			}
			flirtView.dataBind.set(Triple(lastTarget,flirt,isWinner()))
			currentView = flirtView
		}

		cmd.onCommandInvoked(KillCrushCommand) {
			gameOverView.dataBind.set(false)
			currentView = gameOverView
		}

		stage.keyDown().add {
			if ((it.altKey && it.keyCode == Ascii.ENTER) || it.keyCode == Ascii.F11) {
				window.fullScreen = !window.fullScreen
			}
		}

		// TODO: TEMP cheat codes
		keyDown().add {
			if (it.keyCode == Ascii.J && it.ctrlKey) {
				it.preventDefault()
				invokeCommand(HuntCommand("dummy"))
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.M && it.ctrlKey) {
				it.preventDefault()
				invokeCommand(MissCommand(getTargetById("jeff")))
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.M && it.ctrlKey && it.shiftKey) {
				it.preventDefault()
				invokeCommand(MissCommand(getTargetById("joe")))
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.K && it.ctrlKey) {
				it.preventDefault()
				invokeCommand(KillCommand(getTargetById("jeff")))
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.K && it.ctrlKey && it.shiftKey) {
				it.preventDefault()
				invokeCommand(KillCommand(getTargetById("joe")))
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.K && it.ctrlKey && it.shiftKey && it.altKey) {
				it.preventDefault()
				dataBinding {
					it.copy(
							targets = dataBinding.get()!!.targets.map { it.copy(killed = true) }
					)
				}
				invokeCommand(FlirtCommand())
			}
		}
		keyDown().add {
			if (it.keyCode == Ascii.K && it.ctrlKey && it.shiftKey && it.altKey) {
				it.preventDefault()
				dataBinding {
					it.copy(
							targets = dataBinding.get()!!.targets.map { it.copy(killed = true) }
					)
				}
				invokeCommand(FlirtCommand())
			}
		}
	}

	private fun popTerror(): String {
		// TODO: Make more optimized with Random function.  This is O(N)
		val terror = terrors.shuffled().lastOrNull()
		return if (terror != null) terror else {
			terrors = dTerrors
			popTerror()
		}
	}

	private fun getTargetIndexById(targetId: String): Int {
		return dataBinding.get()!!.targets.indexOfFirst { it.id == targetId }
	}

	private fun getTargetById(targetId: String?): TargetVo? {
		return dataBinding.get()!!.targets.firstOrNull { it.id == targetId }
	}

	private fun getFlirtByTargetId(targetId: String): FlirtVo {
		return dataBinding.get()!!.flirts.first { it.targetId == targetId }
	}

	private fun isWinner(): Boolean {
		return dataBinding.get()!!.targets.all { it.killed }
	}
}
