[![Build Status](https://travis-ci.org/propensive/rapture-core.png?branch=master)](https://travis-ci.org/propensive/rapture-core)
Rapture Core
============

The Rapture Core project provides a common foundation upon which other Rapture projects are
based, however it provides utilities which may be useful in any project. Namely,

 - Generalized return-type strategy
 - A lightweight abstraction on time libraries, and implementations for Java time
 - An alias for `implicitly`
 - Miscellaneous other small tools and utilities

Return-Type Strategies
----------------------

Rapture's return-type strategies allow library methods to be written in such a way that they may
be wrapped in another function (and thus have a different return type) at the call site. This
pattern allows users of the library to choose the return type and additional pre- and
post-execution processing to be performed, depending on their needs.  For example, using Rapture
JSON, given one of the imported return-type strategies,

```scala
import strategy.captureExceptions
Json.parse("[1, 2, 3]")
```

will have return type `Either[ParseException, Json]`. Hopefully the parsing succeeded, and the
return type will be `Right[Json]` rather than `Left[ParseException]`.

Alternatively, given a different imported strategy, we will get a different return type.

```scala
import strategy.returnFutures
Json.parse("[1, 2, 3]")
```

This will immediately return a `Future[Json]`, from which the result can be obtained once
processing completes.

A selection of return-type strategies are provided:

- `strategy.throwExceptions` - does no additional processing, and simply returns the value,
  leaving any thrown exceptions unhandled.
- `strategy.captureExceptions` - captures successful results in the `Right` branch of an
  `Either`, or exceptions in the `Left` branch.
- `strategy.discardExceptions` - returns an `Option` of the result, where the exceptional case
  collapses to `None`.
- `strategy.returnTry` - wraps the result in a `scala.util.Try`.
- `strategy.returnFutures` - wraps the result in a `scala.concurrent.Future`; requires an
  implicit ExecutionContext.
- `strategy.timeExecution` - times the duration of carrying out the execution, returning a tuple
  of the return value and the time taken; requires an implicit `rapture.core.TimeSystem`.
- `strategy.kcaco` - "Keep calm and carry on" - catches exceptions and silently returns them as
  null; this is strongly discouraged!
- `strategy.explicit` - returns an instance of `Explicit` which requires the return-type
  strategy to be explicitly specified at the call site every time.

Multiple strategies can be composed, should this be required, for example,

```scala
implicit val handler = strategy.returnTry compose strategy.timeExecution
```

Time System Abstraction
-----------------------

Many APIs take parameters or return values which represent time. Unfortunately, there is no
standard for representing entities like instants and durations.  Rapture Core provides a general
type class for defining these types and methods for creating them, and provides two simple
implementations:

- `timeSystem.numeric` - uses `Long`s to represent both instants and durations.
- `timeSystem.javaUtil` - uses `java.util.Date`s to represent instants, and `Long`s to
  represent durations.

Alias for `implicitly`
----------------------

Context-bounds provide a nice, lightweight syntax for working with type classes in Scala,
however while explicitly specifying an implicit parameter necessarily provides a named handle
for that implicit, context-bounds force us to make repeated use of the `implicitly` method in
order to use the type class. This can make using context-bounds more cumbersome than they
deserve.

Rapture Core introduces an alias for `implicitly` named `?`. Generally speaking, any occurrence
of `implicitly` can be replaced by a `?`.
