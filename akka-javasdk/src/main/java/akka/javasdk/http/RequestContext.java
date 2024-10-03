/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.javasdk.http;

import akka.annotation.DoNotInherit;
import akka.javasdk.Context;
import akka.javasdk.Principals;

/**
 * * Not for user extension, can be injected as constructor parameter into HTTP endpoint components
 */
@DoNotInherit
public interface RequestContext extends Context {

  /**
   * Get the principals associated with this request.
   *
   * @return The principals associated with this request.
   */
  Principals getPrincipals();
}