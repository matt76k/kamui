import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

object OutputDirection {
  val NORTH = 1.U(4.W)   // 0001
  val SOUTH = 2.U(4.W)   // 0010
  val EAST  = 4.U(4.W)   // 0100
  val WEST  = 8.U(4.W)   // 1000

  object Mask {
    val NORTH = 1
    val SOUTH = 2
    val EAST = 4
    val WEST = 8
  }
  
  def apply(mask: Int): UInt = mask.U(4.W)

}

object InputDirection {
  val NORTH = 0
  val SOUTH = 1
  val EAST  = 2
  val WEST  = 3
  val RF1   = 4
  val RF2   = 5
  val IMMEDIATE = 6
  val OUTPUT   = 7
}

class PEConfig extends Bundle {
  val operation = AluOp()
  val input1Sel = UInt(3.W)
  val input2Sel = UInt(3.W)
  val rfWriteAddr = UInt(2.W)
  val rfReadAddr1 = UInt(2.W)
  val rfReadAddr2 = UInt(2.W)
  val immediate = SInt(32.W)
  val outputSel = UInt(4.W)
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
  })
  
  val registerFile = RegInit(VecInit(Seq.fill(4)(0.S(dataWidth.W))))
  val outputReg = RegInit(0.S(dataWidth.W))
  val outputValid = RegInit(false.B)

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
  inputSources(7) := outputReg
  
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
  
  val northSelected = (io.config.outputSel & OutputDirection.NORTH) =/= 0.U
  val southSelected = (io.config.outputSel & OutputDirection.SOUTH) =/= 0.U
  val eastSelected = (io.config.outputSel & OutputDirection.EAST) =/= 0.U
  val westSelected = (io.config.outputSel & OutputDirection.WEST) =/= 0.U
  
  val hasSelectedOutput = northSelected || southSelected || eastSelected || westSelected
  val selectedOutputsReady = Mux(hasSelectedOutput,
    (!northSelected || io.dataOutNorth.ready) &&
    (!southSelected || io.dataOutSouth.ready) &&
    (!eastSelected || io.dataOutEast.ready) &&
    (!westSelected || io.dataOutWest.ready),
    true.B
  )

  alu.io.req.valid := selectedInput1Valid && selectedInput2Valid
  alu.io.req.bits.data1 := inputSources(io.config.input1Sel)
  alu.io.req.bits.data2 := inputSources(io.config.input2Sel)
  alu.io.req.bits.op := io.config.operation
  alu.io.resp.ready := selectedOutputsReady
  
  val inputReady = selectedOutputsReady && alu.io.req.ready
  
  val northInputSelected = (io.config.input1Sel === InputDirection.NORTH.U) || (io.config.input2Sel === InputDirection.NORTH.U)
  val southInputSelected = (io.config.input1Sel === InputDirection.SOUTH.U) || (io.config.input2Sel === InputDirection.SOUTH.U)
  val eastInputSelected = (io.config.input1Sel === InputDirection.EAST.U) || (io.config.input2Sel === InputDirection.EAST.U)
  val westInputSelected = (io.config.input1Sel === InputDirection.WEST.U) || (io.config.input2Sel === InputDirection.WEST.U)

  io.dataInNorth.ready := northInputSelected && inputReady
  io.dataInSouth.ready := southInputSelected && inputReady
  io.dataInEast.ready := eastInputSelected && inputReady
  io.dataInWest.ready := westInputSelected && inputReady

  when(alu.io.resp.valid && alu.io.resp.ready) {
    outputReg := alu.io.resp.bits.result
    outputValid := true.B

    when(io.config.rfWriteEn) {
      registerFile(io.config.rfWriteAddr) := alu.io.resp.bits.result
    }
  }.otherwise {
    when(outputValid && selectedOutputsReady) {
      outputValid := false.B
    }    
  }

  io.dataOutNorth.valid := outputValid && northSelected
  io.dataOutNorth.bits := outputReg
  io.dataOutSouth.valid := outputValid && southSelected
  io.dataOutSouth.bits := outputReg
  io.dataOutEast.valid := outputValid && eastSelected
  io.dataOutEast.bits := outputReg
  io.dataOutWest.valid := outputValid && westSelected
  io.dataOutWest.bits := outputReg

}

object PEDriver extends App {
  ChiselStage.emitSystemVerilogFile(
    new PE(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}