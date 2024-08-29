/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.javasdk.impl.timedaction

import akka.javasdk.timedaction.TimedActionOptions
import java.util

private[akka] final case class TimedActionOptionsImpl(override val forwardHeaders: java.util.Set[String])
    extends TimedActionOptions {
  def withForwardHeaders(headers: util.Set[String]): TimedActionOptions = copy(forwardHeaders = headers)
}