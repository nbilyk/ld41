package ld41.jvm

import com.acornui.component.stage
import com.acornui.core.AppConfig
import com.acornui.core.GlConfig
import com.acornui.core.WindowConfig
import com.acornui.jvm.LwjglApplication
import com.acornui.jvm.restartJvm
import ld41.Ld41

fun main(args: Array<String>) {
	if (restartJvm()) return
	LwjglApplication().start(AppConfig(frameRate = 60, window = WindowConfig(title = "Ld41"), gl = GlConfig(antialias = true))) {
		stage.addElement(Ld41(this))
	}
}