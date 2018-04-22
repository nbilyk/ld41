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

package ld41.model

data class Ld41Vo(
		val targets: List<TargetVo>,
		val emails: List<EmailVo>,
		val flirts: List<FlirtVo>,
		// Keeps track of the last hunted target and the huntCount at that time.
		val lastTarget: String? = null,
		val spurnedCount: Int = 0,
		val huntCount: Int = 0,
		val killCount: Int = 0,
		val targetKillCount: Int = 0,
		val innocentKillCount: Int = 0,
		val whiffCount: Int = 0,
		val targetWhiffCount: Int = 0,
		val innocentWhiffCount: Int = 0
)