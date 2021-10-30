package workshop.waveplayer

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite.{AxiLite4, AxiLite4SlaveFactory}
import spinal.lib.bus.misc.BusSlaveFactory

case class WavePlayerGenerics(
    sampleWidth: Int,
    sampleCountLog2: Int,
    phaseWidth: Int,
    filterCoefWidth: Int
) {
  def Sample = UInt(sampleWidth bits)
  def sampleCount = 1 << sampleCountLog2
  // println(s"sampleWidth=$sampleWidth, sampleCountLog2=$sampleCountLog2, phaseWidth=$phaseWidth, filterCoefWidth=$filterCoefWidth")
}

case class WavePlayer(generics: WavePlayerGenerics) extends Area {
  import generics._
  assert(phaseWidth >= sampleCountLog2)

  val phase = new Area {
    val run = Bool
    val rate = UInt(phaseWidth bits)

    //TODO phase
    val value = Reg(UInt(phaseWidth bits)) init (0)
    when(run) {
      value := value + rate
    }
  }

  val sampler = new Area {
    //TODO Rom definition with a sinus + it sampling
    val romSamples = for (sampleId <- 0 until sampleCount) yield {
      val sin = Math.sin(2.0 * Math.PI * sampleId / sampleCount)
      val normalizedSin = (0.5 * sin + 0.5) * (Math.pow(2.0, sampleWidth) - 1)
      val sinVal = BigInt(normalizedSin.toLong)
      // println(s"sinVal=$sinVal, sampleId=$sampleId")
      sinVal
    }
    val rom = Mem(Sample, sampleCount) initBigInt (romSamples)
    val romIdx = phase.value >> (phaseWidth - sampleCountLog2)
    val sample = rom.readAsync(romIdx)
    // report(L"sample=$sample, romIdx=$romIdx")
  }

  val filter = new Area {
    val bypass = Bool
    val coef = UInt(filterCoefWidth bits)
    //TODO first order filter + bypass logic
    val accumulator = Reg(UInt(sampleWidth + filterCoefWidth bits)) init (0)
    accumulator := accumulator - (accumulator * coef >> filterCoefWidth) + sampler.sample * coef
    val filtredSampler = accumulator >> filterCoefWidth
    val value =
      bypass ? sampler.sample | filtredSampler //Output value of the filter Area
  }
}

class WavePlayerMapper(bus: BusSlaveFactory, wavePlayer: WavePlayer)
    extends Area {
  //TODO phase.rate, phase.value, filter.bypass, filter.coef mapping
  bus.driveAndRead(wavePlayer.phase.run, address = 0x00) init (False)
  bus.drive(wavePlayer.phase.rate, address = 0x04)
  bus.read(wavePlayer.phase.value, address = 0x08)

  bus.driveAndRead(wavePlayer.filter.bypass, address = 0x10) init (True)
  bus.drive(wavePlayer.filter.coef, address = 0x14) init (0)

  //  Could be used to make the ram writable by the BusSlaveFactory
  //  bus.writeMemWordAligned(wavePlayer.sampler.ram, addressOffset = wavePlayer.generics.sampleCount)
}
