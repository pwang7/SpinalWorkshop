package workshop.counter

import spinal.core._

case class Counter(width: Int) extends Component {
  val io = new Bundle {
    val clear = in Bool ()
    val value = out UInt (width bits)
    val full = out Bool ()
  }

  //TODO define the logic
  val cnt = Reg(UInt(width bits)) init (0)
  when(io.clear) {
    cnt := 0
  } otherwise {
    cnt := cnt + 1
  }

  io.value := cnt
  io.full := cnt.andR
}
