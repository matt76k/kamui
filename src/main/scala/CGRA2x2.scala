import chisel3._
import circt.stage.ChiselStage
import chisel3.util._

class CGRAConfig extends Bundle {
  val pe00 = new PEConfig
  val pe01 = new PEConfig
  val pe10 = new PEConfig
  val pe11 = new PEConfig
}

class CGRA2x2(dataWidth: Int = 32, fifoDepth: Int = 2) extends Module {
  val io = IO(new Bundle {
    val dataInNorth00 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInNorth01 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInSouth10 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInSouth11 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInEast01  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInEast11  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInWest00  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInWest10  = Flipped(Decoupled(SInt(dataWidth.W)))

    val dataOutNorth00 = Decoupled(SInt(dataWidth.W))
    val dataOutNorth01 = Decoupled(SInt(dataWidth.W))
    val dataOutSouth10 = Decoupled(SInt(dataWidth.W))
    val dataOutSouth11 = Decoupled(SInt(dataWidth.W))
    val dataOutEast01  = Decoupled(SInt(dataWidth.W))
    val dataOutEast11  = Decoupled(SInt(dataWidth.W))
    val dataOutWest00  = Decoupled(SInt(dataWidth.W))
    val dataOutWest10  = Decoupled(SInt(dataWidth.W))

    val config = Input(new CGRAConfig)
    
  })
  
  val pe00 = Module(new PE(dataWidth))
  val pe01 = Module(new PE(dataWidth))
  val pe10 = Module(new PE(dataWidth))
  val pe11 = Module(new PE(dataWidth))
  
  pe00.io.config := io.config.pe00
  pe01.io.config := io.config.pe01
  pe10.io.config := io.config.pe10
  pe11.io.config := io.config.pe11
  
  val fifo_pe00_to_pe01 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe01_to_pe00 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe10_to_pe11 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe11_to_pe10 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe00_to_pe10 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe10_to_pe00 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe01_to_pe11 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  val fifo_pe11_to_pe01 = Module(new Queue(SInt(dataWidth.W), fifoDepth))
  
  // PE間の接続（FIFOを介して）
  // pe00 → pe01 (East)
  pe00.io.dataOutEast <> fifo_pe00_to_pe01.io.enq
  fifo_pe00_to_pe01.io.deq <> pe01.io.dataInWest
  
  // pe01 → pe00 (West)
  pe01.io.dataOutWest <> fifo_pe01_to_pe00.io.enq
  fifo_pe01_to_pe00.io.deq <> pe00.io.dataInEast
  
  // pe10 → pe11 (East)
  pe10.io.dataOutEast <> fifo_pe10_to_pe11.io.enq
  fifo_pe10_to_pe11.io.deq <> pe11.io.dataInWest
  
  // pe11 → pe10 (West)
  pe11.io.dataOutWest <> fifo_pe11_to_pe10.io.enq
  fifo_pe11_to_pe10.io.deq <> pe10.io.dataInEast
  
  // pe00 → pe10 (South)
  pe00.io.dataOutSouth <> fifo_pe00_to_pe10.io.enq
  fifo_pe00_to_pe10.io.deq <> pe10.io.dataInNorth
  
  // pe10 → pe00 (North)
  pe10.io.dataOutNorth <> fifo_pe10_to_pe00.io.enq
  fifo_pe10_to_pe00.io.deq <> pe00.io.dataInSouth
  
  // pe01 → pe11 (South)
  pe01.io.dataOutSouth <> fifo_pe01_to_pe11.io.enq
  fifo_pe01_to_pe11.io.deq <> pe11.io.dataInNorth
  
  // pe11 → pe01 (North)
  pe11.io.dataOutNorth <> fifo_pe11_to_pe01.io.enq
  fifo_pe11_to_pe01.io.deq <> pe01.io.dataInSouth
  
  // 外部入力の接続
  // 北側入力
  pe00.io.dataInNorth <> io.dataInNorth00
  pe01.io.dataInNorth <> io.dataInNorth01

  // 南側入力
  pe10.io.dataInSouth <> io.dataInSouth10
  pe11.io.dataInSouth <> io.dataInSouth11
  
  // 東側入力
  pe01.io.dataInEast <> io.dataInEast01
  pe11.io.dataInEast <> io.dataInEast11

  // 西側入力
  pe00.io.dataInWest <> io.dataInWest00
  pe10.io.dataInWest <> io.dataInWest10

  // 外部出力の接続
  // 北側出力
  io.dataOutNorth00 <> pe00.io.dataOutNorth
  io.dataOutNorth01 <> pe01.io.dataOutNorth
  
  // 南側出力
  io.dataOutSouth10 <> pe10.io.dataOutSouth
  io.dataOutSouth11 <> pe11.io.dataOutSouth
  
  // 東側出力
  io.dataOutEast01 <> pe01.io.dataOutEast
  io.dataOutEast11 <> pe11.io.dataOutEast
  
  // 西側出力
  io.dataOutWest00 <> pe00.io.dataOutWest
  io.dataOutWest10 <> pe10.io.dataOutWest

}

object CGRA2x2Driver extends App {
  ChiselStage.emitSystemVerilogFile(
    new CGRA2x2(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}