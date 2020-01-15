package realjenius.moongate

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import realjenius.moongate.converse.Conversations
import realjenius.moongate.screen.Maps
import realjenius.moongate.screen.Palettes
import realjenius.moongate.screen.Tiles

class RedMoongate : ApplicationAdapter() {
  private val MAP_WIDTH = 320
  private val MAP_HEIGHT = 200
  private lateinit var camera: OrthographicCamera
  private lateinit var batch: SpriteBatch
  private lateinit var shapes: ShapeRenderer
  private val viewableMap = ByteArray(320 * 200 * 16)

  private var originX = 0
  private var originY = 0
  override fun create() {
    camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    batch = SpriteBatch()
    shapes = ShapeRenderer()
    Conversations.load()
    Palettes.load()
    Tiles.load()
    Maps.load()
    rebuildMap()
  }

  private fun rebuildMap() {
    val mapRowLength = Maps.getRowWidth(0)
    var viewportIdx = 0
    (0 until MAP_HEIGHT).forEach { y ->
      (0 until MAP_WIDTH).forEach { x ->
        val sourceMapX = (originX + x)// and 1023
        val sourceMapY = (originY + y) //and 1023 // TODO - is this right??
        viewableMap[viewportIdx] = Maps.surfaceMap[sourceMapY * mapRowLength + sourceMapX]
        viewportIdx++
      }
    }
  }

  override fun render() {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    var changed = false
    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) { originX = (originX-1).coerceAtLeast(0); changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) { originX += 1; changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.UP)) { originY = (originY-1).coerceAtLeast(0); changed = true }
    if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) { originY += 1; changed = true }

    if(changed) rebuildMap()

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.begin()
    (0 until MAP_HEIGHT).forEach { y ->
      (0 until MAP_WIDTH).forEach { x ->
        val drawX = x * 16
        val drawY = (MAP_HEIGHT * 16) - y * 16
        val tileIndex = viewableMap[(y * MAP_WIDTH) + x].toUByte().toInt()
        if (tileIndex == 0)
          shapes.rect(drawX.toFloat(), drawY.toFloat(), 16.toFloat(), 16.toFloat(), Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK)
        else {
          if (Tiles.isAnimatedMapTile(tileIndex))
            batch.draw(Tiles.getAnimatedBaseTile(tileIndex).sprite, drawX.toFloat(), drawY.toFloat())
          batch.draw(Tiles.tiles[tileIndex].sprite, drawX.toFloat(), drawY.toFloat())
        }



      }
    }
    batch.end()
  }

  override fun dispose() {
    batch.dispose()
    shapes.dispose()
  }
}
