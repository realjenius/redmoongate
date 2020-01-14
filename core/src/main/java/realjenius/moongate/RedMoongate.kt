package realjenius.moongate

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.converse.Conversation
import realjenius.moongate.converse.Conversations
import realjenius.moongate.io.GameFiles
import realjenius.moongate.io.LZW
import realjenius.moongate.io.LibraryIO
import realjenius.moongate.io.LibraryType
import realjenius.moongate.screen.Maps
import realjenius.moongate.screen.Palette
import realjenius.moongate.screen.Palettes
import realjenius.moongate.screen.Tiles

class RedMoongate : ApplicationAdapter() {
  private lateinit var camera: OrthographicCamera
  private lateinit var batch: SpriteBatch
  private lateinit var shapes: ShapeRenderer
  private lateinit var tileTextures: List<Texture>
  private var start: Long = 0
  private var chunkStart = 0
  override fun create() {
    camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    batch = SpriteBatch()
    shapes = ShapeRenderer()
    Conversations.load()
    Palettes.load()
    Tiles.load()
    Maps.load()
    tileTextures = Tiles.tiles.map {
      val pixmap = Pixmap(16, 16, Pixmap.Format.RGBA8888)
      (0 until 16).forEach { row ->
        (0 until 16).forEach { col ->
          val value = it.data[row + col*16]
          val color = Palettes.gamePalette.values[value.toUByte().toInt()].let {
            Color.argb8888(it.red, it.green, it.blue, 1.toFloat())
          }
          pixmap.drawPixel(row, col, color)
        }
      }
      Texture(pixmap).apply { pixmap.dispose() }
    }

    start = System.currentTimeMillis()
  }

  override fun render() {
    val now = System.currentTimeMillis()
    if (now - start > 1000) {
      start = now
      chunkStart += 9
    }
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    camera.update()
    batch.projectionMatrix = camera.combined
    shapes.projectionMatrix = camera.combined
    batch.begin()
    (0..8).forEach { chunkIdx ->
      val chunk = Maps.chunks[chunkStart + chunkIdx]
      val chunkX = (chunkIdx % 3) * (16 * 8 + 2)
      val chunkY = (12*8+2) * 3 - (chunkIdx / 3) * (16 * 8 + 2)

      (0 until 8).forEach { y ->
        (0 until 8).forEach { x ->
          val tileIdx = chunk.tiles[x + y * 8]
          val tile = tileTextures[tileIdx.toUByte().toInt()]
          batch.draw(tile, chunkX + (x * 16).toFloat(), chunkY + ((7-y) * 16).toFloat())
        }
      }
    }
    batch.end()

    shapes.begin(ShapeRenderer.ShapeType.Filled)
    renderPalette(400, Palettes.gamePalette)
    Palettes.cutscenePalettes.forEachIndexed { idx, pal ->
      if (idx > 0) renderPalette(440 + (idx * 40), pal)
    }
    shapes.end()
  }

  private fun renderPalette(xOffset: Int, pal: Palette) {
    pal.values.forEachIndexed { index, rgb ->
      val y = index % 100 * 10
      val x = index / 100 * 10 + xOffset
      shapes.setColor(rgb.red, rgb.green, rgb.blue, 1.toFloat())
      shapes.rect(x.toFloat(), y.toFloat(), 10.toFloat(), 10.toFloat())
    }
  }

  override fun dispose() {
    batch.dispose()
    shapes.dispose()
  }
}
