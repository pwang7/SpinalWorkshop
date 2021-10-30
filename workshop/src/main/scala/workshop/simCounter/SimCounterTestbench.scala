package workshop.simCounter

import spinal.sim._
import spinal.core._
import spinal.core.sim._

object SimCounterTestbench {
  def main(args: Array[String]): Unit = {
    //Compile the simulator
    val compiled = SimConfig.withWave.compile(new SimCounterDut)

    //Run the simulation
    compiled.doSim { dut =>
      // TODO Fork a thread to generate the reset and the clock with a period of 10 units of time
      dut.clockDomain.forkStimulus(period = 10)

      val ranGen = new scala.util.Random()
      var counterModel = 0
      for (_ <- 0 until 1000) {
        //TODO Randomize Dut inputs
        val enable = ranGen.nextBoolean()
        dut.io.enable #= enable

        //TODO Wait until the dut.clockDomain will make the counter register sample its input
        dut.clockDomain.waitSampling()

        //Check that the dut.io.value is matching the expected value.
        assert(
          dut.io.value.toInt == counterModel,
          s"dut.io.value mismatch. io.value=${dut.io.value.toInt} counterModel=$counterModel"
        )

        //TODO Update the counterModel
        if (enable) {
          counterModel = (counterModel + 1) % (scala.math
            .pow(2, widthOf(dut.io.value))
            .toInt)
        }
      }
    }
  }
}
