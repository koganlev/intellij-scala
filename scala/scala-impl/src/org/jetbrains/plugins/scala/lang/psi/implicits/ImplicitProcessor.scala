package org.jetbrains.plugins.scala.lang.psi.implicits

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiClass, PsiElement, PsiNamedElement, ResolveState}
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil._
import org.jetbrains.plugins.scala.lang.psi.api.ScPackageLike
import org.jetbrains.plugins.scala.lang.psi.api.base.ScFieldId
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScBindingPattern
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScExtensionBody, ScFunction, ScPatternDefinition, ScTypeAlias, ScTypeAliasDeclaration, ScTypeAliasDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.{ScExtendsBlock, ScTemplateBody}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScObject, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScDesignatorType, ScProjectionType}
import org.jetbrains.plugins.scala.lang.psi.types.api.{JavaArrayType, ParameterizedType, StdType, TypeParameterType}
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result.TypeResult
import org.jetbrains.plugins.scala.lang.psi.types.{AliasType, ScAbstractType, ScAndType, ScCompoundType, ScExistentialArgument, ScExistentialType, ScOrType, ScParameterizedType, ScType}
import org.jetbrains.plugins.scala.lang.psi.{ElementScope, ScalaPsiUtil}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveState.ResolveStateExt
import org.jetbrains.plugins.scala.lang.resolve.processor.BaseProcessor
import org.jetbrains.plugins.scala.lang.resolve.processor.precedence._
import org.jetbrains.plugins.scala.lang.resolve.{ResolveUtils, ScalaResolveResult, ScalaResolveState, StdKinds}
import org.jetbrains.plugins.scala.project.{ProjectContext, ProjectPsiElementExt}

import java.{util => ju}
import scala.annotation.tailrec
import scala.collection.mutable

/**
  * This class mark processor that only implicit object important among all PsiClasses
  */
abstract class ImplicitProcessor(override protected val getPlace: PsiElement,
                                 protected val withoutPrecedence: Boolean)
  extends BaseProcessor(StdKinds.refExprLastRef)(getPlace.projectContext)
    with SubstitutablePrecedenceHelper {

  private object ImplicitStrategy extends NameUniquenessStrategy

  override protected def nameUniquenessStrategy: NameUniquenessStrategy = ImplicitStrategy

  override protected val holder: TopPrecedenceHolder = new MappedTopPrecedenceHolder(nameUniquenessStrategy)

  private[this] val levelMap: ju.Map[ScalaResolveResult, ju.Set[ScalaResolveResult]] =
    new Object2ObjectOpenCustomHashMap[ScalaResolveResult, ju.Set[ScalaResolveResult]](nameUniquenessStrategy)

  override protected def clearLevelQualifiedSet(result: ScalaResolveResult): Unit = {
    //optimisation, do nothing
  }

  override protected def getLevelSet(result: ScalaResolveResult): ju.Set[ScalaResolveResult] = {
    var levelSet = levelMap.get(result)
    if (levelSet == null) {
      levelSet = new ju.HashSet[ScalaResolveResult]()
      levelMap.put(result, levelSet)
    }
    levelSet
  }

  override protected def addResults(results: Iterable[ScalaResolveResult]): Boolean = {
    if (withoutPrecedence) {
      candidatesSet ++= results
      true
    } else super.addResults(results)
  }

  override def changedLevel: Boolean = {
    if (levelMap.isEmpty) return true
    val iterator = levelMap.values().iterator()
    while (iterator.hasNext) {
      val setIterator = iterator.next().iterator()
      while (setIterator.hasNext) {
        candidatesSet = candidatesSet + setIterator.next
      }
    }
    uniqueNamesSet.addAll(levelUniqueNamesSet)
    levelMap.clear()
    levelUniqueNamesSet.clear()
    true
  }

  override def candidatesS: Set[ScalaResolveResult] = {
    val res = candidatesSet
    val iterator = levelMap.values().iterator()
    while (iterator.hasNext) {
      val setIterator = iterator.next().iterator()
      while (setIterator.hasNext) {
        candidatesSet = candidatesSet + setIterator.next
      }
    }
    res
  }

  override protected def isCheckForEqualPrecedence = false

  override def isImplicitProcessor: Boolean = true

  final def candidatesByPlace: Set[ScalaResolveResult] = {
    val isScala3 = getPlace.isInScala3File

    @tailrec
    def treeWalkUp(@Nullable element: PsiElement, @Nullable lastParent: PsiElement): Unit =
      if (element != null &&
        element.processDeclarations(this, ScalaResolveState.empty, lastParent, getPlace)) {

        val shouldStop =
          element match {
            case expr: ScExpression =>
              isScala3 &&
                !expr.contextFunctionParameters.forall(
                  _.forall(
                    this.execute(_, ScalaResolveState.empty)
                  )
                )
            case _ => false
          }

        val isNewLevel = element match {
          case _: ScTemplateBody | _: ScExtendsBlock => true // template body and inherited members are at the same level
          case _                                     => changedLevel
        }

        if (isNewLevel && !shouldStop) {
          treeWalkUp(element.getContext, element)
        }
      }

    treeWalkUp(getPlace, null)
    candidatesS
  }

  final def candidatesByType(expandedType: ScType): Set[ScalaResolveResult] = {
    val isScala3OrEquivalent =
      getPlace.isInScala3File ||
        getPlace.source3Options.isSource3Enabled ||
        getPlace.source3Options.implicitResolution

    val includePackagePrefix =
      !isScala3OrEquivalent || getPlace.isSource3MigrationEnabled


    val objects =
      ImplicitProcessor
        .findImplicitObjects(
          expandedType.removeAliasDefinitions(place = Option(getPlace)),
          getPlace.resolveScope,
          includePackagePrefix
        )

    objects.foreach(objectTpe =>
      processType(objectTpe, getPlace, ScalaResolveState.withImplicitScopeObject(objectTpe))
    )

    candidatesS
  }
}

object ImplicitProcessor {

  def isAccessible(namedElement: PsiNamedElement, place: PsiElement): Boolean =
    (namedElement match {
      case f: ScFunction              => ResolveUtils.isAccessible(f, place)
      case inNameContext(m: ScMember) => ResolveUtils.isAccessible(m, place)
      case _                          => true
    }) && !lowerInFileWithoutType(namedElement, place)

  private def lowerInFileWithoutType(element: PsiElement, place: PsiElement) = {
    val commonContext = PsiTreeUtil.findCommonContext(element, place)

    def lowerInFile =
      strictlyOrderedByContext(
        before   = place,
        after    = element,
        topLevel = Option(commonContext)
      )

    if (place == commonContext) false
    else
      element match {
        case fun: ScFunction if fun.returnTypeElement.isEmpty && !fun.isExtensionMethod => lowerInFile
        case ScalaPsiUtil.inNameContext(pd: ScPatternDefinition)
          if pd.typeElement.isEmpty =>
          lowerInFile
        case _ => false
      }
  }


  private def findImplicitObjects(
    `type`:               ScType,
    scope:                GlobalSearchScope,
    includePackagePrefix: Boolean
  )(implicit
    context: ProjectContext
  ): Seq[ScType] = {
    val implicitObjectsCache = ScalaPsiManager.instance.collectImplicitObjectsCache
    val cacheKey             = (`type`, scope, includePackagePrefix)

    implicitObjectsCache.get(cacheKey) match {
      case null =>
        val implicitObjects = findImplicitObjectsImpl(`type`, includePackagePrefix)(ElementScope(context.project, scope))
        implicitObjectsCache.put(cacheKey, implicitObjects)
        implicitObjects
      case cached => cached
    }
  }

  private[this] def findImplicitObjectsImpl(
    `type`:               ScType,
    includePackagePrefix: Boolean
  )(implicit
    elementScope: ElementScope
  ): Seq[ScType] = {
    val visited = mutable.HashSet.empty[ScType]
    val parts   = mutable.Queue.empty[ScType]

    def collectPartsIterable(iterable: IterableOnce[ScType]): Unit = {
      val iterator = iterable.iterator
      while (iterator.hasNext) {
        collectParts(iterator.next())
      }
    }

    def collectPartsTypeResult(tr: TypeResult): Unit =
      tr.foreach(collectParts)

    // Java Raw types are converted to F[ScExistentialArgument.Deferred("A", .....), ...]
    // In combination with F-Bounds this can lead to different instantiations that are not ==,
    // but would not reveal further parts of the type.
    //
    // Here, we convert such existential arguments to stand-in types that have a useful
    // equals/hashCode implementation, and use this as the marker in the `visitedType` set.
    def convertRawArgs(tp: ScType): ScType = {
      def rawArgToDummy(tp: ScType) = tp match {
        case existentialArgument: ScExistentialArgument =>
          existentialArgument.typeParamOfRawArg match {
            case Some(typeParam) =>
              ScAbstractType(typeParam, existentialArgument.lower, existentialArgument.upper)
            case None =>
              tp
          }
        case tp => tp
      }
      def isRawArg(tp: ScType) = tp match {
        case existentialArgument: ScExistentialArgument =>
          existentialArgument.typeParamOfRawArg.isDefined
        case _ => false
      }
      tp match {
        case ParameterizedType(des, targs) =>
          if (targs.exists(isRawArg)) {
            val targs1 = targs.map(rawArgToDummy)
            ScParameterizedType(des, targs1)
          } else tp
        case _ => tp
      }
    }

    def collectPartsFromSuperTypes(clazz: PsiClass, subst: ScSubstitutor): Unit =
      clazz match {
        case td: ScTemplateDefinition =>
          collectPartsIterable(td.superTypes.map(subst))
          td.selfType.foreach(stpe => collectParts(subst(stpe)))
        case clazz: PsiClass => collectPartsIterable(clazz.getSuperTypes.map(t => subst(t.toScType())))
      }

    /**
     * In scala 3 references to packages and package objects are anchors only under -source:3.0-migration.
     * https://dotty.epfl.ch/3.0.0/docs/reference/changed-features/implicit-resolution.html
     */
    def processPackagePrefix(pack: ScPackageLike): Unit =
      if (includePackagePrefix) {
        for {
          packageObject <- pack.findPackageObject(elementScope.scope)
          designator     = ScDesignatorType(packageObject)
        } parts += designator
        pack.parentScalaPackage.foreach(processPackagePrefix)
      }

    def collectParts(tp: ScType): Unit = {
      ProgressManager.checkCanceled()
      if (!visited.add(convertRawArgs(tp))) return

      tp match {
        case AliasType(alias, _, Right(t)) =>
          alias match {
            case aDef: ScTypeAliasDefinition if aDef.isOpaque => ()
            case _                                            => collectParts(t)
          }
        case _ => ()
      }

      tp match {
        case ScDesignatorType(v: ScBindingPattern) => collectPartsTypeResult(v.`type`())
        case ScDesignatorType(v: ScFieldId)        => collectPartsTypeResult(v.`type`())
        case ScDesignatorType(p: ScParameter)      => collectPartsTypeResult(p.`type`())
        case ScCompoundType(comps, _, _)           => collectPartsIterable(comps)
        case ScAndType(lhs, rhs)                   => collectParts(lhs); collectParts(rhs)
        case ScOrType(lhs, rhs)                    => collectParts(lhs); collectParts(rhs)
        case ScDesignatorType(alias: ScTypeAliasDefinition) if alias.isOpaque        => parts += tp
        case ScDesignatorType(alias: ScTypeAliasDeclaration) if alias.isInScala3File => parts += tp
        case ParameterizedType(a: ScAbstractType, args) =>
          collectParts(a)
          collectPartsIterable(args)
        case p @ ParameterizedType(des, args) =>
          p.extractClassType match {
            case Some((clazz, subst)) =>
              parts += des
              collectParts(des)
              collectPartsIterable(args)
              collectPartsFromSuperTypes(clazz, subst)
            case _ =>
              collectParts(des)
              collectPartsIterable(args)
          }
        case j: JavaArrayType =>
          val parameterizedType = j.getParameterizedType
          collectParts(
            parameterizedType.getOrElse(
              return
            )
          )
        case proj @ ScProjectionType(projected, _) =>
          collectParts(projected)
          proj.actualElement match {
            case v: ScBindingPattern => collectPartsTypeResult(v.`type`().map(proj.actualSubst))
            case v: ScFieldId        => collectPartsTypeResult(v.`type`().map(proj.actualSubst))
            case v: ScParameter      => collectPartsTypeResult(v.`type`().map(proj.actualSubst))
            case v: ScTypeAliasDeclaration if v.isInScala3File => parts += tp
            case _                   =>
          }

          tp.extractClassType match {
            case Some((clazz, subst)) =>
              parts += tp
              collectPartsFromSuperTypes(clazz, subst)
            case _ =>
          }
        case ScAbstractType(_, _, upper) => collectParts(upper)
        case ScExistentialType(quant, _) => collectParts(quant)
        case tpt: TypeParameterType      => collectParts(tpt.upperType)
        case _ =>
          tp.extractClassType match {
            case Some((clazz, subst)) =>
              parts += tp
              val packagePrefix = clazz.parentOfType(classOf[ScPackageLike], strict = false)
              packagePrefix.foreach(processPackagePrefix)
              collectPartsFromSuperTypes(clazz, subst)
            case _ =>
          }
      }
    }

    collectParts(`type`)
    val res = mutable.HashMap.empty[String, Seq[ScType]]

    def addResult(fqn: String, tp: ScType): Unit = {
      res.get(fqn) match {
        case Some(s) =>
          if (s.forall(!_.equiv(tp))) {
            res.remove(fqn)
            res += ((fqn, s :+ tp))
          }
        case None => res += ((fqn, Seq(tp)))
      }
    }

    def workWithTypeAlias(alias: ScTypeAlias, subst: ScSubstitutor = ScSubstitutor.empty): Unit = alias match {
      case alias: ScTypeAliasDefinition =>
        if (alias.isOpaque) {
          for (fqn <- alias.qualifiedNameOpt;
               companionObject <- elementScope.getCachedObject(fqn)) {
            addResult(fqn, ScDesignatorType(companionObject))
          }
        } else {
          collectObjects(subst(alias.aliasedType.getOrAny))
        }
      case declaration: ScTypeAliasDeclaration if declaration.isInScala3File =>
        for (fqn <- alias.qualifiedNameOpt;
             companionObject <- elementScope.getCachedObject(fqn)) {
          addResult(fqn, ScDesignatorType(companionObject))
        }
      case _ =>
    }

    def collectObjects(tp: ScType): Unit =
      tp match {
        case _ if tp.isAny =>
        case tp: StdType if stdTypes.contains(tp.name) =>
          elementScope
            .getCachedObject("scala." + tp.name)
            .foreach(o => addResult(o.qualifiedName, ScDesignatorType(o)))
        case ScDesignatorType(ta: ScTypeAlias) => workWithTypeAlias(ta)
        case ScProjectionType.withActual(ta: ScTypeAlias, actualSubst) => workWithTypeAlias(ta, actualSubst)
        case ParameterizedType(ScDesignatorType(ta: ScTypeAlias), args) =>
          val genericSubst = ScSubstitutor.bind(ta.typeParameters, args)
          workWithTypeAlias(ta, genericSubst)
        case ParameterizedType(ScProjectionType.withActual(ta: ScTypeAliasDefinition, actualSubst), args) =>
          val genericSubst = ScSubstitutor.bind(ta.typeParameters, args)
          val subst        = actualSubst.followed(genericSubst)
          workWithTypeAlias(ta, subst)
        case _ =>
          tp.extractClass match {
            case Some(obj: ScObject) => addResult(obj.qualifiedName, tp)
            case Some(clazz) =>
              getCompanionModule(clazz) match {
                case Some(obj: ScObject) =>
                  tp match {
                    case ScProjectionType(proj, _) =>
                      addResult(obj.qualifiedName, ScProjectionType(proj, obj))
                    case ParameterizedType(ScProjectionType(proj, _), _) =>
                      addResult(obj.qualifiedName, ScProjectionType(proj, obj))
                    case _ =>
                      addResult(obj.qualifiedName, ScDesignatorType(obj))
                  }
                case _ =>
              }
            case _ =>
          }
      }

    while (parts.nonEmpty) {
      collectObjects(parts.dequeue())
    }

    res.values.flatten.toSeq
  }

  private[this] val stdTypes =
    Seq("Int", "Float", "Double", "Boolean", "Byte", "Short", "Long", "Char")

  def isDeclaredOrExportedInExtension(element: PsiNamedElement, state: ResolveState): Boolean =
    element match {
      case fn: ScFunction => fn.isExtensionMethod || state.exportedIn.exists(_.is[ScExtensionBody])
      case _              => false
    }
}