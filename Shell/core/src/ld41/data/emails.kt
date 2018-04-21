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

import ld41.model.EmailVo

val emails: List<EmailVo> = listOf(
		EmailVo(
				"notifications@MyFacey.com",
				"Notification - Your relationship status has changed!",
				"""
					|Waitress,
					|Your relationship has changed from In a Relationship to Single.  You're free!
					|
					|If this action was not performed by you, please report to support@MyFacey.com
					|""".trimMargin(),
				null
		),
		EmailVo(
				"tracyBear38@hotsmailin.com",
				"Sngl?! Party Times!",
				"""
					|hA babe!  jst saw yor myfacey relationship status chAng frm n a relationship 2 single!
					|  It's pRT time!
					|
					|MEt me @ Jeff's plAc 2nt aftR wrk.  He's throwin a pRT & ther wiL b a bunch of hawt guys there!!!
					|+ I caught him checking U out, workin dat expresso machine.  Did U c Hs nu tat?!  Tribal tats R so sxy!!!!
					|addy:  1234 Ash Rd
					|""".trimMargin(),
				"jeff"
		)
)