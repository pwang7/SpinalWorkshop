package workshop.mandelbrot

import spinal.core._
import spinal.lib._

// case class PixelSolverGenerics(
//     fixAmplitude: Int,
//     fixResolution: Int,
//     iterationLimit: Int
// ) {
//   val iterationWidth = log2Up(iterationLimit + 1)
//   def iterationType = UInt(iterationWidth bits)
//   def fixType = SFix(
//     peak = fixAmplitude exp,
//     resolution = fixResolution exp
//   )
// }

// case class PixelTask(g: PixelSolverGenerics) extends Bundle {
//   val x, y = g.fixType
// }

// case class PixelResult(g: PixelSolverGenerics) extends Bundle {
//   val iteration = g.iterationType
// }

case class PixelSolverPipeline(g: PixelSolverGenerics) extends Component {
  val io = new Bundle {
    val cmd = slave Stream (PixelTask(g))
    val rsp = master Stream (PixelResult(g))
  }

  import g._

  val idWidth = 3
  class Context extends Bundle {
    val id = UInt(idWidth bits)
    val x0, y0 = fixType
    val iteration = UInt(iterationWidth bits)
    val done = Bool
  }

  case class InserterContext() extends Context {
    val x, y = fixType
  }

  case class MulStageContext() extends Context {
    val xx, yy, xy = fixType
  }

  case class AddStageContext() extends Context {
    val x, y = fixType
  }

  case class RouterContext() extends Context {
    val x, y = fixType
  }

  val inserter = new Area {
    val loopback = Stream(RouterContext())
    val freeId = Counter(1 << idWidth, inc = io.cmd.fire)
    val cmdContext = InserterContext()
    cmdContext.id := freeId
    cmdContext.x0 := io.cmd.x
    cmdContext.y0 := io.cmd.y
    cmdContext.x := 0.0
    cmdContext.y := 0.0
    cmdContext.iteration := 0
    cmdContext.done := False

    // loopback.translateWith {
    //   val insert = InserterContext()
    //   insert.assignAllByName(loopback.payload)
    //   insert
    // },
    val input0 = loopback.transmuteWith(InserterContext())
    val input1 = io.cmd.translateWith(cmdContext)
    val output = StreamArbiterFactory.lowerFirst.onArgs(input0, input1)
    // input0.ready := False
    // input1.ready := False
    // val output = Stream(InserterContext())
    // when (input0.valid) {
    //   output << input0
    // } otherwise {
    //   output << input1
    //   // output.valid := input1.valid
    //   // output.payload := input1.payload
    //   // input1.ready := output.ready
    // }
  }

  val mulStage = new Area {
    val input = inserter.output.m2sPipe().s2mPipe()
    val output = input.translateWith {
      val ins = input.payload
      val mul = MulStageContext()
      mul.assignSomeByName(ins)
      mul.xx := (ins.x * ins.x).truncated
      mul.yy := (ins.y * ins.y).truncated
      mul.xy := (ins.x * ins.y).truncated
      mul
    }
    // val output = Stream(MulStageContext())
    // output.valid := input.valid
    // output.payload.assignSomeByName(input.payload)
    // output.xx := (input.x * input.x).truncated
    // output.yy := (input.y * input.y).truncated
    // output.xy := (input.x * input.y).truncated
    // input.ready := output.ready
  }

  val addStage = new Area {
    val input = mulStage.output.m2sPipe().m2sPipe().s2mPipe()
    val output = input.translateWith {
      val mul = input.payload
      val add = AddStageContext()
      add.assignSomeByName(mul)
      add.x := (mul.xx - mul.yy + mul.x0).truncated
      add.y := (((mul.xy) << 1) + mul.y0).truncated
      add.done.allowOverride
      add.iteration.allowOverride
      add.done := mul.done || mul.xx + mul.yy >= 4.0 || mul.iteration === iterationLimit
      add.iteration := mul.iteration + (!add.done).asUInt
      add
    }

    // val output = Stream(AddStageContext())
    // output.valid := input.valid
    // output.payload.assignSomeByName(input.payload)
    // output.x         := (input.xx - input.yy + input.x0).truncated
    // output.y         := (((input.xy) << 1)   + input.y0).truncated
    // output.done.allowOverride
    // output.iteration.allowOverride
    // output.done      := input.done || input.xx + input.yy >= 4.0 || input.iteration === iterationLimit
    // output.iteration := input.iteration + (!output.done).asUInt
    // input.ready := output.ready
  }

  val router = new Area {
    val input = addStage.output.pipelined(m2s = true, s2m = true)
    val wantedId = Counter(1 << idWidth, inc = io.rsp.fire)
    val wanted = input.done && wantedId === input.id
    // io.rsp.valid := input.valid && wanted
    // io.rsp.iteration := input.iteration
    // inserter.loopback <-/< input.throwWhen(io.rsp.fire).transmuteWith(RouterContext())

    val select = input.valid && wanted
    val twoStreams =
      StreamDemux(input, select = (!select).asUInt, portCount = 2)
    inserter.loopback <-/< twoStreams(1).transmuteWith(RouterContext())
    io.rsp <-/< twoStreams(0).translateWith {
      val rslt = PixelResult(g)
      rslt.iteration := twoStreams(0).iteration
      rslt
    }

    // val outValid = Reg(Bool) init(False)
    // val outBuf = Reg(PixelResult(g))
    // io.rsp.payload := outBuf
    // io.rsp.valid := outValid
    // when (io.rsp.fire) {
    //   outValid := False
    // }

    // val vacant = !outValid || io.rsp.ready
    // val throwCond = input.valid && wanted && vacant
    // inserter.loopback <-/< input.throwWhen(throwCond).transmuteWith(RouterContext())
    // when (throwCond) {
    //   outBuf.iteration := input.iteration
    //   outValid := True
    // }
  }
}
