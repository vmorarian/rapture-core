/**********************************************************************************************\
* Rapture Core Library                                                                         *
* Version 0.9.0                                                                                *
*                                                                                              *
* The primary distribution site is                                                             *
*                                                                                              *
*   http://rapture.io/                                                                         *
*                                                                                              *
* Copyright 2010-2013 Propensive Ltd.                                                          *
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

@implicitNotFound(msg = "No exception handler was available. Please import "+
  "a member of rapture.io.strategy, e.g. strategy.throwExceptions.")
trait ExceptionHandler { eh =>
  type ![_, _ <: Exception]
  def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): ![T, E]

  def compose(eh2: ExceptionHandler) = new ExceptionHandler {
    type ![T, E <: Exception] = eh.![eh2.![T, E], E]
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): ![T, E] =
      eh.wrap(eh2.wrap(t))
  }
}

object raw extends strategy.ThrowExceptions

object strategy {
  
  implicit def throwExceptions = new ThrowExceptions
  class ThrowExceptions extends ExceptionHandler {
    type ![T, E <: Exception] = T
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): T = t
  }

  implicit def captureExceptions = new CaptureExceptions
  class CaptureExceptions extends ExceptionHandler {
    type ![T, E <: Exception] = Either[E, T]
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): Either[E, T] =
      try Right(t) catch {
        case e: E => Left(e)
        case e: Throwable => throw e
      }
  }

  implicit def returnTry = new ReturnTry
  class ReturnTry extends ExceptionHandler {
    type ![T, E <: Exception] = Try[T]
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): Try[T] = Try(t)
    
  }

  implicit val kcaco = new Kcaco
  class Kcaco extends ExceptionHandler {
    type ![T, E <: Exception] = T
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): T =
      try t catch { case e: Exception => null.asInstanceOf[T] }
  }

  implicit def returnFutures(implicit ec: ExecutionContext) = new ReturnFutures
  class ReturnFutures(implicit ec: ExecutionContext) extends ExceptionHandler {
    type ![T, E <: Exception] = Future[T]
    def wrap[T, E <: Exception](t: => T)(implicit mf: ClassTag[E]): Future[T] = Future { t }
  }

  implicit def timeExecution[I: TimeSystem] = new TimeExecution
  class TimeExecution[I: TimeSystem] extends ExceptionHandler {
    val ts = implicitly[TimeSystem[I]]
    type ![T, E <: Exception] = (T, ts.Duration)
    def wrap[T, E <: Exception](r: => T)(implicit mf: ClassTag[E]): (T, ts.Duration) = {
      val t0 = System.currentTimeMillis
      (r, ts.duration(t0, System.currentTimeMillis))
    }
  }
}
