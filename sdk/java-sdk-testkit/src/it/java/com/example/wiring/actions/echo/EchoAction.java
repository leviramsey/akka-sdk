/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.actions.echo;

import akka.platform.javasdk.action.Action;
import akka.platform.javasdk.action.ActionCreationContext;
import akka.platform.javasdk.annotations.ActionId;
import akka.platform.javasdk.client.ComponentClient;

@ActionId("echo")
public class EchoAction extends Action {

  private Parrot parrot;
  private ActionCreationContext ctx;
  private final ComponentClient componentClient;

  public EchoAction(ActionCreationContext ctx,  ComponentClient componentClient) {
    this.parrot = new Parrot();
    this.ctx = ctx;
    this.componentClient = componentClient;
  }

  public Effect<Message> stringMessage(String msg) {
    String response = this.parrot.repeat(msg);
    return effects().reply(new Message(response));
  }
}