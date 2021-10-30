package workshop.stream

import spinal.core._

object StreamUnitMain {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(StreamUnit())
  }
}
