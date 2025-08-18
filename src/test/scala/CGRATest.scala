import chisel3._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._

trait CGRATestUtils {
  def initializePorts(dut: CGRA2x2): Unit = {
    // 外部入力を無効化
    dut.io.dataInNorth00.valid.poke(false.B)
    dut.io.dataInNorth01.valid.poke(false.B)
    dut.io.dataInSouth10.valid.poke(false.B)
    dut.io.dataInSouth11.valid.poke(false.B)
    dut.io.dataInEast01.valid.poke(false.B)
    dut.io.dataInEast11.valid.poke(false.B)
    dut.io.dataInWest00.valid.poke(false.B)
    dut.io.dataInWest10.valid.poke(false.B)

    // 外部出力を受信準備完了に
    dut.io.dataOutNorth00.ready.poke(true.B)
    dut.io.dataOutNorth01.ready.poke(true.B)
    dut.io.dataOutSouth10.ready.poke(true.B)
    dut.io.dataOutSouth11.ready.poke(true.B)
    dut.io.dataOutEast01.ready.poke(true.B)
    dut.io.dataOutEast11.ready.poke(true.B)
    dut.io.dataOutWest00.ready.poke(true.B)
    dut.io.dataOutWest10.ready.poke(true.B)

    // 入力データを0に初期化
    dut.io.dataInNorth00.bits.poke(0.S)
    dut.io.dataInNorth01.bits.poke(0.S)
    dut.io.dataInSouth10.bits.poke(0.S)
    dut.io.dataInSouth11.bits.poke(0.S)
    dut.io.dataInEast01.bits.poke(0.S)
    dut.io.dataInEast11.bits.poke(0.S)
    dut.io.dataInWest00.bits.poke(0.S)
    dut.io.dataInWest10.bits.poke(0.S)

  }

  def configurePE(
      peConfig: PEConfig,
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
    peConfig.operation.poke(operation)
    peConfig.input1Sel.poke(input1Sel.U)
    peConfig.input2Sel.poke(input2Sel.U)
    peConfig.rfWriteAddr.poke(writeAddr.U)
    peConfig.rfReadAddr1.poke(readAddr1.U)
    peConfig.rfReadAddr2.poke(readAddr2.U)
    peConfig.immediate.poke(immediate.S)
    peConfig.outputSel.poke(outputSel)
    peConfig.rfWriteEn.poke(writeEnable.B)
  }

  def reset(dut: CGRA2x2): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step(1)
    dut.reset.poke(false.B)
    dut.clock.step(1)
  }

  def disablePE(peConfig: PEConfig): Unit = {
    configurePE(peConfig, outputSel = 0.U)
  }
}

class CGRATest extends AnyFreeSpec with CGRATestUtils {

  "single PE addition test" in {
    simulate(new CGRA2x2()) { dut =>
      reset(dut)
      initializePorts(dut)

      // pe00で 10 + 20 = 30 を計算し、西に出力
      configurePE(dut.io.config.pe00,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.WEST
      )

      configurePE(dut.io.config.pe10,
        operation = AluOp.ADD,
        input1Sel = InputDirection.WEST,
        input2Sel = InputDirection.SOUTH,
        outputSel = OutputDirection.SOUTH
      )

      configurePE(dut.io.config.pe01,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.EAST,
        outputSel = OutputDirection.EAST
      )

      configurePE(dut.io.config.pe11,
        operation = AluOp.ADD,
        input1Sel = InputDirection.SOUTH,
        input2Sel = InputDirection.EAST,
        outputSel = OutputDirection.SOUTH
      )

      dut.io.dataInNorth00.valid.poke(true.B)
      dut.io.dataInNorth00.bits.poke(1.S)
      dut.io.dataInWest00.valid.poke(true.B)
      dut.io.dataInWest00.bits.poke(2.S)

      dut.io.dataInWest10.valid.poke(true.B)
      dut.io.dataInWest10.bits.poke(3.S)
      dut.io.dataInSouth10.valid.poke(true.B)
      dut.io.dataInSouth10.bits.poke(4.S)

      dut.io.dataInNorth01.valid.poke(true.B)
      dut.io.dataInNorth01.bits.poke(5.S)
      dut.io.dataInEast01.valid.poke(true.B)
      dut.io.dataInEast01.bits.poke(6.S)

      dut.io.dataInEast11.valid.poke(true.B)
      dut.io.dataInEast11.bits.poke(7.S)
      dut.io.dataInSouth11.valid.poke(true.B)
      dut.io.dataInSouth11.bits.poke(8.S)

      dut.clock.step(1)

      dut.io.dataOutWest00.valid.expect(true.B)
      dut.io.dataOutWest00.bits.expect(3.S)

      dut.io.dataOutSouth10.valid.expect(true.B)
      dut.io.dataOutSouth10.bits.expect(7.S)

      dut.io.dataOutEast01.valid.expect(true.B)
      dut.io.dataOutEast01.bits.expect(11.S)

      dut.io.dataOutSouth11.valid.expect(true.B)
      dut.io.dataOutSouth11.bits.expect(15.S)
    }
  }

  "horizontal" in {
    simulate(new CGRA2x2()) { dut =>
      reset(dut)
      initializePorts(dut)

      configurePE(dut.io.config.pe00,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.EAST
      )

      configurePE(dut.io.config.pe01,
        operation = AluOp.MUL,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.EAST
      )

      // pe00への入力: 5 + 3 = 8
      dut.io.dataInNorth00.valid.poke(true.B)
      dut.io.dataInNorth00.bits.poke(5.S)
      dut.io.dataInWest00.valid.poke(true.B)
      dut.io.dataInWest00.bits.poke(3.S)
      dut.io.dataInNorth01.valid.poke(true.B)
      dut.io.dataInNorth01.bits.poke(2.S)

      dut.clock.step(7)

      dut.io.dataOutEast01.valid.expect(true.B)
      dut.io.dataOutEast01.bits.expect(16.S)
    }
  }

  "two PE pipeline test" in {
    simulate(new CGRA2x2()) { dut =>
      reset(dut)
      initializePorts(dut)

      configurePE(dut.io.config.pe00,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.SOUTH
      )

      configurePE(dut.io.config.pe10,
        operation = AluOp.MUL,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.SOUTH
      )

      disablePE(dut.io.config.pe01)
      disablePE(dut.io.config.pe11)

      // pe00への入力: 5 + 3 = 8
      dut.io.dataInNorth00.valid.poke(true.B)
      dut.io.dataInNorth00.bits.poke(5.S)
      dut.io.dataInWest00.valid.poke(true.B)
      dut.io.dataInWest00.bits.poke(3.S)

      dut.clock.step(1)

      dut.io.dataInWest10.valid.poke(true.B)
      dut.io.dataInWest10.bits.poke(2.S)

      dut.clock.step(6)

      dut.io.dataOutSouth10.valid.expect(true.B)
      dut.io.dataOutSouth10.bits.expect(16.S)
    }
  }


  "dot product" in {
    simulate(new CGRA2x2()) { dut =>
      reset(dut)
      initializePorts(dut)

      // ベクトルA = [1, 2, 3, 4]
      // ベクトルB = [5, 6, 7, 8]
      // 内積 = 1*5 + 2*6 + 3*7 + 4*8 = 70

      configurePE(dut.io.config.pe00,
        operation = AluOp.MUL,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.WEST,
        outputSel = OutputDirection.SOUTH,
      )

      configurePE(dut.io.config.pe01,
        operation = AluOp.MUL,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.EAST,
        outputSel = OutputDirection.SOUTH,
      )

      dut.io.dataInNorth00.valid.poke(true.B)
      dut.io.dataInNorth00.bits.poke(1.S)   // A[0]
      dut.io.dataInWest00.valid.poke(true.B)
      dut.io.dataInWest00.bits.poke(5.S)    // B[0]

      dut.io.dataInNorth01.valid.poke(true.B)
      dut.io.dataInNorth01.bits.poke(2.S)   // A[1]
      dut.io.dataInEast01.valid.poke(true.B)
      dut.io.dataInEast01.bits.poke(6.S)    // B[1]

      dut.clock.step(6)

      configurePE(dut.io.config.pe10,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        outputSel = OutputDirection.SOUTH,
        writeEnable = true,
        writeAddr = 0,
        immediate = 0
      )

      configurePE(dut.io.config.pe11,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        outputSel = OutputDirection.SOUTH,
        writeEnable = true,
        writeAddr = 0,
        immediate = 0
      )

      dut.io.dataInNorth00.valid.poke(true.B)
      dut.io.dataInNorth00.bits.poke(3.S)   // A[2]
      dut.io.dataInWest00.valid.poke(true.B)
      dut.io.dataInWest00.bits.poke(7.S)    // B[2]

      dut.io.dataInNorth01.valid.poke(true.B)
      dut.io.dataInNorth01.bits.poke(4.S)   // A[3]
      dut.io.dataInEast01.valid.poke(true.B)
      dut.io.dataInEast01.bits.poke(8.S)    // B[3]

      dut.clock.step(1)

      dut.io.dataOutSouth10.valid.expect(true.B)
      dut.io.dataOutSouth10.bits.expect(5.S)
      dut.io.dataOutSouth11.valid.expect(true.B)
      dut.io.dataOutSouth11.bits.expect(12.S)

      dut.clock.step(4)

      configurePE(dut.io.config.pe10,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        outputSel = OutputDirection.SOUTH,
        writeEnable = true,
        writeAddr = 1,
        immediate = 0,
      )

      configurePE(dut.io.config.pe11,
        operation = AluOp.ADD,
        input1Sel = InputDirection.NORTH,
        input2Sel = InputDirection.IMMEDIATE,
        outputSel = OutputDirection.SOUTH,
        writeEnable = true,
        writeAddr = 1,
        immediate = 0,
      )

      dut.clock.step(1)

      dut.io.dataOutSouth10.valid.expect(true.B)
      dut.io.dataOutSouth10.bits.expect(21.S)

      dut.io.dataOutSouth11.valid.expect(true.B)
      dut.io.dataOutSouth11.bits.expect(32.S)

      disablePE(dut.io.config.pe00)
      disablePE(dut.io.config.pe01)

      configurePE(dut.io.config.pe10,
        operation = AluOp.ADD,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.RF2,
        outputSel = OutputDirection.WEST,
        readAddr1 = 0,
        readAddr2 = 1,
        writeAddr = 0,
        writeEnable = true,
      )

      configurePE(dut.io.config.pe11,
        operation = AluOp.ADD,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.RF2,
        outputSel = OutputDirection.SOUTH,
        readAddr1 = 0,
        readAddr2 = 1,
        writeAddr = 0,
        writeEnable = true,
      )

      dut.clock.step(1)

      dut.io.dataOutWest10.valid.expect(true.B)
      dut.io.dataOutWest10.bits.expect(26.S)

      dut.io.dataOutSouth11.valid.expect(true.B)
      dut.io.dataOutSouth11.bits.expect(44.S)

      configurePE(dut.io.config.pe10,
        operation = AluOp.ADD,
        input1Sel = InputDirection.RF1,
        input2Sel = InputDirection.IMMEDIATE,
        outputSel = OutputDirection.EAST,
        readAddr1 = 0,
        immediate = 0,
      )

      configurePE(dut.io.config.pe11,
        operation = AluOp.ADD,
        input1Sel = InputDirection.WEST,
        input2Sel = InputDirection.RF1,
        outputSel = OutputDirection.SOUTH,
        readAddr1 = 0,
      )

      dut.clock.step(2)

      dut.io.dataOutSouth11.valid.expect(true.B)
      dut.io.dataOutSouth11.bits.expect(70.S)

    }
  }
}