package realjenius.moongate

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import realjenius.moongate.converse.Conversations
import realjenius.moongate.gamedata.obj.GameObjects
import realjenius.moongate.ui.MapViewport
import realjenius.moongate.gamedata.map.Maps
import realjenius.moongate.gamedata.palette.Palettes
import realjenius.moongate.gamedata.tile.Tiles

class RedMoongate : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var batch: SpriteBatch
  private lateinit var mapViewport: MapViewport
  override fun create() {
    camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    batch = SpriteBatch()
    Conversations.load()
    Palettes.load()
    Tiles.load()
    Maps.load()
    GameObjects.load()
    mapViewport = MapViewport(3)
    mapViewport.moveTo(0,0)
  }

  override fun render() {
    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    //Gdx.graphics.setCursor(Gdx.graphics.newCursor(Tiles.usePixmap, 0, 0))

    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) mapViewport.shift(x = -1)
    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) mapViewport.shift(x = 1)
    if (Gdx.input.isKeyPressed(Input.Keys.UP)) mapViewport.shift(y = -1)
    if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) mapViewport.shift(y = 1)
    if (Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) { camera.zoom *= 1.01f }
    if (Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) { camera.zoom *= .99f }

    mapViewport.update()

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.begin()
    mapViewport.render(batch)
    batch.end()
  }

  override fun dispose() {
    Tiles.dispose()
    mapViewport.dispose()
    batch.dispose()
  }
}
