package org.jetbrains.plugins.scala.lang.psi.types.api

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.plugins.scala.Tracing
import org.jetbrains.plugins.scala.caches.RecursionManager
import org.jetbrains.plugins.scala.caches.stats.{CacheCapabilities, CacheTracker, Tracer}
import org.jetbrains.plugins.scala.lang.psi.types.Context.{Location, TrackingContext}
import org.jetbrains.plugins.scala.lang.psi.types.api.Equivalence._
import org.jetbrains.plugins.scala.lang.psi.types.{ConstraintSystem, ConstraintsResult, Context, ScType}

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import scala.util.DynamicVariable

trait Equivalence {
  typeSystem: TypeSystem =>

  import ConstraintsResult.Left
  import TypeSystem._

  private val guard = RecursionManager.RecursionGuard[Key, ConstraintsResult](s"${typeSystem.name}.equivalence.guard")

  private val cache: ConcurrentHashMap[Key, Map[Location, ConstraintsResult]] = {
    val cache = new ConcurrentHashMap[Key, Map[Location, ConstraintsResult]]()
    CacheTracker.alwaysTrack(equivInnerTraceId, equivInnerTraceId)(this: Equivalence)
    cache
  }

  private val eval = new DynamicVariable(false)

  final def equiv(left: ScType, right: ScType)(implicit context: Context): Boolean = equivInner(left, right).isRight

  def clearCache(): Unit = cache.clear()

  /**
    * @param falseUndef use false to consider undef type equals to any type
    */
  final def equivInner(left: ScType, right: ScType,
                       constraints: ConstraintSystem = ConstraintSystem.empty,
                       falseUndef: Boolean = true)(implicit context: Context): ConstraintsResult = {
    ProgressManager.checkCanceled()

    if (left == right) constraints
    else if (left.canBeSameClass(right)) {
      val result = equivInner(Key(left, right, falseUndef))
      combine(result)(constraints)
    } else Left
  }

  protected def equivComputable(key: Key)(implicit context: Context): Supplier[ConstraintsResult]

  private def equivInner(key: Key)(implicit context: Context): ConstraintsResult = {
    val tracer = Tracer(equivInnerTraceId, equivInnerTraceId)
    tracer.invocation()

    val nowEval = eval.value

    val cachedLocationToResult =
      if (nowEval) None
      else eval.withValue(true)(Option(cache.get(key)))

    val cachedResult = cachedLocationToResult.flatMap { locationToResult =>
      val matchingLocation = locationToResult.keys.find { location =>
        location.forall { case (opaqueTypeAlias, isInScope) => context.isInScopeOf(opaqueTypeAlias) == isInScope }
      }
      matchingLocation.flatMap(locationToResult.get)
    }

    val result = cachedResult.orElse {
      guard.doPreventingRecursion(key) {
        val stackStamp = RecursionManager.markStack()

        tracer.calculationStart()
        try {
          val ctx = new TrackingContext()
          val computedResult = equivComputable(key)(ctx).get()
          Tracing.equivalence(key.left, key.right, computedResult)
          if (!nowEval && stackStamp.mayCacheNow()) {
            eval.withValue(true) {
              val updatedLocationToResult = cachedLocationToResult.getOrElse(Map.empty).updated(ctx.usedOpaqueTypeAliases, computedResult)
              cache.put(key, updatedLocationToResult)
            }
          }
          computedResult
        } finally {
          tracer.calculationEnd()
        }
      }
    }

    result.getOrElse(Left)
  }
}

object Equivalence {
  val equivInnerTraceId: String = "Equivalence.equivInner"

  implicit val EquivInnerCacheCapabilities: CacheCapabilities[Equivalence] =
    new CacheCapabilities[Equivalence] {
      override def cachedEntitiesCount(cache: CacheType): Int = cache.cache.size()
      override def clear(cache: CacheType): Unit = cache.clearCache()
    }
}