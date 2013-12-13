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

import scala.annotation._

@implicitNotFound("an implicit TimeSystem is required; please import timeSystem.numeric or "+
    "timeSystem.javaUtil")
trait TimeSystem[Instant] {
  type Duration
  def instant(millis: Long): Instant
  def duration(from: Long, to: Long): Duration
  def fromInstant(inst: Instant): Long
}

object timeSystem {
  implicit val numeric = new TimeSystem[Long] {
    type Duration = Long
    def instant(millis: Long): Long = millis
    def duration(from: Long, to: Long): Long = to - from
    def fromInstant(inst: Long): Long = inst
  }

  implicit val javaUtil = new TimeSystem[java.util.Date] {
    import java.util.Date
    type Duration = Long
    def instant(millis: Long) = new Date(millis)
    def duration(from: Long, to: Long): Long = to - from
    def fromInstant(inst: Date): Long = inst.getTime
  }
}
