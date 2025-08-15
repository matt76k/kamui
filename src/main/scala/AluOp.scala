import chisel3._
import chisel3.util._

object AluOp extends ChiselEnum {
  val ADD = Value
  val MUL = Value
  val SUB = Value
  val SLL = Value
  val SRA = Value
  val SRL = Value
  val AND = Value
  val OR  = Value
  val XOR = Value
  val MIN = Value
  val MAX = Value
}

import AluOp._

class AluConfig {
  
  val cycleSpecs = Seq(
    (ADD, 1),
    (SUB, 1), 
    (AND, 1),
    (OR,  1),
    (XOR, 1),
    (SLL, 1),
    (SRA, 1),
    (SRL, 1),
    (MIN, 1),
    (MAX, 1),
    (MUL, 4)
  )
  
  val maxStages = cycleSpecs.map(_._2).max
  val minStages = cycleSpecs.map(_._2).min

  def getCycleLookupSeq: Seq[(UInt, UInt)] = {
    cycleSpecs.map { case (op, cycles) => 
      (op.asUInt, cycles.U)
    }
  }

  def getCycles(op: AluOp.Type): Int = {
    cycleSpecs.find(_._1 == op).map(_._2).getOrElse(1)
  }
}