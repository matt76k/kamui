import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

class PEConfig extends Bundle {
  val operation = AluOp()
  val input1Sel = UInt(3.W)
  val input2Sel = UInt(3.W)
  val rfWriteAddr = UInt(2.W)
  val rfReadAddr1 = UInt(2.W)
  val rfReadAddr2 = UInt(2.W)
  val immediate = SInt(32.W)
  val outputSel = UInt(4.W)  // bit 0: North, bit 1: South, bit 2: East, bit 3: West
  val rfWriteEn = Bool()
}

class PE(dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val dataInNorth = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInSouth = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInEast  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInWest  = Flipped(Decoupled(SInt(dataWidth.W)))
    
    val dataOutNorth = Decoupled(SInt(dataWidth.W))
    val dataOutSouth = Decoupled(SInt(dataWidth.W))
    val dataOutEast  = Decoupled(SInt(dataWidth.W))
    val dataOutWest  = Decoupled(SInt(dataWidth.W))
    
    val config = Input(new PEConfig)
    
    val busy = Output(Bool())
  })
  
  val registerFile = RegInit(VecInit(Seq.fill(4)(0.S(dataWidth.W))))

  val alu = Module(new Alu(dataWidth))
  
  val inputSources = Wire(Vec(8, SInt(dataWidth.W)))
  val inputValids = Wire(Vec(8, Bool()))
  
  inputSources(0) := io.dataInNorth.bits
  inputSources(1) := io.dataInSouth.bits
  inputSources(2) := io.dataInEast.bits
  inputSources(3) := io.dataInWest.bits
  inputSources(4) := registerFile(io.config.rfReadAddr1)
  inputSources(5) := registerFile(io.config.rfReadAddr2)
  inputSources(6) := io.config.immediate
  inputSources(7) := 0.S(dataWidth.W)
  
  inputValids(0) := io.dataInNorth.valid
  inputValids(1) := io.dataInSouth.valid
  inputValids(2) := io.dataInEast.valid
  inputValids(3) := io.dataInWest.valid
  inputValids(4) := true.B
  inputValids(5) := true.B
  inputValids(6) := true.B
  inputValids(7) := true.B
  
  val selectedInput1Valid = inputValids(io.config.input1Sel)
  val selectedInput2Valid = inputValids(io.config.input2Sel)
  
  // 選択された出力のready信号のみをチェック
  val selectedOutputsReady = Wire(Bool())
  val northSelected = io.config.outputSel(0)
  val southSelected = io.config.outputSel(1)
  val eastSelected = io.config.outputSel(2)
  val westSelected = io.config.outputSel(3)
  
  selectedOutputsReady := (!northSelected || io.dataOutNorth.ready) &&
                         (!southSelected || io.dataOutSouth.ready) &&
                         (!eastSelected || io.dataOutEast.ready) &&
                         (!westSelected || io.dataOutWest.ready)

  alu.io.req.valid := selectedInput1Valid && selectedInput2Valid
  alu.io.req.bits.data1 := inputSources(io.config.input1Sel)
  alu.io.req.bits.data2 := inputSources(io.config.input2Sel)
  alu.io.req.bits.op := io.config.operation
  alu.io.resp.ready := selectedOutputsReady
  
  io.dataInNorth.ready := selectedOutputsReady && alu.io.req.ready
  io.dataInSouth.ready := selectedOutputsReady && alu.io.req.ready
  io.dataInEast.ready  := selectedOutputsReady && alu.io.req.ready
  io.dataInWest.ready  := selectedOutputsReady && alu.io.req.ready
  
  when(alu.io.resp.valid && alu.io.resp.ready) {
    registerFile(0) := alu.io.resp.bits.result
  
    when(io.config.rfWriteEn && io.config.rfWriteAddr =/= 0.U) {
      registerFile(io.config.rfWriteAddr) := alu.io.resp.bits.result
    }
  }
  
  // 出力の制御
  io.dataOutNorth.valid := alu.io.resp.valid && northSelected
  io.dataOutNorth.bits := registerFile(0)
  io.dataOutSouth.valid := alu.io.resp.valid && southSelected
  io.dataOutSouth.bits := registerFile(0)
  io.dataOutEast.valid := alu.io.resp.valid && eastSelected
  io.dataOutEast.bits := registerFile(0)
  io.dataOutWest.valid := alu.io.resp.valid && westSelected
  io.dataOutWest.bits := registerFile(0)
  
  io.busy := alu.io.req.valid && !alu.io.req.ready   
}

object PEDriver extends App {
  ChiselStage.emitSystemVerilogFile(
    new PE(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}