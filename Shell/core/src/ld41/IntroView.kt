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

import com.acornui.component.button
import com.acornui.component.layout.algorithm.VerticalLayoutContainer
import com.acornui.component.text.text
import com.acornui.core.di.Owned
import com.acornui.core.input.interaction.click
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.mvc.invokeCommand

class IntroView(owned: Owned) : VerticalLayoutContainer(owned) {

	init {
		+text("Intro")

		+button("Start") {
			click().add {
				invokeCommand(CompleteIntro)
			}
		}
	}

}

object CompleteIntro : Command, CommandType<CompleteIntro> {
	override val type: CommandType<out Command> = this
}