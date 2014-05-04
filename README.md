[![Build Status](https://travis-ci.org/propensive/rapture-core.png?branch=master)](https://travis-ci.org/propensive/rapture-core)
# Rapture Core

The Rapture Core project provides a common foundation upon which other Rapture projects are
based, however it provides utilities which may be useful in any project. Namely,

 - Return-type strategies
 - A lightweight abstraction on time libraries, and implementations for Java time
 - An alias for `implicitly`
 - Miscellaneous other small tools and utilities

### Availability

Rapture Core 0.9.0 is available under the Apache 2.0 License from Maven Central with group ID `com.propensive` and artifact ID `rapture-core_2.10`.

#### SBT

You can include Rapture Core as a dependency in your own project by adding the following library dependency to your build file:

```scala
libraryDependencies ++= Seq("com.propensive" %% "rapture-core" % "0.9.0")
```

#### Maven

If you use Maven, include the following dependency:

```xml
<dependency>
  <groupId>com.propensive</groupId>
  <artifactId>rapture-core_2.10</artifactId>
  <version>0.9.0<version>
</dependency>
```

#### Download

You can download Rapture Core directly from the [Rapture website](http://rapture.io/)
Rapture Core depends on Scala 2.10, but has no other dependencies.

#### Building from source

To build Rapture Core from source, follow these steps:

```
git clone git@github.com:propensive/rapture-core.git
cd rapture-core
sbt package
```

If the compilation is successful, the compiled JAR file should be found in target/scala-2.10

### Return-Type Strategies

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

#### Writing methods to use return-type strategies

To transform a method like this,

```scala
def doSomething[T](arg: String, arg2: T): Double = {
  // method body
}
```

into one which offers end-users a choice of return-type strategy, include an implicit
ExceptionHandler parameter, and wrap your method body and return type, like this,

```scala
def doSomething[T](arg: String, arg2: T)(implicit eh: ExceptionHandler):
    eh.![Double, Exception] = eh.wrap {
  // method body
}
```

If you know that your method body will only throw exceptions of a certain type, you can
specify this in the method return type in place of `Exception`.

### Time System Abstraction

Many APIs take parameters or return values which represent time. Unfortunately, there is no
standard for representing entities like instants and durations.  Rapture Core provides a general
type class for defining these types and methods for creating them, and provides two simple
implementations:

- `timeSystems.numeric` - uses `Long`s to represent both instants and durations.
- `timeSystems.javaUtil` - uses `java.util.Date`s to represent instants, and `Long`s to
  represent durations.

### Alias for `implicitly`

Context-bounds provide a nice, lightweight syntax for working with type classes in Scala,
however while explicitly specifying an implicit parameter necessarily provides a named handle
for that implicit, context-bounds force us to make repeated use of the `implicitly` method in
order to use the type class. This can make using context-bounds more cumbersome than they
deserve.

Rapture Core introduces an alias for `implicitly` named `?`. Generally speaking, any occurrence
of `implicitly` can be replaced by a `?`. This is particularly useful when calling methods which
take multiple implicit parameters and you would like to specify one of these explicitly. For
example, a method like this:

```scala
def performAction[T](v: Int)(implicit alpha: Alpha[T], beta: Beta, gamma: Gamma) = { ... }
```

may now be called quite concisely using

```scala
performAction(42)(?, ?, myGamma)
```

if we only wanted to specify the parameter `myGamma` explicitly.
