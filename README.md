Rapture Core
============

The Rapture Core project provides a common foundation upon which other Rapture projects are
based, however it provides utilities which may be useful in any project. Namely,

 - Generalized return-type handlers
 - A lightweight abstraction on time libraries, and implementations for Java time
 - Miscellaneous other utilities

Return-Type Handlers
--------------------

Rapture's return-type handlers allow library methods to be written in such a way that they may
be wrapped in another function (and thus have a different return type) at the call site. This
pattern allows users of the library to choose the return type and additional pre- and
post-execution processing to be performed, depending on their needs.  For example, using Rapture
JSON, given one of the imported return-type handlers,

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

This will immediately return a `Future[Json]`.

Six return-type strategies are provided:

- `strategy.throwExceptions` - does no additional processing, and simply returns the value,
  leaving any thrown exceptions unhandled.
- `strategy.captureExceptions` - captures successful results in the `Right` branch of an
  `Either`, or exceptions in the `Left` branch.
- `strategy.returnTry` - wraps the result in a `scala.util.Try`.
- `strategy.returnFutures` - wraps the result in a `scala.concurrent.Future`; requires an
  implicit ExecutionContext.
- `strategy.timeExecution` - times the duration of carrying out the execution, returning a tuple
  of the return value and the time taken; requires an implicit `rapture.core.TimeSystem`.
- `strategy.kcaco` - "Keep calm and carry on" - catches exceptions and silently returns them as
  null; strongly discouraged!

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
