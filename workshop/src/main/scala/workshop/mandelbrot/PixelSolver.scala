package workshop.mandelbrot

import spinal.core._
import spinal.lib._

case class PixelSolverGenerics(
    fixAmplitude: Int,
    fixResolution: Int,
    iterationLimit: Int
) {
  val iterationWidth = log2Up(iterationLimit + 1)
  def iterationType = UInt(iterationWidth bits)
  def fixType = SFix(
    peak = fixAmplitude exp,
    resolution = fixResolution exp
  )
}

case class PixelTask(g: PixelSolverGenerics) extends Bundle {
  val x, y = g.fixType
}

case class PixelResult(g: PixelSolverGenerics) extends Bundle {
  val iteration = g.iterationType
}

case class PixelSolver(g: PixelSolverGenerics) extends Component {
  val io = new Bundle {
    val cmd = slave Stream (PixelTask(g))
    val rsp = master Stream (PixelResult(g))
  }

  //TODO implement the mandelbrot algorithm
  val computing = Reg(Bool()) init (False)
  val sending = Reg(Bool()) init (False)
  val x, y, x0, y0 = Reg(g.fixType)
  val xx = x * x
  val yy = y * y
  val xy = x * y
  val xNext = xx - yy + x0
  val yNext = (xy << 1) + y0
  val iter = Reg(g.iterationType)

  when(!sending && !computing && io.cmd.valid) {
    computing := True
    x := 0
    x0 := io.cmd.x
    y := 0
    y0 := io.cmd.y
    iter := 0
  }
  when(io.cmd.valid && io.cmd.ready) {
    computing := False
    sending := True
  }

  val done = !(xx + yy < 4.0 && iter < g.iterationLimit)
  when(computing && !done) {
    x := xNext.truncated
    y := yNext.truncated
    iter := iter + 1
  }

  when(io.rsp.valid && io.rsp.ready) {
    computing := False
    sending := False
  }

  io.cmd.ready := computing && done
  io.rsp.valid := sending
  io.rsp.iteration := iter
}
