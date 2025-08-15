import chisel3._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._

class AluTest extends AnyFreeSpec {
  "ALU operation test" in {
    simulate(new Alu(32)) { dut =>
      // テストケース1: 正の数同士の足し算
      dut.io.req.valid.poke(true.B)
      dut.io.req.bits.data1.poke(10.S)
      dut.io.req.bits.data2.poke(20.S)
      dut.io.req.bits.op.poke(AluOp.ADD)
      dut.io.resp.ready.poke(true.B)

      dut.clock.step()
      
      dut.io.resp.valid.expect(true.B)
      dut.io.resp.bits.result.expect(30.S)

      dut.io.req.valid.poke(false.B)
      dut.clock.step()

      // テストケース2: 正の数同士の掛け算
      dut.io.req.valid.poke(true.B)
      dut.io.req.bits.data1.poke(6.S)
      dut.io.req.bits.data2.poke(7.S)
      dut.io.req.bits.op.poke(AluOp.MUL)
      dut.io.resp.ready.poke(true.B)

      dut.clock.step()
      dut.io.resp.valid.expect(false.B)
      
      dut.clock.step()
      dut.io.resp.valid.expect(false.B)
      
      dut.clock.step()
      dut.io.resp.valid.expect(false.B)
      
      dut.clock.step()
      dut.io.resp.valid.expect(true.B)
      dut.io.resp.bits.result.expect(42.S)        

    }
  }
}