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
import com.acornui.core.focus.Focusable
import com.acornui.core.focus.focusFirst
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.Ascii
import com.acornui.core.input.keyDown
import com.acornui.core.mvc.commander
import com.acornui.core.mvc.invokeCommand
import com.acornui.core.tween.Tween
import com.acornui.skins.BasicUiSkin
import ld41.command.HuntCommand
import ld41.model.EmailVo
import ld41.model.Ld41Vo
import ld41.model.TargetVo
import ld41.data.targets
import ld41.data.emails

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

	private var victoryView: VictoryView

	private var _currentView: UiComponent? = null
	private var currentView: UiComponent?
		get() = _currentView
		set(value) {
			removeElement(_currentView)
			_currentView = value
			addOptionalElement(value)
			value?.focusFirst()
		}

	init {
		Tween.prepare() // Make the very first tween smooth.
		BasicUiSkin(stage).apply()

		dataBinding.set(Ld41Vo(
				targets = targets,
				emails = emails)
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

		victoryView = VictoryView(this) layout { fill() }

		currentView = introView

		cmd.onCommandInvoked(CompleteIntro) {
			currentView = flirtView
		}

		cmd.onCommandInvoked(HuntCommand) {
			huntView.dataBind.set(getTargetById(it.targetId))
			currentView = huntView
		}

		cmd.onCommandInvoked(SpurnedCommand) {
			dataBinding {
				it.copy(spurnedCount = it.spurnedCount + 1)
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(KillCommand) {
			event ->
			dataBinding {
				val index = getTargetIndexById(event.target.id)
				it.copy(targets = it.targets.replace(index, event.target.copy(killed = true)))
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(MissCommand) {
			event ->
			dataBinding {
				val index = getTargetIndexById(event.target.id)
				it.copy(targets = it.targets.replace(index, event.target.copy(attemptedKills = event.target.attemptedKills + 1)))
			}
			currentView = emailView
		}

		cmd.onCommandInvoked(AcquiescedCommand) {
			currentView = victoryView
		}

		cmd.onCommandInvoked(FlirtCommand) {
			currentView = flirtView
		}

		stage.keyDown().add {
			if ((it.altKey && it.keyCode == Ascii.ENTER) || it.keyCode == Ascii.F11) {
				window.fullScreen = !window.fullScreen
			}
		}

		// TODO: TEMP cheat codes
		keyDown().add {
			if (it.keyCode == Ascii.J && it.ctrlKey) {
				invokeCommand(HuntCommand("joe"))
			}
		}
	}

	private fun getTargetIndexById(targetId: String): Int {
		return dataBinding.get()!!.targets.indexOfFirst { it.id == targetId }
	}

	private fun getTargetById(targetId: String): TargetVo {
		return dataBinding.get()!!.targets.first { it.id == targetId }
	}

}
