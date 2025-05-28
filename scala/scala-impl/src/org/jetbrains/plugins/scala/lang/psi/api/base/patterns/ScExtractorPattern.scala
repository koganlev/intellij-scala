package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.FakeCompanionClassOrCompanionClass
import org.jetbrains.plugins.scala.lang.psi.api.ExtractorMatch
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScExtractorPattern.{ArgPatternsMapping, ExtractorTarget}
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScPattern.SeqExpectingPattern
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction.CommonNames
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScMacroDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.base.patterns.ScInterpolationPatternImpl
import org.jetbrains.plugins.scala.lang.psi.impl.expr.PatternTypeInference
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.StdTypes
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

  final def bindWithScrutinee(scrutineeType: Option[ScType]): Option[ScalaResolveResult] =
    ExpandedExtractorResolveProcessor.resolveActualUnapply(ref, scrutineeType)

  final def targetFor(scrutineeType: Option[ScType]): Option[ExtractorTarget] = {
    bindWithScrutinee(scrutineeType).collect {
      case srr@ScalaResolveResult(fun: ScFunction, _) if fun.name == CommonNames.Unapply && ScPattern.isQuasiquote(fun) =>
        new ExtractorTarget.Quasiquote(fun, srr, scrutineeType, this)
      case srr@ScalaResolveResult(fun: ScFunction, _) if fun.name == CommonNames.Unapply && QuasiquoteInferUtil.isMetaQQ(fun) =>
        new ExtractorTarget.MetaQQ(fun, srr, scrutineeType, this)
      case srr@ScalaResolveResult(fun: ScFunction, _) if fun.name == CommonNames.Unapply && fun.parameters.count(!_.isImplicit) == 1 =>
        new ExtractorTarget.Unapply(fun, srr, scrutineeType, this)
      case srr@ScalaResolveResult(fun: ScFunction, _) if fun.name == CommonNames.UnapplySeq && fun.parameters.count(!_.isImplicit) == 1 =>
        new ExtractorTarget.UnapplySeq(fun, srr, scrutineeType, this)
      case srr@ScalaResolveResult(FakeCompanionClassOrCompanionClass(cl: ScClass), _) if cl.isCase && cl.tooBigForUnapply =>
        new ExtractorTarget.TooBigCaseClass(cl,srr, scrutineeType, this)
    }
  }

  final def argPatternsMapping(scrutineeType: Option[ScType]): Option[ArgPatternsMapping] =
    targetFor(scrutineeType).flatMap(_.argPatternsMapping)
}

object ScExtractorPattern {
  sealed abstract class ExtractorTarget(val resolveResult: ScalaResolveResult, scrutineeType: Option[ScType], protected val pattern: ScExtractorPattern) {
    def unapplyType: Option[ScType]
    final lazy val substitutor: ScSubstitutor = {
      val subst = resolveResult.substitutor
      scrutineeType.fold(subst)(tp => subst.followed(PatternTypeInference.doTypeInference(pattern, tp)))
    }

    def isMacroExtractor: Boolean
    def argPatternsMapping: Option[ArgPatternsMapping]
  }

  object ExtractorTarget {
    sealed abstract class Function(ssr: ScalaResolveResult, sty: Option[ScType], pattern: ScExtractorPattern) extends ExtractorTarget(ssr, sty, pattern) {
      def targetFunction: ScFunction
      final def returnType: Option[ScType] = targetFunction.returnType.map(substitutor).toOption
      final def unapplyType: Option[ScType] = returnType

      final override def isMacroExtractor: Boolean = targetFunction.is[ScMacroDefinition]
    }

    trait WithExtractorMatches {
      def extractorMatches: Option[LazyList[ExtractorMatch]]
    }

    final class Quasiquote private[ScExtractorPattern](override val targetFunction: ScFunction, ssr: ScalaResolveResult, sty: Option[ScType], p: ScExtractorPattern) extends Function(ssr, sty, p) {
      override def argPatternsMapping: Some[ArgPatternsMapping] = Some(new ArgPatternsMapping.Quasiquote(pattern, targetFunction))
    }
    final class MetaQQ private[ScExtractorPattern](override val targetFunction: ScFunction, ssr: ScalaResolveResult, sty: Option[ScType], p: ScExtractorPattern) extends Function(ssr, sty, p) {
      override def argPatternsMapping: Option[ArgPatternsMapping] = try {
        val interpolationPattern = pattern.asInstanceOf[ScInterpolationPatternImpl]
        val qqPatterns = QuasiquoteInferUtil.getMetaQQPatternTypes(interpolationPattern)
        Some(new ArgPatternsMapping.MetaQQ(pattern.subpatterns.zip(qqPatterns).toMap, targetFunction))
      } catch {
        case _: ArrayIndexOutOfBoundsException => None // workaround for meta parser failure on malformed quasiquotes
      }
    }
    final class Unapply private[ScExtractorPattern](override val targetFunction: ScFunction, ssr: ScalaResolveResult, sty: Option[ScType], p: ScExtractorPattern) extends Function(ssr, sty, p) with WithExtractorMatches {
      override def extractorMatches: Option[LazyList[ExtractorMatch.Unapply]] = returnType.map(ExtractorMatch.unapplyExtractorMatches(_, pattern, targetFunction))
      override def argPatternsMapping: Option[ArgPatternsMapping] = extractorMatches.map(new ArgPatternsMapping.Unapply(_, pattern))
    }
    final class UnapplySeq private[ScExtractorPattern](override val targetFunction: ScFunction,ssr: ScalaResolveResult, sty: Option[ScType], p: ScExtractorPattern) extends Function(ssr, sty, p) with WithExtractorMatches {
      override def extractorMatches: Option[LazyList[ExtractorMatch.UnapplySeq]] = returnType.map(ExtractorMatch.unapplySeqExtractorMatches(_, pattern, targetFunction))
      override def argPatternsMapping: Option[ArgPatternsMapping] = extractorMatches.map(new ArgPatternsMapping.UnapplySeq(_, pattern))
    }
    final class TooBigCaseClass private[ScExtractorPattern](val clazz: ScClass, ssr: ScalaResolveResult, sty: Option[ScType], p: ScExtractorPattern) extends ExtractorTarget(ssr, sty, p) with WithExtractorMatches {
      def unapplyType: Option[ScType] = clazz.`type`().toOption.map(substitutor)

      def extractorMatch: ExtractorMatch = {
        implicit val elementScope: ElementScope = pattern.elementScope
        val undefSubst = substitutor.followed(ScSubstitutor(ScThisType(clazz)))
        val params: Seq[ScParameter] = clazz.parameters
        val types = params.map(_.`type`().getOrAny).map(undefSubst)
        val selectorType = unapplyType.getOrElse(StdTypes.instance.Any)

        if (types.nonEmpty && params.last.isVarArgs) {
          ExtractorMatch.UnapplySeq(types.dropRight(1), types.last.tryWrapIntoSeqType, selectorType)
        } else {
          ExtractorMatch.Unapply.productWithSelector(types, selectorType)
        }
      }

      override def extractorMatches: Some[LazyList[ExtractorMatch]] = Some(LazyList(extractorMatch))
      override def argPatternsMapping: Some[ArgPatternsMapping] = extractorMatch match {
        case unapply: ExtractorMatch.Unapply => Some(new ArgPatternsMapping.Unapply(LazyList(unapply), pattern))
        case unapply: ExtractorMatch.UnapplySeq => Some(new ArgPatternsMapping.UnapplySeq(LazyList(unapply), pattern))
        case _ =>
          throw new AssertionError("extractorMatch should always be Unapply or UnapplySeq")
      }

      override def isMacroExtractor: false = false
    }

    object UnapplyMatches {
      def unapply(target: ExtractorTarget): Option[LazyList[ExtractorMatch.Unapply]] = target match {
        case target: ExtractorTarget.Unapply => target.extractorMatches
        case target: ExtractorTarget.TooBigCaseClass => target.extractorMatch.asOptionOf[ExtractorMatch.Unapply].map(LazyList(_))
        case _ => None
      }
    }

    object UnapplySeqMatches {
      def unapply(target: ExtractorTarget): Option[LazyList[ExtractorMatch.UnapplySeq]] = target match {
        case target: ExtractorTarget.UnapplySeq => target.extractorMatches
        case target: ExtractorTarget.TooBigCaseClass => target.extractorMatch.asOptionOf[ExtractorMatch.UnapplySeq].map(LazyList(_))
        case _ => None
      }
    }

    object Function {
      def unapply(target: ExtractorTarget.Function): Some[ScFunction] = Some(target.targetFunction)

      object Returning {
        def unapply(target: ExtractorTarget.Function): Option[ScType] = target.returnType
      }
    }
  }

  sealed abstract class ArgPatternsMapping {
    def typeOfArg(pattern: ScPattern): Option[ScType]
  }

  object ArgPatternsMapping {
    private[ScExtractorPattern] final class Quasiquote(extractorPattern: ScExtractorPattern, fun: ScFunction) extends ArgPatternsMapping {
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

    private[ScExtractorPattern] final class MetaQQ(qqPatterns: Map[ScPattern, String], fun: ScFunction) extends ArgPatternsMapping {
      private implicit def projectContext: ProjectContext = fun.projectContext

      private def makeType(clazz: String): Option[ScType] =
        ScalaPsiElementFactory.createTypeElementFromText(clazz, fun).`type`().toOption

      override def typeOfArg(pattern: ScPattern): Option[ScType] = qqPatterns.get(pattern).flatMap(makeType)
    }

    private[ScExtractorPattern] final class Unapply(matches: LazyList[ExtractorMatch.Unapply], extractorPattern: ScExtractorPattern) extends ArgPatternsMapping {
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

    private[ScExtractorPattern] final class UnapplySeq(matches: LazyList[ExtractorMatch.UnapplySeq], pattern: ScExtractorPattern) extends ArgPatternsMapping {
      private implicit def elementScope: ElementScope = pattern.elementScope

      private lazy val argPatterns = pattern.argPatterns
      private lazy val forIndex = matches
        .bestMatch(argPatterns.length)

      override def typeOfArg(pattern: ScPattern): Option[ScType] =
        forIndex.map { forIndex =>
          val i = argPatterns.indexOf(pattern)
          forIndex.productTypes.lift(i).getOrElse {
            pattern match {
              case SeqExpectingPattern() => forIndex.sequenceType.tryWrapIntoSeqType
              case _ => forIndex.sequenceType
            }
          }
        }
    }
  }
}