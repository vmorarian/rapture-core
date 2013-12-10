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
package rapture

import scala.collection.mutable.HashMap
import scala.collection.generic._

import language.higherKinds

package object core {

  type implicitNotFound = annotation.implicitNotFound

  implicit val implicitConversions = language.implicitConversions

  def repeat[T](blk: => T) = new Repeater[T](blk)
  class Repeater[T](blk: => T) { def until(test: T => Boolean) = {
    var t: T = blk
    while(!test(t)) t = blk
    t
  } }

  def yCombinator[A, B](fn: (A => B) => (A => B)): A => B = fn(yCombinator(fn))(_)

  /** Times how long it takes to perform an operation, returning a pair of the result and the
    * duration of the operation in milliseconds. */
  def time[T](blk: => T): (T, Long) = {
    val t = System.currentTimeMillis
    blk -> (System.currentTimeMillis - t)
  }
 
  def yielding[T](result: T)(fn: => Unit) = {
    fn
    result
  }

  @inline implicit class SeqExtras[A, C[A] <: Seq[A]](val xs: C[A]) {

    /** Inserts an element between each of the elements of the sequence. */
    def intersperse[B >: A, That](between: B)(implicit bf: CanBuildFrom[C[A], B, That]):
        That = {
      val b = bf(xs)
      xs.init foreach { x =>
        b += x
        b += between
      }
      b += xs.last
      b.result
    }

    /** Inserts an element between each of the elements of the sequence, and additionally
      * prepends and affixes the sequence with `before` and `after`. */
    def intersperse[B >: A, That](before: B, between: B, after: B)
        (implicit bf: CanBuildFrom[C[A], B, That]): That = {
      val b = bf(xs)
      b += before
      xs.init foreach { x =>
        b += x
        b += between
      }
      b += xs.last
      b += after
      b.result
    }

    /** Convenience method for zipping a sequence with a value derived from each element. */
    def zipWith[T](fn: A => T)(implicit bf: CanBuildFrom[C[A], (A, T), C[(A, T)]]):
        C[(A, T)] = {
      val b = bf(xs)
      xs.foreach { x => b += (x -> fn(x)) }
      b.result
    }
  }

  /** Convenience method for forking a block of code to a new thread */
  def fork(blk: => Unit): Thread = {
    val th = new Thread {
      override def run() = {
        blk
        join()
      }
    }
    th.start()
    th
  }

  implicit val booleanParser: StandardParser[Boolean] =
    StandardParser(s => if(s == "true") Some(true) else if(s == "false") Some(false) else None)

  implicit val byteParser: StandardParser[Byte] =
    StandardParser(s => try Some(s.toByte) catch { case e: NumberFormatException => None })
  
  implicit val charParser: StandardParser[Char] =
    StandardParser(s => if(s.length == 1) Some(s(0)) else None)
  
  implicit val shortParser: StandardParser[Short] =
    StandardParser(s => try Some(s.toShort) catch { case e: NumberFormatException => None })
  
  implicit val intParser: StandardParser[Int] =
    StandardParser(s => try Some(s.toInt) catch { case e: NumberFormatException => None })
  
  implicit val longParser: StandardParser[Long] =
    StandardParser(s => try Some(s.toLong) catch { case e: NumberFormatException => None })
  
  implicit val stringParser: StandardParser[String] = StandardParser(s => Some(s))

  implicit val doubleParser: StandardParser[Double] =
    StandardParser(s => try Some(s.toDouble) catch { case e: NumberFormatException => None })

  implicit val floatParser: StandardParser[Float] =
    StandardParser(s => try Some(s.toFloat) catch { case e: NumberFormatException => None })
}
