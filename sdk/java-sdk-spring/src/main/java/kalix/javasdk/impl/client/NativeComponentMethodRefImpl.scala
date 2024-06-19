/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.client

import akka.NotUsed
import akka.annotation.InternalApi
import kalix.javasdk.DeferredCall
import kalix.javasdk.Metadata
import kalix.javasdk.client.NativeComponentMethodRef
import kalix.javasdk.client.NativeComponentMethodRef1

import java.util.concurrent.CompletionStage

/**
 * INTERNAL API
 */
// FIXME native as in calling component directly without going over HTTP (vs existing ComponentMethodRef which is doing REST:y calls)
//       replace with reasonable name eventually
// FIXME get rid of the JavaPbAny
@InternalApi
final case class NativeComponentMethodRefImpl[A1, R](
    optionalId: Option[String],
    metadataOpt: Option[Metadata],
    createDeferred: (Option[Metadata], Option[A1]) => DeferredCall[A1, R])
    extends NativeComponentMethodRef[R]
    with NativeComponentMethodRef1[A1, R] {

  override def withMetadata(metadata: Metadata): NativeComponentMethodRefImpl[A1, R] = {
    val merged = metadataOpt.map[Metadata](m => m.merge(metadata)).getOrElse(metadata)
    copy(metadataOpt = Some(merged))
  }

  def deferred(): DeferredCall[NotUsed, R] =
    createDeferred(metadataOpt, None).asInstanceOf[DeferredCall[NotUsed, R]]

  def invokeAsync(): CompletionStage[R] =
    deferred().invokeAsync()

  def deferred(arg: A1): DeferredCall[A1, R] =
    createDeferred(metadataOpt, Some(arg))

  def invokeAsync(arg: A1): CompletionStage[R] =
    deferred(arg).invokeAsync()

}