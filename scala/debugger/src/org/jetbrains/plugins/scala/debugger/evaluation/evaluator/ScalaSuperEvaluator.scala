package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.{Evaluator, Modifier}

import scala.annotation.nowarn

trait ScSuperEvaluator

class ScalaSuperEvaluator(iterations: Int = 0) extends ScalaThisEvaluator(iterations) with ScSuperEvaluator

class ScalaSuperDelegate(delegate: Evaluator) extends Evaluator with ScSuperEvaluator {
  override def evaluate(context: EvaluationContextImpl): AnyRef = delegate.evaluate(context)

  override def getModifier: Modifier = delegate.getModifier: @nowarn("cat=deprecation") // IDEA-366620
}
