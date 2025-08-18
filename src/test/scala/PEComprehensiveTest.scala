import chisel3._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._

class PEComprehensiveTest extends AnyFreeSpec with PETestUtils {

  "basic arithmetic operations" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Addition - North(10) + Immediate(5) = 15
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 0,  // north input
        input2Sel = 6,  // immediate
        immediate = 5,
        writeEnable = false
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(10.S)

      dut.clock.step(1)
      initializePorts(dut)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(15.S)

      // Test 2: Subtraction - South(20) - East(3) = 17
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.SUB,
        input1Sel = 1,  // south input
        input2Sel = 2,  // east input
        writeEnable = false
      )

      dut.io.dataInSouth.valid.poke(true.B)
      dut.io.dataInSouth.bits.poke(20.S)
      dut.io.dataInEast.valid.poke(true.B)
      dut.io.dataInEast.bits.poke(3.S)

      dut.clock.step(1)
      initializePorts(dut)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(17.S)

      // Test 3: Immediate only - Immediate(42) + Immediate(42) = 84
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate (same value)
        immediate = 42,
        writeEnable = false
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(84.S)
    }
  }

  "multiplication operations" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Basic multiplication - West(7) * Immediate(6) = 42
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 3,  // west input
        input2Sel = 6,  // immediate
        immediate = 6,
        writeEnable = false
      )

      dut.io.dataInWest.valid.poke(true.B)
      dut.io.dataInWest.bits.poke(7.S)

      // Multiplication takes 5 cycles
      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(42.S)

      // Test 2: Larger multiplication - North(13) * South(11) = 143
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 0,  // north input
        input2Sel = 1,  // south input
        writeEnable = false
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(13.S)
      dut.io.dataInSouth.valid.poke(true.B)
      dut.io.dataInSouth.bits.poke(11.S)

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(143.S)

      // Test 3: Immediate multiplication - Immediate(9) * Immediate(9) = 81
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 9,
        writeEnable = false
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(81.S)
    }
  }

  "register file operations" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Write to register - North(25) + Immediate(5) = 30, store in RF[1]
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 0,  // north input
        input2Sel = 6,  // immediate
        writeAddr = 1,  // write to register 1
        immediate = 5,
        writeEnable = true
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(25.S)

      dut.clock.step(1)
      initializePorts(dut)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(30.S)

      // Test 2: Read from register - RF[1] + Immediate(10) = 40
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 4,  // register file 1
        input2Sel = 6,  // immediate
        readAddr1 = 1,  // read from register 1
        immediate = 10,
        writeEnable = false
      )

      dut.clock.step(1)

      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(40.S)

      // Test 3: Use both register files - Store values then compute
      // Store 15 in RF[0]
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write to register 0
        immediate = 15,
        writeEnable = true
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.bits.expect(30.S) // 15 + 15 = 30

      // Store 20 in RF[2]
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        writeAddr = 2,  // write to register 2
        immediate = 20,
        writeEnable = true
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.bits.expect(40.S) // 20 + 20 = 40

      // Compute RF[0] * RF[2] = 30 * 40 = 1200
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 4,  // register file 1 (reads from readAddr1)
        input2Sel = 5,  // register file 2 (reads from readAddr2)
        readAddr1 = 0,  // read from register 0
        readAddr2 = 2,  // read from register 2
        writeEnable = false
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(1200.S)
    }
  }

  "output direction control" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Output to North
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 50,
        outputSel = OutputDirection.NORTH,
        writeEnable = false
      )

      dut.clock.step(1)

      dut.io.dataOutNorth.valid.expect(true.B)
      dut.io.dataOutNorth.bits.expect(100.S)
      dut.io.dataOutSouth.valid.expect(false.B)
      dut.io.dataOutEast.valid.expect(false.B)
      dut.io.dataOutWest.valid.expect(false.B)

      // Test 2: Output to East
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.SUB,
        input1Sel = 6,  // immediate (75)
        input2Sel = 0,  // north input (25)
        immediate = 75,
        outputSel = OutputDirection.EAST,
        writeEnable = false
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(25.S)

      dut.clock.step(1)
      initializePorts(dut)

      dut.io.dataOutEast.valid.expect(true.B)
      dut.io.dataOutEast.bits.expect(50.S) // 75 - 25 = 50
      dut.io.dataOutNorth.valid.expect(false.B)
      dut.io.dataOutSouth.valid.expect(false.B)
      dut.io.dataOutWest.valid.expect(false.B)

      // Test 3: Output to West
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 1,  // south input
        input2Sel = 2,  // east input
        outputSel = OutputDirection.WEST,
        writeEnable = false
      )

      dut.io.dataInSouth.valid.poke(true.B)
      dut.io.dataInSouth.bits.poke(33.S)
      dut.io.dataInEast.valid.poke(true.B)
      dut.io.dataInEast.bits.poke(17.S)

      dut.clock.step(1)
      initializePorts(dut)

      dut.io.dataOutWest.valid.expect(true.B)
      dut.io.dataOutWest.bits.expect(50.S) // 33 + 17 = 50
      dut.io.dataOutNorth.valid.expect(false.B)
      dut.io.dataOutSouth.valid.expect(false.B)
      dut.io.dataOutEast.valid.expect(false.B)
    }
  }

  "complex accumulator pattern" in {
    simulate(new PE(32)) { dut =>
      
      // Initialize accumulator to 0
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate (0)
        input2Sel = 6,  // immediate (0)
        writeAddr = 0,  // accumulator in RF[0]
        immediate = 0,
        writeEnable = true
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.bits.expect(0.S)

      // Accumulate: ACC = ACC + 10
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 4,  // register file (ACC)
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write back to accumulator
        readAddr1 = 0,  // read accumulator
        immediate = 10,
        writeEnable = true
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.bits.expect(10.S) // 0 + 10 = 10

      // Accumulate: ACC = ACC + 25
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 4,  // register file (ACC)
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write back to accumulator
        readAddr1 = 0,  // read accumulator
        immediate = 25,
        writeEnable = true
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.bits.expect(35.S) // 10 + 25 = 35

      // Multiply accumulator by 2
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 4,  // register file (ACC)
        input2Sel = 6,  // immediate
        writeAddr = 0,  // write back to accumulator
        readAddr1 = 0,  // read accumulator
        immediate = 2,
        writeEnable = true
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(70.S) // 35 * 2 = 70

      // Final read from accumulator
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 4,  // register file (ACC)
        input2Sel = 6,  // immediate (0)
        readAddr1 = 0,  // read accumulator
        immediate = 0,
        writeEnable = false
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(70.S) // Final accumulated value
    }
  }

  "pipeline behavior and timing" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Back-to-back operations without interference
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 0,  // north input
        input2Sel = 6,  // immediate
        immediate = 1,
        writeEnable = false
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(100.S)

      dut.clock.step(1)
      initializePorts(dut)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(101.S)

      // Immediately start another operation
      configureOperation(dut,
        operation = AluOp.SUB,
        input1Sel = 1,  // south input
        input2Sel = 6,  // immediate
        immediate = 50,
        writeEnable = false
      )

      dut.io.dataInSouth.valid.poke(true.B)
      dut.io.dataInSouth.bits.poke(200.S)

      dut.clock.step(1)
      initializePorts(dut)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(150.S) // 200 - 50 = 150

      // Test 2: Mixed operation types
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 7,
        writeEnable = false
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(49.S) // 7 * 7 = 49

      // Follow immediately with addition
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 8,
        writeEnable = false
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(16.S) // 8 + 8 = 16
    }
  }

  "edge cases and error conditions" in {
    simulate(new PE(32)) { dut =>
      
      // Test 1: Zero operations
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 0,
        writeEnable = false
      )

      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(0.S) // 0 + 0 = 0

      // Test 2: Negative numbers
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.ADD,
        input1Sel = 6,  // immediate (-10)
        input2Sel = 0,  // north input (5)
        immediate = -10,
        writeEnable = false
      )

      dut.io.dataInNorth.valid.poke(true.B)
      dut.io.dataInNorth.bits.poke(5.S)

      dut.clock.step(1)
      initializePorts(dut)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(-5.S) // -10 + 5 = -5

      // Test 3: Large numbers
      initializePorts(dut)
      configureOperation(dut,
        operation = AluOp.MUL,
        input1Sel = 6,  // immediate
        input2Sel = 6,  // immediate
        immediate = 1000,
        writeEnable = false
      )

      for (i <- 1 to 4) {
        dut.clock.step(1)
        dut.io.dataOutSouth.valid.expect(false.B)
      }
    
      dut.clock.step(1)
      dut.io.dataOutSouth.valid.expect(true.B)
      dut.io.dataOutSouth.bits.expect(1000000.S) // 1000 * 1000 = 1,000,000
    }
  }
}