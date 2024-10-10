package customer.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.annotations.http.Put;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpException;
import customer.application.CustomerEntity;
import customer.domain.Address;
import customer.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

// Opened up for access from the public internet to make the sample service easy to try out.
// For actual services meant for production this must be carefully considered, and often set more limited
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/customer")
public class CustomerEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEndpoint.class);

    private final ComponentClient componentClient;

    public CustomerEndpoint(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @Post("/{id}")
    public CompletionStage<CustomerEntity.Ok> create(String id, Customer customer) {
        logger.info("Request to create customer {}", customer);
        if (customer.name() == null || customer.name().isEmpty()) {
            throw HttpException.badRequest("Customer name must not be empty");
        }
        return componentClient.forKeyValueEntity(id)
                .method(CustomerEntity::create)
                .invokeAsync(customer);
    }


    @Get("/{id}")
    public CompletionStage<Customer> get(String id) {
        return componentClient.forKeyValueEntity(id)
                .method(CustomerEntity::getCustomer)
                .invokeAsync();
    }


    @Put("/{id}/name")
    public CompletionStage<CustomerEntity.Ok> changeName(String id, String newName) {
        logger.info("Request to change name for customer [{}] to [{}]", id, newName);
        if (newName.isEmpty()) {
            throw HttpException.badRequest("Customer name must not be empty");
        }
        return componentClient.forKeyValueEntity(id)
                .method(CustomerEntity::changeName)
                .invokeAsync(newName);
    }


    @Get("/{id}/address")
    public CompletionStage<Address> getAddress(String id) {
        return componentClient.forKeyValueEntity(id)
            .method(CustomerEntity::getCustomer)
            .invokeAsync().thenApply(Customer::address);
    }


    @Put("/{id}/address")
    public CompletionStage<CustomerEntity.Ok> changeAddress(String id, Address newAddress) {
        logger.info("Request to change address for customer [{}] to [{}]", id, newAddress);
        return componentClient.forKeyValueEntity(id)
                .method(CustomerEntity::changeAddress)
                .invokeAsync(newAddress);
    }

}
