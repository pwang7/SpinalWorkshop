package workshop.stream

import spinal.core._
import spinal.lib._

case class MemoryWrite() extends Bundle {
  val address = UInt(8 bits)
  val data = Bits(32 bits)
}

case class StreamUnit() extends Component {
  val io = new Bundle {
    val memWrite = slave Flow (MemoryWrite())
    val cmdA = slave Stream (UInt(8 bits))
    val cmdB = slave Stream (Bits(32 bits))
    val rsp = master Stream (Bits(32 bits))
  }

  val mem = Mem(Bits(32 bits), 1 << 8)
  //TODO
  when(io.memWrite.valid) {
    mem(io.memWrite.address) := io.memWrite.data
  }

  val readStream = mem.streamReadSync(io.cmdA.m2sPipe().s2mPipe())
  val bufferedStream = io.cmdB.m2sPipe().s2mPipe()
  val (readfork1, readfork2) = StreamFork2(readStream, synchronous = false)
  val (bufferfork1, bufferfork2) =
    StreamFork2(bufferedStream, synchronous = false)
  val joinedStreamWithoutPayload =
    StreamJoin.arg(bufferfork1, bufferfork2, readfork1, readfork2)
  io.rsp <-/< joinedStreamWithoutPayload.translateWith(
    bufferfork2.payload ^ readfork2.payload
  )
}
