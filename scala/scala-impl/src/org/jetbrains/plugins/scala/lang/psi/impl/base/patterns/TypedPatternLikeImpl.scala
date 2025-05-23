package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScPattern
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result.Typeable

trait TypedPatternLikeImpl extends ScPattern { this: Typeable =>

  final override def isIrrefutableForImpl(scrutineeType: ScType, deep: Boolean): Boolean =
    `type`().forall { ty =>
      scrutineeType.conforms(ty) && (!deep || subpatterns.forall(_.isIrrefutableFor(scrutineeType, deep)))
    }
}
