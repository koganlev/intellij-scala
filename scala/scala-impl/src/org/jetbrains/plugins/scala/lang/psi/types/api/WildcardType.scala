package org.jetbrains.plugins.scala.lang.psi.types.api

import org.jetbrains.plugins.scala.lang.psi.types.{LeafType, ScalaTypeVisitor}
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.NonValueType
import org.jetbrains.plugins.scala.project.ProjectContext

/**
  * Special type used to represent higher-kinded unbounded [[org.jetbrains.plugins.scala.lang.psi.types.ScAbstractType]].
  * Should only be used as a designator in [[org.jetbrains.plugins.scala.lang.psi.types.ScParameterizedType]]
  * and never outside of conformance checks.
  * Used to avoid code duplication between [[org.jetbrains.plugins.scala.lang.psi.types.ScAbstractType]]
  * and [[UndefinedType]] (e.g. partial unification).
  *
  * DO NOT USE THIS, UNLESS YOU KNOW WHAT YOU ARE DOING
  */
final case class WildcardType(tparam: TypeParameter) extends NonValueType with LeafType {
  override def inferValueType: ValueType               = TypeParameterType(tparam)

  override def visitType(visitor: ScalaTypeVisitor): Unit = {}

  override implicit def projectContext: ProjectContext = tparam.psiTypeParameter
}
