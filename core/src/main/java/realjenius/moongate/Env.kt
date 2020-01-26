package realjenius.moongate

object Env {
  var testMode: Boolean = false
  private val testEnv: MutableMap<String,String> = hashMapOf("STATICDATA" to "Projects/u6_data")

  operator fun get(key: String) = testEnv[key]

  operator fun set(key: String, value: String) { testEnv[key] = value }
}