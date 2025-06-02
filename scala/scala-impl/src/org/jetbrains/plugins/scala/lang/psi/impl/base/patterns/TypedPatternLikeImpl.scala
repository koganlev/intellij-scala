package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScPattern
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

trait TypedPatternLikeImpl extends ScPattern { this: Typeable =>

  final override def isIrrefutableForImpl(scrutineeType: ScType, deep: Boolean): Boolean =
    (scrutineeType.isAny ||
      `type`().forall(scrutineeType.conforms(_))
      ) &&
      (!deep || subpatterns.forall(_.isIrrefutableFor(scrutineeType, deep)))
}
