import chisel3._
import chisel3.simulator.EphemeralSimulator._
import OutputDirection._

trait PETestUtils {
  def initializePorts(pe: PE): Unit = {
    pe.io.dataInNorth.valid.poke(false.B)
    pe.io.dataInSouth.valid.poke(false.B)
    pe.io.dataInEast.valid.poke(false.B)
    pe.io.dataInWest.valid.poke(false.B)
    pe.io.dataOutNorth.ready.poke(true.B)
    pe.io.dataOutSouth.ready.poke(true.B)
    pe.io.dataOutEast.ready.poke(true.B)
    pe.io.dataOutWest.ready.poke(true.B)

    pe.io.dataInNorth.bits.poke(0.S)
    pe.io.dataInSouth.bits.poke(0.S)
    pe.io.dataInEast.bits.poke(0.S)
    pe.io.dataInWest.bits.poke(0.S)
  }

  def configureOperation(
      pe: PE,
      operation: AluOp.Type = AluOp.ADD,
      input1Sel: Int = 0,
      input2Sel: Int = 0,
      writeAddr: Int = 0,
      readAddr1: Int = 0,
      readAddr2: Int = 0,
      immediate: Int = 0,
      outputSel: UInt = OutputDirection.SOUTH,
      writeEnable: Boolean = false
  ): Unit = {
    pe.io.config.operation.poke(operation)
    pe.io.config.input1Sel.poke(input1Sel.U)
    pe.io.config.input2Sel.poke(input2Sel.U)
    pe.io.config.rfWriteAddr.poke(writeAddr.U)
    pe.io.config.rfReadAddr1.poke(readAddr1.U)
    pe.io.config.rfReadAddr2.poke(readAddr2.U)
    pe.io.config.immediate.poke(immediate.S)
    pe.io.config.outputSel.poke(outputSel)
    pe.io.config.rfWriteEn.poke(writeEnable.B)
  }
}
