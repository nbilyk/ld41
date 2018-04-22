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

package ld41.data

import ld41.model.FlirtVo

val flirts: List<FlirtVo> = listOf(
		FlirtVo(
				"You successfully killed your target!  You rock!  This is your positive flirtation experience!",
				"You failed at killing your target!  You suck!  This is your negative flirtation experience!",
				"jeff"
		),
		FlirtVo(
				"Successful kill!",
				"Whiffed kill!",
				"sam"

		),
		FlirtVo(
				"Successful kill!",
				"Whiffed kill!",
				"richie"

		),
		FlirtVo(
				"Successful kill!",
				"Whiffed kill!",
				"dummy"

		)
)

val initialFlirt: FlirtVo = FlirtVo(
		fBody = "This is your first flirt encounter with Waitress.  It's an auto-spurn."
)