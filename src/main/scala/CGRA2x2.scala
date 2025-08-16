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
    // 外部入力 (各辺から)
    val dataInNorth0 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInNorth1 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInSouth0 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInSouth1 = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInEast0  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInEast1  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInWest0  = Flipped(Decoupled(SInt(dataWidth.W)))
    val dataInWest1  = Flipped(Decoupled(SInt(dataWidth.W)))
    
    // 外部出力 (各辺へ)
    val dataOutNorth0 = Decoupled(SInt(dataWidth.W))
    val dataOutNorth1 = Decoupled(SInt(dataWidth.W))
    val dataOutSouth0 = Decoupled(SInt(dataWidth.W))
    val dataOutSouth1 = Decoupled(SInt(dataWidth.W))
    val dataOutEast0  = Decoupled(SInt(dataWidth.W))
    val dataOutEast1  = Decoupled(SInt(dataWidth.W))
    val dataOutWest0  = Decoupled(SInt(dataWidth.W))
    val dataOutWest1  = Decoupled(SInt(dataWidth.W))
    
    // 設定
    val config = Input(new CGRAConfig)
    
    // ステータス
    val busy = Output(Bool())
  })
  
  // 4つのPEをインスタンス化
  val pe00 = Module(new PE(dataWidth)) // 左上
  val pe01 = Module(new PE(dataWidth)) // 右上
  val pe10 = Module(new PE(dataWidth)) // 左下
  val pe11 = Module(new PE(dataWidth)) // 右下
  
  // 各PEの設定
  pe00.io.config := io.config.pe00
  pe01.io.config := io.config.pe01
  pe10.io.config := io.config.pe10
  pe11.io.config := io.config.pe11
  
  // PE間接続用のFIFO
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
  pe00.io.dataInNorth <> io.dataInNorth0
  pe01.io.dataInNorth <> io.dataInNorth1
  
  // 南側入力
  pe10.io.dataInSouth <> io.dataInSouth0
  pe11.io.dataInSouth <> io.dataInSouth1
  
  // 東側入力
  pe01.io.dataInEast <> io.dataInEast0
  pe11.io.dataInEast <> io.dataInEast1
  
  // 西側入力
  pe00.io.dataInWest <> io.dataInWest0
  pe10.io.dataInWest <> io.dataInWest1
  
  // 外部出力の接続
  // 北側出力
  io.dataOutNorth0 <> pe00.io.dataOutNorth
  io.dataOutNorth1 <> pe01.io.dataOutNorth
  
  // 南側出力
  io.dataOutSouth0 <> pe10.io.dataOutSouth
  io.dataOutSouth1 <> pe11.io.dataOutSouth
  
  // 東側出力
  io.dataOutEast0 <> pe01.io.dataOutEast
  io.dataOutEast1 <> pe11.io.dataOutEast
  
  // 西側出力
  io.dataOutWest0 <> pe00.io.dataOutWest
  io.dataOutWest1 <> pe10.io.dataOutWest
  
  // 全体のビジー信号
  io.busy := pe00.io.busy || pe01.io.busy || pe10.io.busy || pe11.io.busy
}

object CGRA2x2Driver extends App {
  ChiselStage.emitSystemVerilogFile(
    new CGRA2x2(),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}