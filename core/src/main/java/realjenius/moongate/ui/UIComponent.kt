package realjenius.moongate.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch

interface UIComponent {

  fun input() {}

  fun update() {}

  fun render(camera: OrthographicCamera, batch: SpriteBatch) {}

  fun dispose() {}

}