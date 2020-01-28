package realjenius.moongate.io

import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import realjenius.moongate.CodedError
import realjenius.moongate.lsbThreeByte
import java.io.File

private const val MAX_WORD_LEN = 12
private const val STARTING_WORD_LEN = 9
private const val REINIT_MARKER = 0x100U
private const val END_MARKER = 0x101U
private const val STARTING_NEXT_WORD = 0x102U
private const val STARTING_DICT_SIZE = 0x200U

object LZW {
  fun decompress(file: File) = file.source().buffer().use {
    decompress(it)
  }

  fun decompressInto(file: File, target: Buffer) = file.source().buffer().use {
    decompressInto(it, target)
  }

  fun decompress(buffer: BufferedSource) = Buffer().apply { decompressInto(buffer, this) }.readByteArray()

  fun decompressInto(buffer: BufferedSource, target: Buffer) {
    val uncompressedSize = buffer.readIntLe()
    if (uncompressedSize == 0) {
      target.writeAll(buffer)
      return
    }
    val state = DecompressState(source = buffer, target = target)
    while (!buffer.exhausted()) {
      var codeword = readCodeword(state)
      if (codeword == REINIT_MARKER) {
        state.reinit()
        codeword = readCodeword(state)
        state.addByte(codeword)
      } else if (codeword == END_MARKER) break
      else {
        state.addSegment(
            if (codeword < state.nextWord) readSegment(codeword, state.segments)
            else {
              if (codeword != state.nextWord)
                CodedError.LzwParseError.fail("Unable to read Next Free Word: $codeword != ${state.nextWord}")
              readSegment(state.previousCodeword, state.segments, true)
            }
        )
      }
      state.advanceCodeword(codeword)
    }
  }

  private fun readCodeword(state: DecompressState) : UInt {
    val isThreeByte = state.isThreeByteCodewordPosition()
    if (!state.checkBuffer(if (isThreeByte) 3 else 2))
      CodedError.LzwBufferUnderflow.fail("Unable to read any more bytes from buffer: ${state.source.buffer.size} - Current Text: ${state.target}")

    val part1 = state.readByte()
    val part2 = state.readByte(1)
    val part3 = if (isThreeByte) state.readByte(2) else 0U

    var codewordBytes = lsbThreeByte(part1.toInt(), part2.toInt(), part3.toInt()).toUInt()
    codewordBytes = codewordBytes shr (state.bitsRead % 8)
    val result = codewordBytes and when (state.codewordSize) {
      0x9 -> 0x1ffU
      0xa -> 0x3ffU
      0xb -> 0x7ffU
      0xc -> 0xfffU
      else -> CodedError.LzwInvalidCodewordSize.fail("Unable to decompress. Invalid codeword size")
    }
    state.advanceBitsRead()
    return result
  }

  private fun readSegment(codeword: UInt, segments: Map<UInt, Segment>, addLast: Boolean = false) : ByteArray {
    var currCodeword = codeword
    val buffer = Buffer()
    while (currCodeword > 0xffU) {
      val seg = segments[currCodeword]
          ?: CodedError.LzwParseError.fail("Unable to decompress, no entry for $currCodeword found")

      buffer.writeByte(seg.root.toInt())
      currCodeword = seg.codeword
    }
    buffer.writeByte(currCodeword.toUByte().toInt())

    return ByteArray(buffer.size.toInt() + if(addLast) 1 else 0).apply {
      (0 until buffer.size.toInt()).forEach { this[it] = buffer[buffer.size-1-it] }
      if (addLast) this[this.size-1] = buffer[buffer.size-1]
    }
  }

  private data class Segment(val root: UByte, val codeword: UInt)

  private data class DecompressState(
      var source: BufferedSource,
      var target: Buffer = Buffer(),
      var bitsRead: Int = 0,
      var codewordSize: Int = STARTING_WORD_LEN,
      var nextWord: UInt = STARTING_NEXT_WORD,
      var dictSize: UInt = STARTING_DICT_SIZE,
      var previousCodeword: UInt = 0U,
      var segments: MutableMap<UInt,Segment> = hashMapOf()
  ) {

    fun reinit() {
      codewordSize = STARTING_WORD_LEN
      nextWord = STARTING_NEXT_WORD
      dictSize = STARTING_DICT_SIZE
      segments.clear()
    }

    fun advanceBitsRead() { bitsRead += codewordSize }

    fun advanceCodeword(codeword: UInt) { previousCodeword = codeword }

    fun isThreeByteCodewordPosition() = codewordSize + (bitsRead % 8) > 16

    fun readByte(offset: Int = 0) = source.buffer[(bitsRead / 8 + offset).toLong()].toUByte().toUInt()

    fun addByte(value: UInt) = target.writeByte(value.toInt())

    fun addSegment(bytes: ByteArray) {
      val root = bytes[0]
      segments[nextWord] = Segment(root.toUByte(), previousCodeword)
      nextWord++
      if (nextWord >= dictSize && codewordSize < MAX_WORD_LEN) {
        codewordSize++
        dictSize *= 2U
      }
      target.write(bytes)
    }

    fun checkBuffer(size: Int) = source.request(((bitsRead / 8) + size).toLong())
  }
}
