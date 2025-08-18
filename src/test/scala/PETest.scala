import chisel3._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._

class PETest extends AnyFreeSpec with PETestUtils {

  "op test" in {
    simulate(new PE(32)) { dut =>
      
      initializePorts(dut)

      //　north(5) + immediate(1)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        writeAddr = 0,  // write to register file address 0
        readAddr1 = 0,  // not used in this operation
        readAddr2 = 0,  // not used in this operation
        immediate = 1,
        writeEnable = true
      )

      dut.io.dataOutSouth.ready.poke(true.B)
      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(5.S)

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(6.S)

      // レジスタファイルを使う

      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.IMMEDIATE,
        writeAddr = 0,  // write to register file address 0
        readAddr1 = 0,  // register file 0 (accumulator)
        readAddr2 = 0,  // not used in this operation
        immediate = 1,
        writeEnable = true
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(7.S)

      // 掛け算

      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.IMMEDIATE,
        writeAddr = 0,  // write to register file address 0
        readAddr1 = 0,  // not used in this operation
        readAddr2 = 0,  // not used in this operation
        immediate = 6,
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(42.S)

      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(false.B)
      dut.io.dataOutSouth.bits.expect(42.S)
    }
  }

  "rf test" in {
    simulate(new PE(32)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)
      dut.clock.step(1)
      initializePorts(dut)

      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        writeAddr = 0,  // write to register file address 0
        immediate = 1,
        writeEnable = true
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(0.S)

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(1.S)

      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        writeAddr = 1,  // write to register file address 0
        immediate = 2,
        writeEnable = true
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(2.S)

      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.RF2,
        readAddr1 = 0,
        readAddr2 = 1,
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(3.S)
    }
  }

}