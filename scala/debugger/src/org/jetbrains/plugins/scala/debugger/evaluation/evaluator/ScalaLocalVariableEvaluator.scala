package org.jetbrains.plugins.scala.debugger.evaluation.evaluator

import com.intellij.debugger.JavaDebuggerBundle
import com.intellij.debugger.engine.evaluation.expression.{ModifiableEvaluator, ModifiableValue, Modifier}
import com.intellij.debugger.engine.evaluation.{EvaluateException, EvaluationContextImpl}
import com.intellij.debugger.jdi.{LocalVariableProxyImpl, StackFrameProxyImpl}
import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl
import com.intellij.debugger.ui.tree.NodeDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.sun.jdi._
import org.jetbrains.plugins.scala.debugger.evaluation.EvaluationException
import org.jetbrains.plugins.scala.debugger.evaluation.evaluator.ScalaLocalVariableEvaluator.MyModifier
import org.jetbrains.plugins.scala.debugger.evaluation.util.DebuggerUtil

/**
 * Follows the implementation details of `com.intellij.debugger.engine.evaluation.expression.LocalVariableEvaluator`
 * but is adapted for Scala.
 */
class ScalaLocalVariableEvaluator(name: String, sourceName: String) extends ModifiableEvaluator {
  private val depthOfSearch = 20

  private val myName: String = DebuggerUtil.withoutBackticks(name)
  private val mySourceName: String = DebuggerUtil.withoutBackticks(sourceName)
  private var myContext: EvaluationContextImpl = _
  private var myEvaluatedVariable: LocalVariableProxyImpl = _
  private var myParameterIndex: Option[Int] = None
  private var myMethodName: String = _

  def setParameterIndex(parameterIndex: Int): Unit = {
    myParameterIndex = Some(parameterIndex)
  }

  def setMethodName(name: String): Unit = {
    myMethodName = name
  }

  private def sourceName(frameProxy: StackFrameProxyImpl) =
    try frameProxy.location().sourceName()
    catch {
      case _: AbsentInformationException => ""
    }

  override def evaluateModifiable(context: EvaluationContextImpl): ModifiableValue = {

    def saveContextAndGetValue(framePr: StackFrameProxyImpl, local: LocalVariableProxyImpl) = {
      myEvaluatedVariable = local
      myContext = context
      Some(framePr.getValue(local))
    }

    val startFrame = context.getFrameProxy
    val threadProxy = startFrame.threadProxy()
    val startIndex = startFrame.getFrameIndex
    val lastIndex = threadProxy.frameCount() - 1
    val upperBound = Math.min(lastIndex, startIndex + depthOfSearch)

    def evaluateWithFrames(evaluationStrategy: StackFrameProxyImpl => Option[AnyRef]): Option[AnyRef] = {
      for (frameIndex <- startIndex to upperBound) {
        val frameProxy = threadProxy.frame(frameIndex)
        if (sourceName(frameProxy) == mySourceName) {
          try {
            evaluationStrategy(frameProxy) match {
              case Some(x) => return Some(x)
              case _ =>
            }
          }
          catch {
            case _: EvaluateException =>
              myEvaluatedVariable = null
              myContext = null
          }
        }
      }
      None
    }

    def withSimpleName(frameProxy: StackFrameProxyImpl) : Option[AnyRef] = {
      val local: LocalVariableProxyImpl = frameProxy.visibleVariableByName(myName)
      Option(local).flatMap(saveContextAndGetValue(frameProxy, _))
    }

    def withDollar(frameProxy: StackFrameProxyImpl): Option[AnyRef] = {
      for (i <- 1 to 2) {
        val local = frameProxy.visibleVariableByName(myName + "$" + i)
        if (local != null) return saveContextAndGetValue(frameProxy, local)
      }
      val locals = frameProxy.visibleVariables()
      locals.forEach(local =>
        if (local.name().startsWith(myName + "$"))
          return saveContextAndGetValue(frameProxy, local)
      )
      None
    }

    def parameterByIndex(frameProxy: StackFrameProxyImpl) = {
      if (frameProxy == null || myParameterIndex.isEmpty) None
      else {
        val frameMethodName = frameProxy.location().method().name()
        if ((myMethodName == null) || frameMethodName.startsWith(myMethodName)) {
          try {
            val values = frameProxy.getArgumentValues
            if (values != null && !values.isEmpty) {
              val idx = myParameterIndex.get
              val paramIdx = if (idx < 0) values.size() + idx else idx
              Some(values.get(paramIdx))
            } else {
              None
            }
          }
          catch {case _: InternalException => None}
        } else None
      }
    }

    if (context.getFrameProxy == null) {
      throw EvaluationException(JavaDebuggerBundle.message("evaluation.error.no.stackframe"))
    }

    val result = evaluateWithFrames(withSimpleName)
      .orElse(evaluateWithFrames(parameterByIndex))
      .orElse(evaluateWithFrames(withDollar))

    result match {
      case Some(x) =>
        val modifier =
          if ((myEvaluatedVariable ne null) && (myContext ne null)) new MyModifier(myContext, myEvaluatedVariable)
          else null
        new ModifiableValue(x, modifier)
      case None =>
        myEvaluatedVariable = null
        myContext = null
        throw EvaluationException(JavaDebuggerBundle.message("evaluation.error.local.variable.missing", myName))
    }
  }

  override def getModifier: Modifier = {
    if ((myEvaluatedVariable ne null) && (myContext ne null)) {
      return new MyModifier(myContext, myEvaluatedVariable)
    }
    null
  }
}

private object ScalaLocalVariableEvaluator {
  private val LOG: Logger = Logger.getInstance(classOf[ScalaLocalVariableEvaluator])

  private final class MyModifier(context: EvaluationContextImpl, evaluatedVariable: LocalVariableProxyImpl) extends Modifier {
    override def canInspect: Boolean = true
    override def canSetValue: Boolean = true
    override def setValue(value: Value): Unit = {
      val frameProxy = context.getFrameProxy
      try {
        if (DebuggerUtil.isScalaRuntimeRef(evaluatedVariable.getType.name())) {
          frameProxy.getValue(evaluatedVariable) match {
            case objRef: ObjectReference =>
              val field = DebuggerUtil.runtimeRefField(objRef.referenceType())
              field.foreach(objRef.setValue(_, value))
            case _ =>
              frameProxy.setValue(evaluatedVariable, value)
          }
        } else {
          frameProxy.setValue(evaluatedVariable, value)
        }
      }
      catch {
        case e: EvaluateException =>
          LOG.error(e)
      }
    }

    override def getExpectedType: Type =
      try evaluatedVariable.getType
      catch {
        case e: EvaluateException =>
          LOG.error(e)
          null
      }

    override def getInspectItem(project: Project): NodeDescriptor =
      new LocalVariableDescriptorImpl(project, evaluatedVariable)
  }
}
