package org.jetbrains.plugins.scala.text.scala3

import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.text.TextToTextTestBase

class ScalatestTest extends TextToTextTestBase(
  Seq(
    "org.scalatest" %% "scalatest" % "3.2.14"
  ),
  Seq("org.scalatest"), Set.empty, 660,
  Set(
    "org.scalatest.enablers.InspectorAsserting", // Tuple2 type argument
    "org.scalatest.matchers.must.Matchers", // No this. prefix
    "org.scalatest.matchers.should.Matchers", // No this. prefix
    "org.scalatest.tools.Framework", // Any
    "org.scalatest.tools.ScalaTestAntTask", // Cannot resolve reference
    "org.scalatest.tools.ScalaTestFramework", // Any
  ),
  withSources = true,
  Set(
    "org.scalatest.AsyncSuperEngine", // Predef.Set
    "org.scalatest.CompileMacro", // with Product & Serializable
    "org.scalatest.FixtureTestSuite", // Predef.Set
    "org.scalatest.PathEngine", // scala.List
    "org.scalatest.Suite", // Predef.String
    "org.scalatest.SuperEngine", // Predef.Set
    "org.scalatest.concurrent.PimpedThreadGroup", // scala.List
    "org.scalatest.diagrams.DiagramsMacro", // Cannot resolve x$1.reflect.Term
    "org.scalatest.events.AlertProvided", // extends Event, NotificationEvent
    "org.scalatest.events.DiscoveryCompleted", // Event vs DiscoveryCompleted
    "org.scalatest.events.DiscoveryStarting", // Event vs DiscoveryStarting
    "org.scalatest.events.Event", // Object vs Any
    "org.scalatest.events.InfoProvided", // extends Event, RecordableEvent
    "org.scalatest.events.MarkupProvided", // Event vs MarkupProvided
    "org.scalatest.events.MotionToSuppress", // final case object
    "org.scalatest.events.NoteProvided", // Event vs NoteProvided
    "org.scalatest.events.RunAborted", // Event vs RunAborted
    "org.scalatest.events.RunCompleted", // Event vs RunCompleted
    "org.scalatest.events.RunStarting", // Event vs RunStarting
    "org.scalatest.events.RunStopped", // Event vs RunStopped
    "org.scalatest.events.ScopeClosed", // Event vs ScopeClosed
    "org.scalatest.events.ScopeOpened", // Event vs ScopeOpened
    "org.scalatest.events.ScopePending", // Event vs ScopePending
    "org.scalatest.events.SeeStackDepthException", // final case object
    "org.scalatest.events.SuiteAborted", // Event vs SuiteAborted
    "org.scalatest.events.SuiteCompleted", // Event vs SuiteCompleted
    "org.scalatest.events.SuiteStarting", // Event vs SuiteStarting
    "org.scalatest.events.TestCanceled", // Event vs TestCanceled
    "org.scalatest.events.TestFailed", // Event vs TestFailed
    "org.scalatest.events.TestIgnored", // Event vs TestIgnored
    "org.scalatest.events.TestPending", // Event vs TestPending
    "org.scalatest.events.TestStarting", // Event vs TestStarting
    "org.scalatest.events.TestSucceeded", // Event vs TestSucceeded
    "org.scalatest.exceptions.NotSerializableWrapperException", // case class with Serializable
    "org.scalatest.matchers.AMatcher", // with Object { toString }
    "org.scalatest.matchers.AnMatcher", // with Object { toString }
    "org.scalatest.matchers.CompileMacro", // with Product & Serializable
    "org.scalatest.matchers.Matcher", // T with T, T with Any, Expr[...]
    "org.scalatest.matchers.TypeMatcherMacro", // Cannot resolve x$1.reflect.Term
    "org.scalatest.matchers.dsl.EndWithWord", // with Object { toString }
    "org.scalatest.matchers.dsl.FullyMatchWord", // with Object { toString }
    "org.scalatest.matchers.dsl.IncludeWord", // with Object { toString }
    "org.scalatest.matchers.dsl.MatchPatternWord", // Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory1", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory2", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory3", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory4", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory5", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory6", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory7", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory8", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.MatcherFactory9", // SC with SC, TC1 vs Nothing, Expr[...]
    "org.scalatest.matchers.dsl.NotWord", // Expr[...]
    "org.scalatest.matchers.dsl.ResultOfNotWordForAny", // Expr[...]
    "org.scalatest.matchers.dsl.StartWithWord", // with Object { toString }
    "org.scalatest.matchers.must.TypeMatcherMacro", // Cannot resolve x$1.reflect.Term
    "org.scalatest.matchers.should.TypeMatcherMacro", // Cannot resolve x$1.reflect.Term
    "org.scalatest.tools.ArgsParser", // Predef.String
    "org.scalatest.tools.DiscoverySuite", // Predef.String
    "org.scalatest.tools.NestedSuiteElement", // Predef.String
    "org.scalatest.tools.PrettyPrinter", // class BrokenException()
    "org.scalatest.tools.ProgressBarPanel", // empty () constructor
    "org.scalatest.tools.StringReporter", // Unicode \u001b char
    "org.scalatest.tools.SuiteElement", // Predef.String
    "org.scalatest.wordspec.AsyncWordSpecLike", // Expr[...]
  )
)