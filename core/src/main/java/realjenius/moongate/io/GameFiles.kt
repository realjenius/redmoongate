package realjenius.moongate.io

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import realjenius.moongate.CodedError
import realjenius.moongate.Env
import java.io.File
import java.nio.file.Files

object GameFiles {
  const val STATICDATA = "STATICDATA"
  fun loadExternal(path: String) : File {
    return if (Env.testMode) File("${Env[STATICDATA]}/$path")
    else {
      if (!Gdx.files.isExternalStorageAvailable)
        CodedError.ExternalStorageRequired.fail("External Storage Required for Game Data Files")

      Gdx.files.external("${Env[STATICDATA]}/$path").file()
    }
  }

  fun loadInternal(path: String) : FileHandle {
    return Gdx.files.internal(path)
  }
}