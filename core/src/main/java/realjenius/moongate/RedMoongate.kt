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
import realjenius.moongate.tools.TileView
import realjenius.moongate.ui.UIComponent

class RedMoongate : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var batch: SpriteBatch
  private lateinit var root: UIComponent
  private lateinit var tiles: TileView
  private lateinit var map: MapViewport

  private var targetFrameTime = 33333333
  private var lastTick = 0L

  override fun create() {
    camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    batch = SpriteBatch()
    Conversations.load()
    Palettes.load()
    Tiles.load()
    Maps.load()
    GameObjects.load()
    map = MapViewport(0).apply { moveTo(0,0) }
    tiles = TileView()
    root = map
  }

  override fun render() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) root = map
    if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) root = tiles
    if (Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) { camera.zoom *= 1.01f }
    if (Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) { camera.zoom *= .99f }
    //Gdx.graphics.setCursor(Gdx.graphics.newCursor(Tiles.usePixmap, 0, 0))
    root.input()
    if (lastTick > 0L && (System.nanoTime() - lastTick) < targetFrameTime) return

    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    root.update()
    Palettes.update()

    camera.update()
    batch.projectionMatrix = camera.combined
    batch.begin()
    root.render(camera, batch)
    batch.end()
    lastTick = System.nanoTime()
  }

  override fun dispose() {
    Tiles.dispose()
    root.dispose()
    batch.dispose()
  }
}
