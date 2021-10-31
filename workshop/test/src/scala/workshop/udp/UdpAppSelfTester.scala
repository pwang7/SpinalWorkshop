package workshop.udp

import org.scalatest.funsuite.AnyFunSuite
import spinal.core._
import workshop.common.CocotbRunner

//Run this scala test to generate and check that your RTL work correctly
class UdpAppSelfTester extends AnyFunSuite {
  test("test") {
    SpinalConfig(targetDirectory = "rtl")
      .dumpWave(0, "../../../../../../../waves/UdpAppSelfTester.vcd")
      .generateVerilog(
        UdpApp("test1234")
      )

    assert(
      CocotbRunner("./workshop/test/src/python/workshop/udp/selftested"),
      "Simulation faild"
    )
    println("SUCCESS")
  }
}
