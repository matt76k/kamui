import chisel3._
import circt.stage.ChiselStage
import chisel3.util._
import AluOp._

class AluRequest(dataWidth: Int) extends Bundle {
  val data1 = SInt(dataWidth.W)
  val data2 = SInt(dataWidth.W)
  val op = AluOp()
}

class AluResponse(dataWidth: Int) extends Bundle {
  val result = SInt(dataWidth.W)
}

class Alu(dataWidth: Int = 32, config: AluConfig = new AluConfig) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new AluRequest(dataWidth)))
    val resp = Decoupled(new AluResponse(dataWidth))
  })

  // 計算ロジックを分離
  private def computeResult(op: AluOp.Type, data1: SInt, data2: SInt): SInt = {
    val shiftAmount = data2(log2Ceil(dataWidth)-1, 0).asUInt
    
    MuxLookup(op.asUInt, 0.S)(Seq(
      ADD.asUInt -> (data1 + data2),
      MUL.asUInt -> (data1 * data2),
      SUB.asUInt -> (data1 - data2),
      SLL.asUInt -> (data1 << shiftAmount),
      SRA.asUInt -> (data1 >> shiftAmount),
      SRL.asUInt -> ((data1.asUInt >> shiftAmount).asSInt),
      AND.asUInt -> (data1 & data2),
      OR.asUInt  -> (data1 | data2),
      XOR.asUInt -> (data1 ^ data2),
      MIN.asUInt -> Mux(data1 < data2, data1, data2),
      MAX.asUInt -> Mux(data1 > data2, data1, data2)
    ))
  }

  object State extends ChiselEnum {
    val idle, executing = Value
  }
  
  val state = RegInit(State.idle)
  val cycleCounter = RegInit(0.U(3.W))
  
  val computedResult = computeResult(io.req.bits.op, io.req.bits.data1, io.req.bits.data2)
  val requiredCycles = MuxLookup(io.req.bits.op.asUInt, 1.U)(config.getCycleLookupSeq)
  val isSingleCycle = requiredCycles === 1.U

  
  io.req.ready := state === State.idle

  switch(state) {
    is(State.idle) {
      when(io.req.valid) {
        when(isSingleCycle) {
          state := State.idle
        }.otherwise {
          state := State.executing
          cycleCounter := requiredCycles - 1.U
        }
      }
    }
    is(State.executing) {
      when(cycleCounter === 0.U) {
        state := State.idle
      }.otherwise {
        cycleCounter := cycleCounter - 1.U
      }
    }
  }

  val canRespond = (state === State.idle && io.req.valid && isSingleCycle) || 
                   (state === State.executing && cycleCounter === 0.U)
  
  io.resp.valid := canRespond
  io.resp.bits.result := computedResult
}

object AluDriver extends App {
  ChiselStage.emitSystemVerilogFile(
    new Alu(32),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}