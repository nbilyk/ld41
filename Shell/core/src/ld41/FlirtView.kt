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
import com.acornui.core.immutable.DataBinding
import com.acornui.core.input.interaction.click
import com.acornui.core.mvc.Command
import com.acornui.core.mvc.CommandType
import com.acornui.core.mvc.invokeCommand
import ld41.model.FlirtVo
import ld41.model.TargetVo

class FlirtView(owned: Owned) : VerticalLayoutContainer(owned) {

	val dataBind = DataBinding<Triple<TargetVo?, FlirtVo, Boolean>>()

	init {

		+text("Flirt")

		+text{
			dataBind.bind {
				val (target, flirt, _) = it
				text = if (target?.killed == true) flirt.sBody!! else flirt.fBody!!
			}
		}

		+button("Spurned") {
			dataBind.bind {
				val hasWon = it.third
				visible = hasWon
			}
			click().add {
				invokeCommand(SpurnedCommand)
			}
		}

		+button("Loved") {
			dataBind.bind {
				// TODO: Work off of victory conditions.
				val hasWon = it.third
				visible = hasWon
			}
			click().add {
				invokeCommand(AcquiescedCommand)
			}
		}
	}

}


object SpurnedCommand : Command, CommandType<SpurnedCommand> {
	override val type: CommandType<out Command> = this
}

object AcquiescedCommand : Command, CommandType<AcquiescedCommand> {
	override val type: CommandType<out Command> = this
}