/**********************************************************************************************\
* Rapture Core Library                                                                         *
* Version 0.10.0                                                                               *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2014 Jon Pretty, Propensive Ltd.                                              *
*                                                                                              *
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file    *
* except in compliance with the License. You may obtain a copy of the License at               *
*                                                                                              *
*   http://www.apache.org/licenses/LICENSE-2.0                                                 *
*                                                                                              *
* Unless required by applicable law or agreed to in writing, software distributed under the    *
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,    *
* either express or implied. See the License for the specific language governing permissions   *
* and limitations under the License.                                                           *
\**********************************************************************************************/
package rapture.core

import language.higherKinds

import scala.annotation._

import scala.reflect._
import scala.util._

import scala.concurrent._

trait ModeGroup

trait LowPriorityMode {
  implicit def defaultMode = new modes.ThrowExceptions
}

object Mode extends LowPriorityMode

@implicitNotFound(msg = "No implicit mode was available for ${Group} methods. "+
    "Please import a member of rapture.core.modes, e.g. modes.throwExceptions.")
trait Mode[+Group <: ModeGroup] { mode =>
  type Wrap[+_, _ <: Exception]
  def wrap[Result, E <: Exception: ClassTag](blk: => Result): Wrap[Result, E]

  def compose[Group2 <: ModeGroup](mode2: Mode[Group2]) = new Mode[Group] {
    type Wrap[+Result, E <: Exception] = mode.Wrap[mode2.Wrap[Result, E], E]
    def wrap[Result, E <: Exception: ClassTag](blk: => Result): Wrap[Result, E] =
      mode.wrap(mode2.wrap(blk))
  }
}

object raw extends modes.ThrowExceptions

object repl {
  
  var showStackTraces: Boolean = false
  private var lastExceptionValue: Throwable = new SilentException

  def lastException: Nothing = throw lastExceptionValue

  implicit def replMode[Group <: ModeGroup] = new Repl[Group]
  
  class SilentException extends Throwable {
    override def printStackTrace(pw: java.io.PrintWriter) = ()
  }

  class Repl[+Group <: ModeGroup] extends Mode[Group] {
    type Wrap[+Return, E <: Exception] = Return
    def wrap[Return, E <: Exception: ClassTag](blk: => Return): Return = try blk catch {
      case e: Exception => if(showStackTraces) throw e else {
        println("Execution failed with exception: "+e.toString)
        print("For the full stacktrace, see repl.lastException.")
        lastExceptionValue = e
        throw new SilentException()
      }
    }
  }
}

object modes {

  implicit def throwExceptions[G <: ModeGroup] = new ThrowExceptions[G]
  class ThrowExceptions[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T = t
  }

  implicit def explicit[G <: ModeGroup] = new ExplicitReturns[G]
  class ExplicitReturns[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = Explicit[T, E]
    def wrap[T, E <: Exception: ClassTag](t: => T): Explicit[T, E] =
      new Explicit[T, E](t)
  }

  class Explicit[+Result, E <: Exception: ClassTag](blk: => Result) {
    def get: Result = blk
    def opt: Option[Result] = discardExceptions.wrap(blk)
    def getOrElse[Result2 >: Result](t: Result2): Result2 = opt.getOrElse(blk)
    //def default(implicit default: Default[Result]) = useDefaults.wrap(blk).apply()
    def either: Either[E, Result] = captureExceptions.wrap(blk)
    def attempt: Try[Result] = returnTry.wrap(blk)
    def backoff(maxRetries: Int = 10, initialPause: Long = 1000L,
        backoffRate: Double = 2.0): Result =
      new ExponentialBackoff(maxRetries, initialPause, backoffRate).wrap(blk)
    def time[D: TimeSystem.ByDuration] = timeExecution.wrap(blk)
    def future(implicit ec: ExecutionContext): Future[Result] = returnFutures.wrap(blk)
  
    override def toString = "[unexpanded result]"
  }

  implicit def captureExceptions[Group <: ModeGroup] = new CaptureExceptions[Group]
  class CaptureExceptions[+Group <: ModeGroup] extends Mode[Group] {
    type Wrap[+Result, E <: Exception] = Either[E, Result]
    def wrap[Result, E <: Exception: ClassTag](blk: => Result): Either[E, Result] =
      try Right(blk) catch {
        case e: E => Left(e)
        case e: Throwable => throw e
      }

    override def toString = "[modes.captureExceptions]"
  }

  implicit def returnTry[G <: ModeGroup] = new ReturnTry[G]
  class ReturnTry[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = Try[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Try[T] = Try(t)
    
    override def toString = "[modes.returnTry]"
  }

  implicit def exponentialBackoff[G <: ModeGroup] = new ExponentialBackoff[G]()
  class ExponentialBackoff[+G <: ModeGroup](maxRetries: Int = 10, initialPause: Long = 1000L,
      backoffRate: Double = 2.0) extends Mode[G] {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T = {
      var multiplier = 1.0
      var count = 1
      var result: T = null.asInstanceOf[T]
      var exception: Exception = null.asInstanceOf[Exception]
      while(result == null && count < maxRetries) try { result = t } catch {
        case e: Exception =>
          exception = e
          Thread.sleep((multiplier*initialPause).toLong)
          multiplier *= backoffRate
          count += 1
      }
      if(result != null) result else throw exception
    }
  }

  implicit def kcaco[G <: ModeGroup] = new Kcaco[G]
  class Kcaco[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T =
      try t catch { case e: Exception => null.asInstanceOf[T] }

    override def toString = "[modes.kcaco]"
  }

  implicit def discardExceptions[G <: ModeGroup] = new DiscardExceptions[G]
  class DiscardExceptions[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = Option[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Option[T] =
      try Some(t) catch { case e: Exception => None }

    override def toString = "[modes.discardExceptions]"
  }

  implicit def returnFutures[G <: ModeGroup](implicit ec: ExecutionContext) =
    new ReturnFutures[G]
  
  class ReturnFutures[+G <: ModeGroup](implicit ec: ExecutionContext) extends Mode[G] {
    type Wrap[+T, E <: Exception] = Future[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Future[T] = Future { t }

    override def toString = "[modes.returnFutures]"
  }

  implicit def timeExecution[D: TimeSystem.ByDuration, G <: ModeGroup] =
    new TimeExecution[D, G]
  class TimeExecution[D: TimeSystem.ByDuration, +G <: ModeGroup] extends Mode[G] {
    val ts = ?[TimeSystem.ByDuration[D]]
    type Wrap[+T, E <: Exception] = (T, D)
    def wrap[T, E <: Exception: ClassTag](r: => T): (T, D) = {
      val t0 = System.currentTimeMillis
      (r, ts.duration(t0, System.currentTimeMillis))
    }
    
    override def toString = "[modes.timeExecution]"
  }

  /*class Defaulting[-T](t: => T) {
    def apply[T]()(implicit default: Default[T]) =
      try t catch { case e: Exception => ?[Default[T]].default }
  }

  implicit def useDefaults[G <: ModeGroup] = new UseDefaults[G]
  class UseDefaults[+G <: ModeGroup] extends Mode[G] {
    type Wrap[+T, E <: Exception] = Defaulting[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Defaulting[T] = new Defaulting(t)
    override def toString = "[modes.useDefaults]"
  }*/
}
