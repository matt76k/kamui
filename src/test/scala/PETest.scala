import chisel3._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._

class PETest extends AnyFreeSpec {

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
    outputSel: UInt = "b0010".U(4.W),
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

  "PE operation test" in {
    simulate(new PE(32)) { dut =>
      
      initializePorts(dut)

      //　north(5) + immediate(1)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 0,  // north input
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write to register file address 0
        readAddr1 = 0,  // not used in this operation
        readAddr2 = 0,  // not used in this operation
        immediate = 1,
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(5.S)

      dut.clock.step(1)

      // 出力の確認
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(6.S)

      initializePorts(dut)

      dut.clock.step(1)

      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 4,  // register file
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write to register file address 0
        readAddr1 = 0,  // register file 0 (accumulator)
        readAddr2 = 0,  // not used in this operation
        immediate = 1,
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(7.S)

    }
  }
}