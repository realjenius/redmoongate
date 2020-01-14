package realjenius.moongate

enum class CodedError(val code: Int) {
  LzwParseError(101),
  LzwInvalidCodewordSize(102),
  LzwBufferUnderflow(103),
  ExternalStorageRequired(201),
  InvalidConversationFormat(301);


  fun fail(message: String) : Nothing = throw RuntimeException("Error: $code - $message")
}