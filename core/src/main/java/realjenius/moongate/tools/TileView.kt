package realjenius.moongate.tools

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.gamedata.palette.Palettes
import realjenius.moongate.gamedata.tile.Tiles
import realjenius.moongate.ui.UIComponent

class TileView : UIComponent {

  private var lastSelected = 0
  private var selected = 0
  private var font = BitmapFont().apply {
    this.color = Color.WHITE
  }
  private var shapes = ShapeRenderer()

  override fun input() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) if (selected > 0) selected--
    if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) if (selected < 2047) selected++
    if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) if (selected - 64 >= 0) selected -= 64
    if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) if (selected + 64 <= 2047) selected += 64
  }

  override fun update() {
    if (lastSelected != selected) {
      Gdx.app.log("tileview", "$selected : ${Tiles.tiles[selected].description.forCount(1)}")
      lastSelected = selected
    }
  }

  override fun render(camera: OrthographicCamera, batch: SpriteBatch) {
    shapes.projectionMatrix = camera.combined
    shapes.begin(ShapeRenderer.ShapeType.Line)
    for (index in 0 until 2048) {
      val x = index % 64
      val y = 32 - (index / 64)
      val drawX = 10 + (18 * x)
      val drawY = 1100 - ((18 * 31) - (18 * y))
      batch.draw(Tiles.tiles[index].spriteForPalette(Palettes.currentPalette()), drawX.toFloat(), drawY.toFloat())
      if (selected == index) {
        shapes.rect(drawX-1f, drawY-1f, 18f, 18f, Color.RED, Color.RED, Color.RED, Color.RED)
      }
    }

    shapes.end()
  }
}