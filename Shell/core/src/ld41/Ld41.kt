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

import com.acornui.component.StackLayoutContainer
import com.acornui.component.scroll.ScrollPolicy
import com.acornui.component.scroll.scrollArea
import com.acornui.component.stage
import com.acornui.component.text.TextArea
import com.acornui.component.text.textArea
import com.acornui.core.di.Owned
import com.acornui.core.nav.NavBindable
import com.acornui.math.Pad
import com.acornui.skins.BasicUiSkin

/**
 * @author nbilyk
 */
class Ld41(owner: Owned) : StackLayoutContainer(owner), NavBindable {

	private lateinit var mainText: TextArea

	init {
		BasicUiSkin(stage).apply()

		+scrollArea {
//			+vGroup {
//				style.gap = 0f
//
//				+hGroup {
//					style.padding = Pad(4f)
//					radioGroup<FlowHAlign> {
//						+radioButton(FlowHAlign.LEFT) {
//							label = "Left"
//						}
//						+radioButton(FlowHAlign.CENTER) {
//							label = "Center"
//						}
//						+radioButton(FlowHAlign.RIGHT) {
//							label = "Right"
//						}
//						+radioButton(FlowHAlign.JUSTIFY) {
//							label = "Justify"
//						}
//						selectedData = FlowHAlign.LEFT
//						changed.add {
//							old, new ->
//							mainText.flowStyle.horizontalAlign = new?.data ?: FlowHAlign.LEFT
//						}
//					}
//				}
//
//				+scrollArea {

					mainText = +textArea {

						allowTab = true
						flowStyle.padding = Pad(20f)
//						flowStyle.multiline = false
						hScrollPolicy = ScrollPolicy.AUTO

						text = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus magna nulla, commodo non nisl vel, congue gravida turpis. Mauris sed ultrices purus. Duis gravida sapien in faucibus malesuada. Etiam in hendrerit quam, non porta diam. Vestibulum sit amet congue felis. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Proin tempus velit sed neque lobortis iaculis. Etiam cursus leo vitae massa finibus finibus. Donec semper nunc non neque venenatis, vitae dignissim nibh luctus. Suspendisse maximus risus sit amet pretium tempor. Nullam nec enim vitae leo ultrices congue.

Suspendisse et purus euismod, pulvinar quam vel, consequat dui. Aliquam luctus neque sit amet tortor varius rutrum. Sed sollicitudin condimentum massa, ut scelerisque magna fermentum lobortis. Nam rutrum bibendum purus, quis luctus nunc tincidunt nec. Ut non velit fringilla, maximus metus a, ultricies libero. Duis felis nisl, pulvinar eu felis non, pellentesque semper velit. Donec semper massa ipsum, quis finibus eros congue non. Proin ultricies erat eget arcu scelerisque, pellentesque gravida lectus facilisis.

Cras porttitor tincidunt pharetra. Nulla sollicitudin sollicitudin fringilla. Donec volutpat aliquet sem. Etiam non felis lorem. Aenean auctor placerat ipsum, non dignissim mauris porttitor vitae. Vestibulum enim dolor, dictum nec volutpat ac, dapibus eget mi. Ut laoreet, sem quis cursus tristique, erat nulla sodales ex, a feugiat enim nisl eu enim. Praesent justo libero, sollicitudin at ligula porta, lacinia volutpat odio. Etiam nunc augue, lacinia et lacinia et, molestie eget elit. In nec pharetra mauris, ac semper erat. Sed at elit id urna egestas convallis sed vel lectus. Pellentesque bibendum nulla lorem, at aliquam ex pulvinar non. Fusce pulvinar ultricies mauris, semper feugiat lacus. Mauris hendrerit vel dolor sit amet lobortis. Donec rhoncus nisi posuere mi auctor porttitor.

Nullam ante urna, pulvinar id fringilla et, vehicula ac lacus. Praesent tempus ex vitae massa auctor, vitae vestibulum nunc imperdiet. Quisque dignissim molestie turpis varius convallis. Maecenas sagittis dui purus, vitae cursus massa ornare eget. Nulla facilisi. Phasellus interdum vel ipsum non feugiat. In pharetra erat vitae maximus hendrerit. Nullam in urna quis nisi viverra commodo. Maecenas ligula justo, tristique at dolor in, sagittis euismod arcu. In aliquam lectus imperdiet turpis gravida, in mollis nunc consequat. Maecenas posuere porta leo eget pellentesque. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. In malesuada ultricies tortor at feugiat. Nunc quis arcu ut ante blandit varius malesuada in neque. Etiam malesuada purus vel nisl ornare suscipit. Etiam tellus libero, tempus quis lacus ut, rhoncus varius ligula.

Cras sagittis, felis et fermentum malesuada, sapien justo tincidunt tortor, eget lobortis turpis magna et augue. Curabitur vitae lacus a velit elementum gravida. In rutrum orci vitae metus condimentum sagittis. Praesent et orci vitae mi dapibus feugiat quis id purus. Sed semper congue risus, in tempor ligula elementum eu. Nunc sagittis velit non tincidunt sollicitudin. Vestibulum id blandit massa."""
					} layout { widthPercent = 1f; heightPercent = 1f }
//				} layout { fill() }
//
//			} layout { fill() }
		} layout { fill() }

//		+rect {
//			style.backgroundColor = Color.RED
//			setOrigin(-100f, -200f)
//			defaultWidth = 100f
//			defaultHeight = 50f
//			cursor(StandardCursors.HAND)
//			click().add {
//				println("Clicked")
//			}
//		}

	}
}
