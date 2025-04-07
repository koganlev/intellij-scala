package org.jetbrains.plugins.scala.lang.psi.types.api

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.scala.Tracing
import org.jetbrains.plugins.scala.caches.RecursionManager
import org.jetbrains.plugins.scala.caches.stats.{CacheCapabilities, CacheTracker, Tracer}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.types.Context.{Location, TrackingContext}
import org.jetbrains.plugins.scala.lang.psi.types.api.Conformance._
import org.jetbrains.plugins.scala.lang.psi.types.{ConstraintSystem, ConstraintsResult, Context, ScType}

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

trait Conformance {
  typeSystem: TypeSystem =>

  import TypeSystem._
  import org.jetbrains.plugins.scala.lang.psi.types.ConstraintsResult.Left

  private val guard = RecursionManager.RecursionGuard[Key, ConstraintsResult](s"${typeSystem.name}.conformance.guard")

  private val cache: ConcurrentHashMap[Key, Map[Location, ConstraintsResult]] = {
    val cache = new ConcurrentHashMap[Key, Map[Location, ConstraintsResult]]()
    CacheTracker.alwaysTrack(conformsInnerCache, conformsInnerCache)(cache)
    cache
  }

  /**
    * Checks, whether the following assignment is correct:
    * val x: l = (y: r)
    */
  final def conformsInner(left: ScType, right: ScType,
                          visited: Set[PsiClass] = Set.empty,
                          constraints: ConstraintSystem = ConstraintSystem.empty,
                          checkWeak: Boolean = false)(implicit context: Context): ConstraintsResult = {
    ProgressManager.checkCanceled()

    if (left.isAny || left.isAnyKind || left.is[WildcardType] || right.isNothing || left == right)
      constraints
    else if (right.canBeSameOrInheritor(left)) {
      val result = conformsInner(Key(left, right, checkWeak), visited)
      combine(result)(constraints)
    } else if (left.isTupleBaseType) {
      if (right.isTupleN) constraints
      else                Left
    } else Left
  }

  def clearCache(): Unit = cache.clear()

  protected def conformsComputable(key: Key, visited: Set[PsiClass])(implicit context: Context): Supplier[ConstraintsResult]

  def conformsInner(key: Key, visited: Set[PsiClass])(implicit context: Context): ConstraintsResult = {
    val tracer = Tracer(conformsInnerCache, conformsInnerCache)
    tracer.invocation()

    val cachedLocationToResult = Option(cache.get(key))

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
          val computedResult = conformsComputable(key, visited)(ctx).get()
          Tracing.conformance(key.left, key.right, computedResult)
          if (stackStamp.mayCacheNow()) {
            val updatedLocationToResult = cachedLocationToResult.getOrElse(Map.empty).updated(ctx.usedOpaqueTypeAliases, computedResult)
            cache.put(key, updatedLocationToResult)
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

object Conformance {
  val conformsInnerCache: String = "Conformance.conformsInner"
  implicit def ConformanceCacheCapabilities[T]: CacheCapabilities[ConcurrentHashMap[T, Map[Location, ConstraintsResult]]] =
    new CacheCapabilities[ConcurrentHashMap[T, Map[Location, ConstraintsResult]]] {
      override def cachedEntitiesCount(cache: CacheType): Int = cache.size()
      override def clear(cache: CacheType): Unit = cache.clear()
    }
}
