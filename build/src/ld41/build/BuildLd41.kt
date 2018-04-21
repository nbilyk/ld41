package ld41.build

import com.acornui.build.*
import java.io.File
import java.util.*

fun main(args: Array<String>) {
	val allModules = ArrayList(ALL_ACORNUI_MODULES)
	allModules.addAll(arrayListOf(Ld41Core, Ld41Js, Ld41Jvm))
	BuildUtil.execute(allModules, args)
}


object Ld41Core : CoreModule(File("Shell/core"), name = "Ld41Core") {
	init {
		//resources += skin("basic")
		moduleDependencies = arrayListOf(AcornUtils, AcornUiCore, AcornGame, AcornSpine)
	}
}

object Ld41Js : JsModule(File("Shell/js"), name = "Ld41Js") {
	init {
		//minimize = false
		moduleDependencies = arrayListOf(Ld41Core, AcornUtils, AcornUiCore, AcornUiJsBackend)
	}
}

object Ld41Jvm : JvmModule(File("Shell/jvm"), name = "Ld41Jvm") {
	init {
		mainClass = "ld41.jvm.Ld41JvmKt"
		moduleDependencies = arrayListOf(Ld41Core, AcornUtils, AcornUiCore, AcornUiLwjglBackend)
	}
}