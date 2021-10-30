package workshop.function

import spinal.core._
import spinal.lib._

case class FunctionUnit() extends Component {
  val io = new Bundle {
    val cmd = slave Flow (Bits(8 bits))
    val valueA = out Bits (8 bits)
    val valueB = out Bits (32 bits)
    val valueC = out Bits (48 bits)
  }

  // val setCase = Reg(Bits(2 bits)) init(0)
  // val valueRegA = Reg(Bits(8 bits))
  // val valueRegB = Reg(Vec(Bits(8 bits), 4))
  // val valueRegC = Reg(Vec(Bits(8 bits), 6))
  // val prefixCharVec = Vec("setValue".toCharArray().map(c => B(c.toInt & 0xFF, 8 bits)))
  // val cntMax = Reg(UInt(5 bits)) init(1)
  def patternDetector(str: String) = new Area {
    val hit = False
    // TODO
    val strVec = Vec(str.map(c => B(c.toInt, 8 bits)))
    val cnt = Reg(UInt(log2Up(str.length) bits)) init (0)
    when(io.cmd.valid) {
      when(strVec(cnt.resized) === io.cmd.payload) {
        cnt := cnt + 1
        when(cnt === str.length - 1) {
          cnt := 0
          hit := True
        }
      } otherwise {
        cnt := 0
      }
    }
    /*
    val cnt = Reg(UInt(5 bits)) init(0)
    when (io.cmd.valid) {
      when (cnt < 8 && io.cmd.payload === prefixCharVec(cnt.resized)) {
        cnt := cnt + 1
      } elsewhen (cnt === 8) {
        cnt := cnt + 1
        switch (io.cmd.payload) {
          is ('A'.toInt) {
            cntMax := 1 + 9
            setCase := 1
          }
          is ('B'.toInt) {
            cntMax := 4 + 9
            setCase := 2
          }
          is ('C'.toInt) {
            cntMax := 6 + 9
            setCase := 3
          }
          default {
            setCase := 0
            cntMax := 1
          }
        }
      } otherwise {
        switch (setCase) {
          is (1) {
            valueRegA := io.cmd.payload
            when (cnt === cntMax - 1) {
              cnt := 0
              setCase := 0
              hit := True
            }
          }
          is (2) {
            valueRegB((cnt - 9).resized) := io.cmd.payload
            cnt := cnt + 1
            when (cnt === cntMax - 1) {
              cnt := 0
              setCase := 0
              hit := True
            }
          }
          is (3) {
            valueRegC((cnt - 9).resized) := io.cmd.payload
            cnt := cnt + 1
            when (cnt === cntMax - 1) {
              cnt := 0
              setCase := 0
              hit := True
            }
          }
          default {
            cnt := 0
            setCase := 0
          }
        }
      }
    }
     */
  }

  def valueLoader(start: Bool, that: Data) = new Area {
    require(
      widthOf(that) % widthOf(io.cmd.payload) == 0
    ) //You can make the assumption that the 'that' width is always an mulitple of 8
    // TODO
    val vecLength = widthOf(that) / widthOf(io.cmd.payload)
    val buffer = Reg(Vec(Bits(8 bits), vecLength))
    val cnt = Reg(UInt(log2Up(widthOf(that)) bits))
    val writeToBuf = RegInit(False)
    when(start) {
      writeToBuf := True
      cnt := 0
    } elsewhen (io.cmd.valid && writeToBuf) {
      buffer(cnt.resized) := io.cmd.payload
      cnt := cnt + 1
      writeToBuf := True
      when(cnt === vecLength - 1) {
        writeToBuf := False
      }
    }
    that.assignFromBits(buffer.asBits)
  }

  val setA = patternDetector("setValueA")
  val loadA = valueLoader(setA.hit, io.valueA)
  // io.valueA := valueRegA

  val setB = patternDetector("setValueB")
  val loadB = valueLoader(setB.hit, io.valueB)
  // io.valueB := valueRegB.asBits

  val setC = patternDetector("setValueC")
  val loadC = valueLoader(setC.hit, io.valueC)
  // io.valueC := valueRegC.asBits
}
