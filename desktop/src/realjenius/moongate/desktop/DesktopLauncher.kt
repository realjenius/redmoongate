@file:JvmName("DesktopLauncher")
package realjenius.moongate.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import realjenius.moongate.RedMoongate

fun main() {
  val config = LwjglApplicationConfiguration()
  config.width = 1536
  config.height = 1152
  LwjglApplication(RedMoongate(), config)
}