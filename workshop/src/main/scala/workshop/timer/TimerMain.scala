package workshop.timer

import spinal.core._
import workshop.uart.{UartCtrlRx, UartRxGenerics}

object TimerMain {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(Timer(width = 16))
  }
}
