/*
 * Copyright 2012 Pellucid and Zenexity
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aws.core

import play.api.libs.ws.{ Response => WSResponse }

import aws.core.parsers._
import aws.core.parsers.Parser._


/**
 * Metadata returned in a response wether the result was an error or not. Usually related to the request.
 */
trait Metadata
case object EmptyMeta extends Metadata

/**
 * An AWS Result, that can be a success on a error. M represents the metadata (some information always there
 * whether the result is a success or an error) and T represents the type of the body in case of a success.
 *
 * Some services don't have any metadata with responses, in this case `EmptyMeta` will be returned.
 *
 * To consume it, you can use pattern matching:
 * {{{
 *   r match {
 *     case Result(metadata, body) => // Do something with the body
 *     case Errors(metadata, errors) => // Do something with the errors
 *   }
 * }}}
 */
sealed trait Result[M <: Metadata, +T] {
  /**
   * The metadata, `EmptyMeta` if the service doesn't support metadata.
   */
  def metadata: M
  /**
   * Return the body if a success, throws an exception if an error. May be null if the response is empty.
   */
  def body: T
  def toEither: Either[AWSError[M], Result[M, T]]
  def map[T2](f: (T) => T2): Result[M, T2]
  def flatMap[T2](f: (T) => Result[M, T2]): Result[M, T2]
  def foreach(f: (T => Unit)): Unit
  override def toString = "Result(%s, %s)".format(metadata, body)
}

object Result {
  def apply[M <: Metadata, T](m: M = EmptyMeta, b: T): Result[M, T] = new Result[M, T] {
    override def toEither = Right(this)
    override def metadata = m
    override def body = b
    override def map[T2](f: (T) => T2) = Result(this.metadata, f(this.body))
    override def flatMap[T2](f: (T) => Result[M, T2]) = f(this.body)
    override def foreach(f: (T => Unit)) = f(b)
  }
  def unapply[M <: Metadata, T](r: Result[M, T]): Option[(M, T)] = r match {
    case AWSError(_, _, _) => None
    case _ => Some(r.metadata -> r.body)
  }

}

object EmptyResult {
  def apply[M <: Metadata](m: M = EmptyMeta) = Result.apply(m, ())
}

class AWSError[M <: Metadata](val metadata: M, val code: String, val message: String) extends Result[M, Nothing] {
  override def toEither = Left(this)
  override def map[T2](f: (Nothing) => T2) = this
  override def flatMap[T2](f: (Nothing) => Result[M, T2]) = this
  override def body = throw new RuntimeException(this.toString)
  override def foreach(f: (Nothing => Unit)) = ()
  override def toString = ("AWSError: " + code + " - " + message)
}

object AWSError {
  def apply[M <: Metadata](metadata: M = EmptyMeta, code: String, message: String) = new AWSError(metadata, code, message)
  def unapply[M <: Metadata](e: AWSError[M]): Option[(M, String, String)] = Some((e.metadata, e.code, e.message))
}
// TODO: AWS sometimes returns a 200 when there is an error (example: NoSuchDomain error for DomainMetadata)
