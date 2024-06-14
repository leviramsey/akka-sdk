package com.example;

import com.example.actions.CounterCommandFromTopicAction;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.awaitility.Awaitility;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::class[]
public class CounterIntegrationWithRealPubSubTest extends KalixIntegrationTestKitSupport { // <1>

// end::class[]


  @Override
  protected KalixTestKit.Settings kalixTestKitSettings() {
    return KalixTestKit.Settings.DEFAULT.withAclEnabled()
      .withEventingSupport(KalixTestKit.Settings.EventingSupport.GOOGLE_PUBSUB);
  }

  @Test
  public void verifyCounterEventSourcedConsumesFromPubSub() {
    WebClient pubsubClient = WebClient.builder()
      .baseUrl("http://localhost:8085")
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build();

    var counterId = "testRealPubSub";
    var messageBody = buildMessageBody(
      "{\"counterId\":\"" + counterId + "\",\"value\":20}",
      CounterCommandFromTopicAction.IncreaseCounter.class.getName());

    var projectId = "test";
    var injectMsgResult = pubsubClient.post()
      .uri("/v1/projects/{projectId}/topics/counter-commands:publish", projectId)
      .bodyValue(messageBody)
      .retrieve()
      .toBodilessEntity().block();
    assertTrue(injectMsgResult.getStatusCode().is2xxSuccessful());

    var getCounterState =
      componentClient.forEventSourcedEntity(counterId)
        .method(Counter::get);

    Awaitility.await()
      .ignoreExceptions()
      .atMost(20, TimeUnit.SECONDS)
      .until(
        () -> getCounterState.invokeAsync().toCompletableFuture().get(1, TimeUnit.SECONDS),
        new IsEqual("\"20\""));
  }
  // end::test-topic[]

  // builds a message in PubSub format, ready to be injected
  private String buildMessageBody(String jsonMsg, String ceType) {
    var data = Base64.getEncoder().encodeToString(jsonMsg.getBytes());

    return """
      {
          "messages": [
              {
                  "data": "%s",
                  "attributes": {
                      "Content-Type": "application/json",
                      "ce-specversion": "1.0",
                      "ce-type": "%s"
                  }
              }
          ]
      }
      """.formatted(data, ceType);
  }

// tag::class[]
}
// end::class[]