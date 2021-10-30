package workshop.function

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import spinal.core.sim._
import workshop.common.WorkshopSimConfig

import scala.util.Random

//Run this scala test to generate and check that your RTL work correctly
class FunctionUnitTester extends AnyFunSuite {
  var compiled: SimCompiled[FunctionUnit] = null

  test("compile") {
    compiled = WorkshopSimConfig().compile(FunctionUnit())
  }

  test("testbench") {
    compiled.doSim(seed = 42) { dut =>
      dut.clockDomain.forkStimulus(10)
      dut.io.cmd.valid #= false

      def sendWithRandomTiming(value: BigInt) = {
        dut.clockDomain.waitSampling(Random.nextInt(4))
        dut.io.cmd.valid #= true
        dut.io.cmd.payload #= value
        dut.clockDomain.waitSampling()
        dut.io.cmd.valid #= false
        dut.io.cmd.payload.randomize()
      }

      def driveAndCheck(header: String, value: BigInt, pin: Bits) = {
        for (char <- header) {
          sendWithRandomTiming(char.toInt & 0xff)
        }
        for (byteId <- 0 until widthOf(pin) / 8) {
          sendWithRandomTiming((value >> byteId * 8) & 0xff)
        }
        dut.clockDomain.waitSampling(8)
        assert(
          pin.toBigInt === value,
          s"${pin.getName()} wasn't loaded correctly"
        )
      }

      driveAndCheck("setValueA", 0x11L, dut.io.valueA)
      driveAndCheck("setValueB", 0x22334455L, dut.io.valueB)
      driveAndCheck("setValueC", 0x66778899aabbL, dut.io.valueC)
      driveAndCheck("setValueB", 0xcafef00dL, dut.io.valueB)
    }
  }
}
