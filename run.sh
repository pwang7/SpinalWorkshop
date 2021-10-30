#! /bin/sh

set -o errexit
set -o nounset
set -o xtrace

MILL_VERSION=0.9.7

if [ ! -f mill ]; then
  curl -L https://github.com/com-lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION > mill && chmod +x mill
fi

./mill version

# Check format and lint
# ./mill mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources
./mill workshop.checkFormat
./mill workshop.fix --check

# Run test and simulation
./mill workshop.test.testOnly workshop.apb3decoder.Apb3DecoderTester
./mill workshop.test.testOnly workshop.blackboxAndClock.BlackBoxAndClockTester
./mill workshop.test.testOnly workshop.counter.CounterTester
./mill workshop.test.testOnly workshop.function.FunctionUnitTester
./mill workshop.test.testOnly workshop.mandelbrot.PixelSolverTester
./mill workshop.test.testOnly workshop.mandelbrot.PixelSolverPipelineTester
./mill workshop.test.testOnly workshop.mandelbrot.PixelSolverMultiCoreTester
./mill workshop.test.testOnly workshop.prime.PrimeTester
./mill workshop.test.testOnly workshop.pwm.ApbPwmTester
./mill workshop.test.testOnly workshop.simCounter.SimCounterTester
./mill workshop.test.testOnly workshop.simStreamJoinFork.SimStreamJoinForkTester
./mill workshop.test.testOnly workshop.stream.StreamUnitTester
./mill workshop.test.testOnly workshop.timer.Apb3TimerTester
./mill workshop.test.testOnly workshop.timer.TimerTester
./mill workshop.test.testOnly workshop.uart.UartCtrlRxTester
./mill workshop.test.testOnly workshop.udp.UdpAppSelfTester
./mill workshop.test.testOnly workshop.waveplayer.AxiLite4WavePlayerTester
# ./mill workshop.test # Not work yet

cd workshop/test/src/python/workshop/udp/onnetwork
timeout 5 make &
sleep 3
python3 Client.py
