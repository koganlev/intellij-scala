package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.debugger.engine.evaluation.expression.{ExpressionEvaluator, Modifier}
import com.intellij.debugger.engine.evaluation.{EvaluationContext, EvaluationContextImpl}
import com.sun.jdi.Value

import scala.annotation.nowarn

class ScalaCompilingExpressionEvaluator(evaluator: ScalaCompilingEvaluator) extends ExpressionEvaluator {
  override def evaluate(context: EvaluationContext): Value = evaluator.evaluate(context.asInstanceOf[EvaluationContextImpl])

  override def getModifier: Modifier = evaluator.getModifier: @nowarn("cat=deprecation")
}
