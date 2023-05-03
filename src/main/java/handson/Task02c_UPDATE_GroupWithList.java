package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.*;
import com.commercetools.api.models.customer_group.*;
import handson.impl.CustomerService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static handson.impl.ApiPrefixHelper.API_DEV_CLIENT_PREFIX;
import static handson.impl.ClientService.createApiClient;

public class Task02c_UPDATE_GroupWithList {

    public static void main(String[] args) {
        try (GroupListDemo demo = new GroupListDemo()) {
            demo.run();
        } catch (IOException e) {
            System.exit(-1);
        } catch (ExecutionException e) {
            System.exit(-2);
        } catch (InterruptedException e) {
            System.exit(-3);
        }
    }
}

class GroupListDemo implements Closeable {
    private static final CustomerRecord CUSTOMER_1 = new CustomerRecord(
            "first-group-customer",
            "first-group-customer@mail.com",
            "pass-1st$0",
            "John",
            "Monday",
            "UA"
    );
    private static final CustomerRecord CUSTOMER_2 = new CustomerRecord(
            "second-group-customer",
            "second-group-customer@mail.com",
            "pass$02nd#2",
            "Mary",
            "Tuesday",
            "UA"
    );

    private static final GroupRecord GROUP_DATA = new GroupRecord(
            "Target Group",
            "target-group-key"
    );
    private final ProjectApiRoot client;
    private final CustomerService customerService;

    private final Logger logger = LoggerFactory.getLogger(GroupListDemo.class);

    GroupListDemo() throws IOException {
        client = createApiClient(API_DEV_CLIENT_PREFIX.getPrefix());
        customerService = new CustomerService(client);
    }

    void run() throws ExecutionException, InterruptedException {
        CompletableFuture<Customer> customer1Future = ensureCustomer(CUSTOMER_1);
        CompletableFuture<Customer> customer2Future = ensureCustomer(CUSTOMER_2);
        CompletableFuture<CustomerGroup> groupFuture = ensureGroup(GROUP_DATA);
        Pair<Customer, Customer> customers = groupFuture
                .thenApplyAsync(
                        group -> Collections.<CustomerUpdateAction>singletonList(
                                CustomerSetCustomerGroupActionBuilder.of()
                                        .customerGroup(
                                                CustomerGroupResourceIdentifierBuilder.of()
                                                        .id(group.getId())
                                                        .build()
                                        )
                                        .build()
                        )
                )
                .thenCombineAsync(
                        customer1Future,
                        (setGroupList, customer) -> client.customers()
                                .withId(customer.getId())
                                .post(
                                        CustomerUpdateBuilder.of()
                                                .version(customer.getVersion())
                                                .actions(setGroupList)
                                                .build()
                                )
                                .execute()
                                .thenApplyAsync(ApiHttpResponse::getBody)
                                .thenCombineAsync(
                                        customer2Future,
                                        (customer1, customer2) -> client.customers()
                                                .withId(customer2.getId())
                                                .post(
                                                        CustomerUpdateBuilder.of()
                                                                .version(customer2.getVersion())
                                                                .actions(setGroupList)
                                                                .build()
                                                )
                                                .execute()
                                                .thenApplyAsync(resp -> new ImmutablePair<Customer, Customer>(customer1, resp.getBody()))
                                )
                )
                .toCompletableFuture()
                .get()
                .toCompletableFuture()
                .get()
                .toCompletableFuture()
                .get();

        logger.info(
                "\nCustomer #1: key = {}; group = {}\nCustomer #2: key = {}; group = {}",
                customers.getLeft().getKey(), customers.getLeft().getCustomerGroup().getId(),
                customers.getRight().getKey(), customers.getRight().getCustomerGroup().getId()
        );
    }

    CompletableFuture<Customer> ensureCustomer(CustomerRecord customerRecord) {
        return customerService.getCustomerByKey(customerRecord.key)
                .exceptionally(t -> {
                    logger.error("An exception occurred: {}", t.getMessage());
                    return new ApiHttpResponse<Customer>(HttpStatusCode.OK_200, null, null);
                })
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenComposeAsync(customer -> customer == null
                        ? customerService.createCustomer(
                                customerRecord.email,
                                customerRecord.password,
                                customerRecord.key,
                                customerRecord.firstName,
                                customerRecord.lastName,
                                customerRecord.country)
                        .thenApplyAsync(ApiHttpResponse::getBody)
                        .thenApplyAsync(CustomerSignInResult::getCustomer)
                        : CompletableFuture.supplyAsync(() -> customer));
    }

    CompletableFuture<CustomerGroup> ensureGroup(GroupRecord groupRecord) {
        return customerService.getCustomerGroupByKey(groupRecord.key)
                .exceptionally(t -> {
                    logger.error("An exception occurred: {}", t.getMessage());
                    return new ApiHttpResponse<CustomerGroup>(HttpStatusCode.OK_200, null, null);
                })
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenComposeAsync(
                        customerGroup ->
                                customerGroup == null ? customerService.createGroup(groupRecord.key, groupRecord.name)
                                        .thenApplyAsync(ApiHttpResponse::getBody)
                                        : CompletableFuture.supplyAsync(() -> customerGroup)
                );
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private static class CustomerRecord {
        final String key;
        final String email;
        final String password;
        final String firstName;
        final String lastName;
        final String country;

        public CustomerRecord(String key, String email, String password, String firstName, String lastName, String country) {
            this.key = key;
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
            this.country = country;
        }
    }

    private static class GroupRecord {
        final String name;
        final String key;

        public GroupRecord(String name, String key) {
            this.name = name;
            this.key = key;
        }
    }
}
