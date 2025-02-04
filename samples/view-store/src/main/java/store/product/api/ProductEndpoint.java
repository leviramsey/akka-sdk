package store.product.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import store.product.application.ProductEntity;
import store.product.domain.Product;

import java.util.concurrent.CompletionStage;

import static akka.javasdk.http.HttpResponses.created;

@HttpEndpoint("/products")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class ProductEndpoint {
  private final ComponentClient componentClient;

  public ProductEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/{productId}")
  public CompletionStage<HttpResponse> create(String productId, Product product) {
    return componentClient.forEventSourcedEntity(productId)
      .method(ProductEntity::create)
      .invokeAsync(product)
      .thenApply(__ -> created());
  }

  @Get("/{productId}")
  public CompletionStage<Product> get(String productId) {
    return componentClient.forEventSourcedEntity(productId)
      .method(ProductEntity::get)
      .invokeAsync();
  }
}
