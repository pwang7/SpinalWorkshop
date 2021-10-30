package workshop.mandelbrot

import spinal.core._
import spinal.lib._

case class Dispatcher[T <: Data](dataType: T, outputsCount: Int)
    extends Component {
  val io = new Bundle {
    val input = slave Stream (dataType)
    val outputs = Vec(master Stream (dataType), outputsCount)
  }
  // TODO
  val streamVec = StreamDispatcherSequencial(io.input, outputsCount)
  for (idx <- 0 until outputsCount) {
    io.outputs(idx) << streamVec(idx)
  }
}

// TODO Define the Arbiter component (similar to the Dispatcher)
case class Arbiter[T <: Data](dataType: T, outputsCount: Int)
    extends Component {
  val io = new Bundle {
    val inputs = Vec(slave Stream (dataType), outputsCount)
    val output = master Stream (dataType)
  }
  io.output << StreamArbiterFactory.roundRobin.on(io.inputs)
}

case class PixelSolverMultiCore(g: PixelSolverGenerics, coreCount: Int)
    extends Component {
  val io = new Bundle {
    val cmd = slave Stream (PixelTask(g))
    val rsp = master Stream (PixelResult(g))
  }

  //TODO instantiate all components
  val solvers = (0 until coreCount).map(idx => PixelSolver(g))
  val dispatcher = Dispatcher(PixelTask(g), coreCount)
  val arbiter = Arbiter(PixelResult(g), coreCount)

  //TODO interconnect all that stuff
  dispatcher.io.input << io.cmd
  for (idx <- 0 until coreCount) {
    solvers(idx).io.cmd << dispatcher.io.outputs(idx)
    arbiter.io.inputs(idx) << solvers(idx).io.rsp
  }
  io.rsp << arbiter.io.output
}
