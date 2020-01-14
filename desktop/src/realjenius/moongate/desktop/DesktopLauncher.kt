@file:JvmName("DesktopLauncher")
package realjenius.moongate.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import realjenius.moongate.RedMoongate

fun main() {
  val config = LwjglApplicationConfiguration()
  LwjglApplication(RedMoongate(), config)
}