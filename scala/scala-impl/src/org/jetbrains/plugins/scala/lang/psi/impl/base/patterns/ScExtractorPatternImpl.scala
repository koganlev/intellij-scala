package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScPrimaryConstructor, ScStableCodeReference}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.{ScExtractorPattern, ScNamingPattern, ScParenthesisedPattern, ScPattern, ScSeqWildcardPattern}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.ScExtractorPatternImpl._
import org.jetbrains.plugins.scala.lang.psi.impl.expr.PatternTypeInference
import org.jetbrains.plugins.scala.lang.psi.types.{Context, ScType}
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result.{Failure, TypeResult}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

abstract class ScExtractorPatternImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScPatternImpl with ScExtractorPattern {
  override def `type`(): TypeResult =
    ref.bind() match {
      case Some(ScalaResolveResult(fun: ScFunction, _)) if fun.isUnapplyMethod && fun.parameters.count(!_.isImplicit) == 1 =>
        val expectedType = this.expectedType
        val subst =
          expectedType.fold(
            ScSubstitutor.empty
          )(PatternTypeInference.doTypeInference(this, _))

        fun.paramClauses.clauses.head.parameters.head.`type`().map(subst)
      case _ =>
        Failure(ScalaBundle.message("cannot.resolve.unknown.symbol"))
    }

  override def isIrrefutableForImpl(scrutineeType: ScType, deep: Boolean): Boolean = {
    val typedParamsOpt = for {
      unapplyMethod <- resolveUnapplyMethodFromReference(ref)
      caseClass <- unapplyMethod.syntheticCaseClass
      (clazz: ScClass, substitutor) <- scrutineeType.extractClassType
      if clazz.isCase && clazz == caseClass
      constr <- clazz.constructor
    } yield getTypedParametersOfPrimaryConstructor(constr, substitutor)

    typedParamsOpt exists {
      // check if the patterns are irrefutable for the parameter types
      typedParams =>
        subpatterns.corresponds(typedParams) {
          case (pattern, (param, paramType)) =>
            if (param.isRepeatedParameter) extractsRepeatedParameterIrrefutably(pattern)
            else pattern.isIrrefutableFor(paramType, deep)
        }
    }
  }
}

object ScExtractorPatternImpl {
  private def resolveUnapplyMethodFromReference(ref: ScStableCodeReference): Option[ScFunction] = for {
    resolveResult <- ref.bind()
    maybeUnapplyMethod = Option(resolveResult.getElement)
    unapplyMethod <- maybeUnapplyMethod.collect { case method: ScFunction if method.isUnapplyMethod => method }
  } yield unapplyMethod

  private def getTypedParametersOfPrimaryConstructor(constr: ScPrimaryConstructor, substitutor: ScSubstitutor): Seq[(ScParameter, ScType)] = {
    val params = constr.parameterList.clauses.headOption.map(_.parameters).getOrElse(Seq.empty)
    for {
      param <- params
      paramType = param.`type`().map(substitutor).getOrAny
    } yield param -> paramType
  }

  private def extractsRepeatedParameterIrrefutably(pattern: ScPattern): Boolean = {
    pattern match {
      case _: ScSeqWildcardPattern => true
      case p: ScNamingPattern => extractsRepeatedParameterIrrefutably(p.named)
      case p: ScParenthesisedPattern => p.innerElement.exists(extractsRepeatedParameterIrrefutably)
      case _ => false
    }
  }
}