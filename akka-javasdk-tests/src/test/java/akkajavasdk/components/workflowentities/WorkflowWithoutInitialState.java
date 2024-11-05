/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package akkajavasdk.components.workflowentities;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.workflow.Workflow;

import java.util.concurrent.CompletableFuture;

@ComponentId("workflow-without-initial-state")
public class WorkflowWithoutInitialState extends Workflow<String> {


  @Override
  public WorkflowDef<String> definition() {
    var test =
        step("test")
            .asyncCall(() -> CompletableFuture.completedFuture("ok"))
            .andThen(String.class, result -> effects().updateState("success").end());

    return workflow()
        .addStep(test);
  }

  public Effect<String> start() {
    return effects().transitionTo("test").thenReply("ok");
  }

  public Effect<String> get() {
    if (currentState() == null) {
      return effects().reply("empty");
    } else {
      return effects().reply(currentState());
    }
  }
}