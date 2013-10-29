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

import scala.reflect.ClassTag
import scala.util.Try

import scala.concurrent._

@implicitNotFound(msg = "No exception handler was available. Please import "+
  "a member of rapture.io.strategy, e.g. strategy.throwExceptions.")
trait ExceptionHandler {
  type ![_ <: Exception, _]
  def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): ![E, T]
}

object strategy {
  
  class ThrowExceptions extends ExceptionHandler {
    type ![E <: Exception, T] = T
    def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): T = t
  }

  implicit def throwExceptions = new ThrowExceptions

  class CaptureExceptions extends ExceptionHandler {
    type ![E <: Exception, T] = Either[E, T]
    def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): Either[E, T] =
      try Right(t) catch {
        case e: E => Left(e)
        case e: Throwable => throw e
      }
    
  }

  implicit def returnTry = new ReturnTry
  
  class ReturnTry extends ExceptionHandler {
    type ![E <: Exception, T] = Try[T]
    def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): Try[T] = Try(t)
    
  }

  implicit def captureExceptions = new CaptureExceptions

  implicit def returnFutures(implicit ec: ExecutionContext) = new ExceptionHandler {
    type ![E <: Exception, T] = Future[T]
    def except[E <: Exception, T](t: => T)(implicit mf: ClassTag[E]): Future[T] = Future { t }
  }
}

sealed trait IoException extends Exception

sealed trait GeneralIoExceptions extends IoException
sealed trait HttpExceptions extends IoException
sealed trait CryptoExceptions extends IoException

case class InterruptedIo() extends GeneralIoExceptions

sealed trait NotFoundExceptions extends GeneralIoExceptions

case class NotFound() extends NotFoundExceptions with HttpExceptions

case class Forbidden() extends HttpExceptions
case class TooManyRedirects() extends HttpExceptions
case class BadHttpResponse() extends HttpExceptions
case class DecryptionException() extends CryptoExceptions

case class ParseException(source: String, line: Option[Int] = None, column: Option[Int] = None)
    extends Exception {
  override def toString = "Failed to parse source"
}
