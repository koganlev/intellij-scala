package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.FakeCompanionClassOrCompanionClass
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScExtractorPattern.ArgPatternsMapping
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScPattern.SeqExpectingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction.CommonNames
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.ScInterpolationPatternImpl
import org.jetbrains.plugins.scala.lang.psi.impl.expr.PatternTypeInference
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScThisType
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.lang.resolve.processor.ExpandedExtractorResolveProcessor
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.plugins.scala.scalaMeta.QuasiquoteInferUtil

/**
 * Base trait for patterns, which resolve to unapply/unapplySeq methods
 */
trait ScExtractorPattern extends ScPattern {
  def ref: ScStableCodeReference
  final def argPatterns: Seq[ScPattern] = subpatterns

  final def targetFor(scrutineeType: Option[ScType]): Option[ScalaResolveResult] =
    ExpandedExtractorResolveProcessor.resolveActualUnapply(ref, scrutineeType)

  final def argPatternsMapping(scrutineeType: Option[ScType]): Option[ArgPatternsMapping] =
    ArgPatternsMapping(this, scrutineeType)
}

object ScExtractorPattern {
  sealed abstract class ArgPatternsMapping {
    def typeOfArg(pattern: ScPattern): Option[ScType]
  }

  object ArgPatternsMapping {
    private final class Quasiquote(extractorPattern: ScExtractorPattern, fun: ScFunction) extends ArgPatternsMapping {
      private implicit def projectContext: ProjectContext = fun.projectContext

      private lazy val seqSeqTreeType =
        ScalaPsiElementFactory.createTypeElementFromText("Seq[Seq[scala.reflect.api.Trees#Tree]]", fun).`type`().toOption
      private lazy val seqTreeType =
        ScalaPsiElementFactory.createTypeElementFromText("Seq[scala.reflect.api.Trees#Tree]", fun).`type`().toOption
      private lazy val treeType =
        ScalaPsiElementFactory.createTypeElementFromText("scala.reflect.api.Trees#Tree", fun).`type`().toOption

      override def typeOfArg(pattern: ScPattern): Option[ScType] = {
        if (pattern.getContext.getContext != extractorPattern)
          return None

        def possible(e: PsiElement): Boolean = e.isWhitespaceOrComment || (e.elementType match {
          case ScalaTokenTypes.tINTERPOLATED_STRING => true
          case ScalaTokenTypes.tLBRACE => true
          case _ => false
        })

        // in a string alá q"xyz ..${pattern} xyz" look for the .. or ...
        val nextInterpolatedString =
          pattern.prevSiblings.takeWhile(possible).find(_.elementType == ScalaTokenTypes.tINTERPOLATED_STRING)

        nextInterpolatedString.map(_.getText) match {
          case Some(str) if str.endsWith("...") => seqSeqTreeType
          case Some(str) if str.endsWith("..")  => seqTreeType
          case _ => treeType
        }
      }
    }

    private final class MetaQQ(qqPatterns: Map[ScPattern, String], fun: ScFunction) extends ArgPatternsMapping {
      private implicit def projectContext: ProjectContext = fun.projectContext

      private def makeType(clazz: String): Option[ScType] =
        ScalaPsiElementFactory.createTypeElementFromText(clazz, fun).`type`().toOption

      override def typeOfArg(pattern: ScPattern): Option[ScType] = qqPatterns.get(pattern).flatMap(makeType)
    }

    private final class Unapply(returnType: ScType, extractorPattern: ScExtractorPattern, subst: ScSubstitutor, fun: ScFunction) extends ArgPatternsMapping {
      private lazy val matches = ScPattern.unapplyExtractorMatches(subst(returnType), extractorPattern, fun)
      private lazy val argPatterns = extractorPattern.argPatterns

      private lazy val forIndexed =
        matches.bestMatch(argPatterns.length)
      private lazy val forNamed =
        matches.find(_.supportsNamedPatterns)

      override def typeOfArg(pattern: ScPattern): Option[ScType] =
        pattern match {
          case arg: ScNamedConstructorArgPattern =>
            forNamed.flatMap { forNamed =>
              forNamed.namedPatternTypes(pattern).get(arg.name).flatten
            }
          case _ =>
            forIndexed.flatMap(_.productTypes.lift(argPatterns.indexOf(pattern)))
        }
    }

    private final class UnapplySeq(returnType: ScType, pattern: ScExtractorPattern, subst: ScSubstitutor, fun: ScFunction) extends ArgPatternsMapping {
      private implicit def elementScope: ElementScope = fun.elementScope

      private lazy val argPatterns = pattern.argPatterns
      private lazy val forIndex = ScPattern.unapplySeqExtractorMatches(subst(returnType), pattern, fun)
        .bestMatch(argPatterns.length)

      override def typeOfArg(pattern: ScPattern): Option[ScType] =
        forIndex.map { forIndex =>
          val i = argPatterns.indexOf(pattern)
          subst(forIndex.productTypes.lift(i).getOrElse {
            pattern match {
              case SeqExpectingPattern() => forIndex.sequenceType.tryWrapIntoSeqType
              case _ => forIndex.sequenceType
            }
          })
        }
    }

    private final class TooBigCaseClass(subpatterns: Seq[ScPattern], types: Seq[ScType]) extends ArgPatternsMapping {
      override def typeOfArg(pattern: ScPattern): Option[ScType] = types.lift(subpatterns.indexOf(pattern))
    }

    def apply(pattern: ScExtractorPattern, scrutineeType: Option[ScType]): Option[ArgPatternsMapping] = {
      def updateSubstitutor(subst: ScSubstitutor): ScSubstitutor =
        scrutineeType.fold(subst)(tp => subst.followed(PatternTypeInference.doTypeInference(pattern, tp)))

      pattern.targetFor(scrutineeType) match {
        case Some(ScalaResolveResult(fun: ScFunction, _)) if fun.name == CommonNames.Unapply && ScPattern.isQuasiquote(fun) =>
          Some(new Quasiquote(pattern, fun))
        case Some(ScalaResolveResult(fun: ScFunction, _)) if fun.name == CommonNames.Unapply && QuasiquoteInferUtil.isMetaQQ(fun) =>
          try {
            val interpolationPattern = pattern.asInstanceOf[ScInterpolationPatternImpl]
            val qqPatterns = QuasiquoteInferUtil.getMetaQQPatternTypes(interpolationPattern)
            Some(new MetaQQ(pattern.subpatterns.zip(qqPatterns).toMap, fun))
          } catch {
            case _: ArrayIndexOutOfBoundsException => None // workaround for meta parser failure on malformed quasiquotes
          }
        case Some(ScalaResolveResult(fun: ScFunction, substitutor: ScSubstitutor)) if fun.name == CommonNames.Unapply && fun.parameters.count(!_.isImplicit) == 1 =>
          fun.returnType.toOption
            .map(new Unapply(_, pattern, updateSubstitutor(substitutor), fun))
        case Some(ScalaResolveResult(fun: ScFunction, substitutor: ScSubstitutor)) if fun.name == CommonNames.UnapplySeq && fun.parameters.count(!_.isImplicit) == 1 =>
          fun.returnType.toOption
            .map(new UnapplySeq(_, pattern, updateSubstitutor(substitutor), fun))
        case Some(ScalaResolveResult(FakeCompanionClassOrCompanionClass(cl: ScClass), subst: ScSubstitutor)) if cl.isCase && cl.tooBigForUnapply =>
          implicit val elementScope: ElementScope = pattern.elementScope
          val undefSubst = subst.followed(ScSubstitutor(ScThisType(cl)))
          val params: Seq[ScParameter] = cl.parameters
          val types = params.map(_.`type`().getOrAny).map(undefSubst)
          val args =
            if (types.nonEmpty && params.last.isVarArgs) types.dropRight(1) ++ types.last.wrapIntoSeqType
            else                                         types
          Some(new TooBigCaseClass(pattern.subpatterns, args))
        case _ => None
      }
    }
  }
}