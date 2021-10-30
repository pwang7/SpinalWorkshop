package workshop.timer

import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.BusSlaveFactory

case class Timer(width: Int) extends Component {
  val io = new Bundle {
    val tick = in Bool ()
    val clear = in Bool ()
    val limit = in UInt (width bits)

    val full = out Bool ()
    val value = out UInt (width bits)
  }

  //TODO phase 1
  val cntReg = RegInit(U(0, width bits))
  when(io.clear) {
    cntReg := 0
  } elsewhen (io.tick) {
    when(cntReg < io.limit) {
      cntReg := cntReg + 1
    }
  }
  io.full := cntReg === io.limit
  io.value := cntReg

  def driveFrom(
      busCtrl: BusSlaveFactory,
      baseAddress: BigInt
  )(ticks: Seq[Bool], clears: Seq[Bool]) = new Area {
    //TODO phase 2
    val ticksEnable = busCtrl.createReadWrite(
      dataType = Bits(ticks.length bits),
      address = baseAddress + 0x0,
      bitOffset = 0
    ) init (0)
    val clearsEnable = busCtrl.createReadWrite(
      dataType = Bits(clears.length bits),
      address = baseAddress + 0x0,
      bitOffset = 16
    ) init (0)

    io.clear := (clearsEnable & clears.asBits).orR || busCtrl
      .isWriting(address = baseAddress + 0x8) || busCtrl.isWriting(address =
      baseAddress + 0x4
    )
    io.tick := (ticksEnable & ticks.asBits).orR

    val cntLimit = busCtrl.createReadWrite(
      dataType = UInt(width bits),
      address = baseAddress + 0x4,
      bitOffset = 0
    ) init (0)
    io.limit := cntLimit

    busCtrl.read(io.value, address = baseAddress + 0x8, bitOffset = 0)
  }
}
