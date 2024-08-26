/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.headers;

import akka.platform.javasdk.timedaction.TimedAction;
import akka.platform.javasdk.annotations.ComponentId;

/**
 * Action with the same name in a different package.
 */
@ComponentId("echo2")
public class EchoAction extends TimedAction {

  public Effect stringMessage(String msg) {
    return effects().done();
  }
}
