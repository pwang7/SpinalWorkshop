package workshop.udp

import spinal.core.{SpinalConfig, SpinalVhdl}

object UdpAppMain {
  def main(args: Array[String]): Unit = {
    SpinalConfig(targetDirectory = "rtl").generateVerilog(UdpApp("012345"))
  }
}
