/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.views;

import akka.javasdk.view.TableUpdater;
import akka.javasdk.annotations.DeleteHandler;
import com.example.wiring.keyvalueentities.user.User;
import com.example.wiring.keyvalueentities.user.UserEntity;
import akka.javasdk.annotations.Query;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.view.View;

import java.util.List;

@ComponentId("users_by_name")
public class UsersByName extends View {

  @Consume.FromKeyValueEntity(value = UserEntity.class)
  public static class Users extends TableUpdater<User> {
    @DeleteHandler
    public Effect<User> onDelete() {
      return effects().deleteRow();
    }
  }

  public record QueryParameters(String name) {}
  public record UserList(List<User> users) {}

  @Query("SELECT * AS users FROM users WHERE name = :name")
  public QueryEffect<UserList> getUsers(QueryParameters params) {
    return queryResult();
  }
}