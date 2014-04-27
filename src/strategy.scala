/**********************************************************************************************\
* Rapture Core Library                                                                         *
* Version 0.9.0                                                                                *
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

@implicitNotFound(msg = "No return-type strategy was available. Please import "+
  "a member of rapture.core.strategy, e.g. strategy.throwExceptions.")
trait Rts { rts =>
  type Wrap[+_, _ <: Exception]
  def wrap[T, E <: Exception: ClassTag](t: => T): Wrap[T, E]

  def compose(rts2: Rts) = new Rts {
    type Wrap[+T, E <: Exception] = rts.Wrap[rts2.Wrap[T, E], E]
    def wrap[T, E <: Exception: ClassTag](t: => T): Wrap[T, E] =
      rts.wrap(rts2.wrap(t))
  }
}

object raw extends strategy.ThrowExceptions

object strategy {
  
  implicit def throwExceptions = new ThrowExceptions
  class ThrowExceptions extends Rts {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T = t
  }

  implicit def explicit = new ExplicitReturns
  class ExplicitReturns extends Rts {
    type Wrap[+T, E <: Exception] = Explicit[T, E]
    def wrap[T, E <: Exception: ClassTag](t: => T): Explicit[T, E] =
      new Explicit[T, E](t)
  }

  class Explicit[+T, E <: Exception: ClassTag](t: => T) {
    def get: T = t
    def opt: Option[T] = discardExceptions.wrap(t)
    def getOrElse[T2 >: T](t: T2): T2 = opt.getOrElse(t)
    //def default[T](implicit default: Default[T]) = useDefaults.wrap(t).apply()
    def either: Either[E, T] = captureExceptions.wrap(t)
    def attempt: Try[T] = returnTry.wrap(t)
    def time[D: TimeSystem.ByDuration] = timeExecution.wrap(t)
    def future(implicit ec: ExecutionContext): Future[T] = returnFutures.wrap(t)
  
    override def toString = "[unexpanded result]"
  }

  implicit def captureExceptions = new CaptureExceptions
  class CaptureExceptions extends Rts {
    type Wrap[+T, E <: Exception] = Either[E, T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Either[E, T] =
      try Right(t) catch {
        case e: E => Left(e)
        case e: Throwable => throw e
      }

    override def toString = "[strategy.captureExceptions]"
  }

  implicit def returnTry = new ReturnTry
  class ReturnTry extends Rts {
    type Wrap[+T, E <: Exception] = Try[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Try[T] = Try(t)
    
    override def toString = "[strategy.returnTry]"
  }

  implicit val kcaco = new Kcaco
  class Kcaco extends Rts {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T =
      try t catch { case e: Exception => null.asInstanceOf[T] }

    override def toString = "[strategy.kcaco]"
  }

  implicit val discardExceptions = new DiscardExceptions
  class DiscardExceptions extends Rts {
    type Wrap[+T, E <: Exception] = Option[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Option[T] =
      try Some(t) catch { case e: Exception => None }

    override def toString = "[strategy.discardExceptions]"
  }

  implicit def returnFutures(implicit ec: ExecutionContext) = new ReturnFutures
  class ReturnFutures(implicit ec: ExecutionContext) extends Rts {
    type Wrap[+T, E <: Exception] = Future[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Future[T] = Future { t }

    override def toString = "[strategy.returnFutures]"
  }

  implicit def timeExecution[D: TimeSystem.ByDuration] = new TimeExecution[D]
  class TimeExecution[D: TimeSystem.ByDuration] extends Rts {
    val ts = ?[TimeSystem.ByDuration[D]]
    type Wrap[+T, E <: Exception] = (T, D)
    def wrap[T, E <: Exception: ClassTag](r: => T): (T, D) = {
      val t0 = System.currentTimeMillis
      (r, ts.duration(t0, System.currentTimeMillis))
    }
    
    override def toString = "[strategy.timeExecution]"
  }

  /*class Defaulting[-T](t: => T) {
    def apply[T]()(implicit default: Default[T]) =
      try t catch { case e: Exception => ?[Default[T]].default }
  }

  implicit def useDefaults = new UseDefaults
  class UseDefaults extends Rts {
    type Wrap[+T, E <: Exception] = Defaulting[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Defaulting[T] = new Defaulting(t)
    override def toString = "[strategy.useDefaults]"
  }*/
}
