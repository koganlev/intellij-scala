package org.jetbrains.plugins.scala.lang.psi.impl.base
package patterns

import _root_.org.jetbrains.plugins.scala.lang.psi.types._
import com.intellij.lang.ASTNode
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._
import org.jetbrains.plugins.scala.lang.psi.api.base.{ScPrimaryConstructor, ScStableCodeReference}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.ScConstructorPatternImpl.calcType
import org.jetbrains.plugins.scala.lang.psi.impl.expr.PatternTypeInference
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.project.ProjectContext

class ScConstructorPatternImpl(node: ASTNode) extends ScalaPsiElementImpl (node) with ScPatternImpl with ScConstructorPattern {

  override def toString: String = "ConstructorPattern"

  override def subpatterns: Seq[ScPattern] = if (args != null) args.patterns else Seq.empty

  override def isIrrefutableForImpl(scrutineeType: ScType, deep: Boolean): Boolean =
    ScConstructorPatternImpl.isIrrefutable(scrutineeType, ref, subpatterns, deep)

  override def `type`(): TypeResult = calcType(this, ref, this.expectedType)
}

object ScConstructorPatternImpl {
  def calcType(pattern: ScPattern, ref: ScStableCodeReference, expectedType: => Option[ScType])(implicit projectContext: ProjectContext): TypeResult =
    ref.bind() match {
      case Some(ScalaResolveResult(fun: ScFunction, _)) if fun.isUnapplyMethod && fun.parameters.count(!_.isImplicit) == 1 =>
        val subst =
          expectedType.fold(
            ScSubstitutor.empty
          )(PatternTypeInference.doTypeInference(pattern, _))

        fun.paramClauses.clauses.head.parameters.head.`type`().map(subst)
      case _ => Failure(ScalaBundle.message("cannot.resolve.unknown.symbol"))
    }

  def isIrrefutable(matchedType: ScType, ref: ScStableCodeReference, subpatterns: Seq[ScPattern], deep: Boolean): Boolean = {
    implicit val context: Context = Context(ref)
    val typedParamsOpt = for {
      unapplyMethod <- resolveUnapplyMethodFromReference(ref)
      caseClass <- unapplyMethod.syntheticCaseClass
      (clazz: ScClass, substitutor) <- matchedType.extractClassType
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