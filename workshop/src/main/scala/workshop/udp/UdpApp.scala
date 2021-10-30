package workshop.udp
import spinal.core._
import spinal.lib._
import spinal.lib.fsm.{EntryPoint, StateParallelFsm, State, StateMachine}

case class UdpAppCmd() extends Bundle {
  val ip = Bits(32 bits)
  val srcPort = Bits(16 bits)
  val dstPort = Bits(16 bits)
  val length = UInt(16 bits)
}

case class UdpAppBus() extends Bundle with IMasterSlave {
  val cmd = Stream(UdpAppCmd())
  val data = Stream(Fragment(Bits(8 bits)))

  override def asMaster(): Unit = master(cmd, data)
}

object Hello {
  val discoveringCmd = 0x11
  val discoveringRsp = 0x22
}

case class UdpApp(helloMessage: String, helloPort: Int = 37984)
    extends Component {
  // println(s"helloPort=$helloPort")
  val io = new Bundle {
    val rx = slave(UdpAppBus())
    val tx = master(UdpAppBus())
  }

  // TODO give default value to rx/tx output pins
  io.rx.cmd.ready := False
  io.rx.data.ready := False
  io.tx.cmd.valid := False
  io.tx.cmd.ip := io.rx.cmd.ip
  io.tx.cmd.srcPort := io.rx.cmd.dstPort
  io.tx.cmd.dstPort := io.rx.cmd.srcPort
  io.tx.cmd.length := 0
  io.tx.data.valid := False
  io.tx.data.last := False
  io.tx.data.fragment := 0

  val msg = Reg(Vec(Bits(8 bits), helloMessage.length))
  msg := Vec(helloMessage.map(c => B(c.toInt, 8 bits)))

  val fsm = new StateMachine {
    //Filter rx dst ports
    val idle: State = new State with EntryPoint {
      whenIsActive {
        // TODO Check io.rx.cmd dst port
        when(io.rx.cmd.valid) {
          when(io.rx.cmd.dstPort === helloPort) {
            goto(helloHeader)
          } otherwise {
            io.rx.data.ready := True
            io.rx.cmd.ready := io.rx.data.last
          }
        }
      }
    }

    //Check the hello protocol Header
    val helloHeader = new State {
      whenIsActive {
        // TODO check that the first byte of the packet payload is equals to Hello.discoveringCmd
        io.rx.data.ready := True
        when(io.rx.data.valid && io.rx.data.ready) {
          when(io.rx.data.fragment === Hello.discoveringCmd) {
            goto(discoveringRspTx)
          } elsewhen (io.rx.data.last) {
            io.rx.cmd.ready := True
            goto(idle)
          }
        }
      }
    }

    //Send an discoveringRsp packet
    val discoveringRspTx = new StateParallelFsm(
      discoveringRspTxCmdFsm,
      discoveringRspTxDataFsm
    ) {
      whenCompleted {
        //TODO return to IDLE
        io.rx.cmd.ready := True
        goto(idle)
      }
    }
  }

  //Inner FSM of the discoveringRspTx state
  lazy val discoveringRspTxCmdFsm = new StateMachine {
    val sendCmd = new State with EntryPoint {
      whenIsActive {
        //TODO send one io.tx.cmd transaction
        io.tx.cmd.valid := True
        io.tx.cmd.length := helloMessage.length + 1
        // when (io.tx.cmd.valid && io.tx.cmd.ready) {
        when(io.tx.cmd.ready) {
          exit()
        }
      }
    }
  }

  //Inner FSM of the discoveringRspTx state
  lazy val discoveringRspTxDataFsm = new StateMachine {
    val sendHeader = new State with EntryPoint {
      whenIsActive {
        //TODO send the io.tx.data header (Hello.discoveringRsp)
        io.tx.data.valid := True
        // io.tx.data.last := False
        io.tx.data.fragment := Hello.discoveringRsp
        // when (io.tx.data.valid && io.tx.data.ready) {
        when(io.tx.data.ready) {
          goto(sendMessage)
        }
      }
    }

    val sendMessage = new State {
      val counter = Reg(UInt(log2Up(helloMessage.length) bits))
      onEntry {
        counter := 0
      }
      whenIsActive {
        //TODO send the message on io.tx.data body
        io.tx.data.valid := True
        io.tx.data.last := counter === helloMessage.length - 1
        io.tx.data.fragment := msg(counter)
        when(io.tx.data.valid && io.tx.data.ready) {
          counter := counter + 1
          when(io.tx.data.last) {
            exit()
          }
        }
      }
    }
  }
}
