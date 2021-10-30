package workshop.blackboxAndClock

import spinal.core._
import spinal.lib._

// Define a Ram as a BlackBox
case class Ram_1w_1r_2c(
    wordWidth: Int,
    addressWidth: Int,
    writeClock: ClockDomain,
    readClock: ClockDomain
) extends BlackBox {
  // TODO define Generics
  val generic = new Generic {
    val wordWidth = Ram_1w_1r_2c.this.wordWidth
    val addressWidth = Ram_1w_1r_2c.this.addressWidth
  }

  // TODO define IO
  val io = new Bundle {
    val wr = new Bundle {
      val clk = in Bool ()
      val en = in Bool ()
      val addr = in UInt (addressWidth bits)
      val data = in Bits (wordWidth bits)
    }
    val rd = new Bundle {
      val clk = in Bool ()
      val en = in Bool ()
      val addr = in UInt (addressWidth bits)
      val data = out Bits (wordWidth bits)
    }
  }

  // TODO define ClockDomains mappings
  mapClockDomain(writeClock, io.wr.clk)
  mapClockDomain(readClock, io.rd.clk)
}

// Create the top level and instanciate the Ram
case class MemorySumming(writeClock: ClockDomain, sumClock: ClockDomain)
    extends Component {
  val io = new Bundle {
    val wr = new Bundle {
      val en = in Bool ()
      val addr = in UInt (8 bits)
      val data = in Bits (16 bits)
    }

    val sum = new Bundle {
      val start = in Bool ()
      val done = out Bool ()
      val value = out UInt (16 bits)
    }
  }

  // TODO define the ram
  val ram = Ram_1w_1r_2c(
    wordWidth = 16,
    addressWidth = 8,
    writeClock = writeClock,
    readClock = sumClock
  )

  // TODO connect the io.wr port to the ram
  ram.io.wr.assignSomeByName(io.wr)
  when(io.wr.en) {
    report(L"write to ram with addr=${io.wr.addr}, data=${io.wr.data}")
  }

  val sumArea = new ClockingArea(sumClock) {
    // TODO define the memory read + summing logic
    val cnt = Reg(UInt(8 bits))
    val ramSum = Reg(UInt(16 bits))
    val start = Reg(Bool()) init (False)
    ram.io.rd.en := start
    ram.io.rd.addr := cnt

    when(io.sum.start.rise(initAt = False)) {
      start := True
      ramSum := 0
      cnt := 0
    }

    val compute = RegNext(start) init (False)
    when(compute) {
      ramSum := ramSum + ram.io.rd.data.asUInt
      //report(L"read addr=${ram.io.rd.addr}, data=${ram.io.rd.data}, sum=${ramSum}")
    }
    when(start) {
      cnt := cnt + 1
      when(cnt === cnt.maxValue) {
        start := False
      }
    }

    io.sum.done := compute.fall(initAt = False)
    io.sum.value := ramSum
  }
}
