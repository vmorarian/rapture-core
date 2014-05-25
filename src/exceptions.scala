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
    extends Exception { override def toString = "Failed to parse source" }
