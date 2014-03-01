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

@implicitNotFound("No default value for type ${T}")
case class Default[+T](default: T)

object defaults {
  object primitivesZero {
    implicit val longZero: Default[Long] = Default(0L)
    implicit val intZero: Default[Int] = Default(0)
    implicit val shortZero: Default[Short] = Default(0)
    implicit val byteZero: Default[Byte] = Default(0)
    implicit val doubleZero: Default[Double] = Default(0.0)
    implicit val floatZero: Default[Float] = Default(0.0f)
    implicit val booleanFalse: Default[Boolean] = Default(false)
  }

  implicit val emptyString: Default[String] = Default[String]("")
}
