package workshop.uart

import spinal.core.{SpinalConfig, SpinalVhdl}

object UartCtrlRxMain {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(
      UartCtrlRx(UartRxGenerics(1, 5, 2))
    )
  }
}
