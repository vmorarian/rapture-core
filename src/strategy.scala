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
trait Rts[+Group] { rts =>
  type Wrap[+_, _ <: Exception]
  def wrap[T, E <: Exception: ClassTag](t: => T): Wrap[T, E]

  def compose[Group2](rts2: Rts[Group2]) = new Rts[Group] {
    type Wrap[+T, E <: Exception] = rts.Wrap[rts2.Wrap[T, E], E]
    def wrap[T, E <: Exception: ClassTag](t: => T): Wrap[T, E] =
      rts.wrap(rts2.wrap(t))
  }
}

object raw extends strategy.ThrowExceptions

object strategy {
  
  implicit def throwExceptions[Group] = new ThrowExceptions[Group]
  class ThrowExceptions[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T = t
  }

  implicit def explicit[Group] = new ExplicitReturns[Group]
  class ExplicitReturns[+Group] extends Rts[Group] {
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

  implicit def captureExceptions[Group] = new CaptureExceptions[Group]
  class CaptureExceptions[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = Either[E, T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Either[E, T] =
      try Right(t) catch {
        case e: E => Left(e)
        case e: Throwable => throw e
      }

    override def toString = "[strategy.captureExceptions]"
  }

  implicit def returnTry[Group] = new ReturnTry[Group]
  class ReturnTry[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = Try[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Try[T] = Try(t)
    
    override def toString = "[strategy.returnTry]"
  }

  implicit def kcaco[Group] = new Kcaco[Group]
  class Kcaco[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = T
    def wrap[T, E <: Exception: ClassTag](t: => T): T =
      try t catch { case e: Exception => null.asInstanceOf[T] }

    override def toString = "[strategy.kcaco]"
  }

  implicit def discardExceptions[Group] = new DiscardExceptions[Group]
  class DiscardExceptions[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = Option[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Option[T] =
      try Some(t) catch { case e: Exception => None }

    override def toString = "[strategy.discardExceptions]"
  }

  implicit def returnFutures[Group](implicit ec: ExecutionContext) = new ReturnFutures[Group]
  class ReturnFutures[+Group](implicit ec: ExecutionContext) extends Rts[Group] {
    type Wrap[+T, E <: Exception] = Future[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Future[T] = Future { t }

    override def toString = "[strategy.returnFutures]"
  }

  implicit def timeExecution[D: TimeSystem.ByDuration, Group] = new TimeExecution[D, Group]
  class TimeExecution[D: TimeSystem.ByDuration, +Group] extends Rts[Group] {
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

  implicit def useDefaults[Group] = new UseDefaults[Group]
  class UseDefaults[+Group] extends Rts[Group] {
    type Wrap[+T, E <: Exception] = Defaulting[T]
    def wrap[T, E <: Exception: ClassTag](t: => T): Defaulting[T] = new Defaulting(t)
    override def toString = "[strategy.useDefaults]"
  }*/
}
