package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import handson.impl.ApiPrefixHelper;
import handson.impl.CustomerService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.ApiClientException;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static handson.impl.ClientService.createApiClient;


public class Task09a_ERROR_HANDLING {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        final String apiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();

        final ProjectApiRoot client = createApiClient(apiClientPrefix);
        Logger logger = LoggerFactory.getLogger(Task09a_ERROR_HANDLING.class.getName());

        CustomerService customerService = new CustomerService(client);

        // TODO:
        //  Provide a WRONG or CORRECT customer key
        //
        final String customerKeyMayOrMayNotExist = "customer-michele-WRONG-KEY";

        // TODO: Handle 4XX errors, exceptions
        //  Use CompletionStage
        //
        logger.info("Customer fetch: {}",
                customerService.getCustomerByKey(customerKeyMayOrMayNotExist)
                        .exceptionally(t -> {
                            logger.error("An error occurred: {}", t.getMessage());
                            int statusCode = t instanceof ApiClientException ? ((ApiClientException) t).getStatusCode() : HttpStatusCode.OK_200;
                            return new ApiHttpResponse<Customer>(statusCode, null, null);
                        })
                        .thenApplyAsync(ApiHttpResponse::getBody)
                        .toCompletableFuture()
                        .get()
        );


        // TODO: Handle 4XX errors, exceptions
        //  Use Optionals, Either (Java 9+)
        //
        Optional<Customer> optionalCustomer = Optional.ofNullable(
                customerService
                        .getCustomerByKey(customerKeyMayOrMayNotExist)
                        .thenApply(ApiHttpResponse::getBody)
                        .exceptionally(throwable -> null)
                        .toCompletableFuture()
                        .get()
        );

        // Handle now
        logger.info("Customer with key {} {}", customerKeyMayOrMayNotExist, optionalCustomer.map(c -> "exists").orElse("doesn't exist"));


        logger.info("Customer fetch: {}",
                customerService.getCustomerByKey(customerKeyMayOrMayNotExist)
                        .handle((ApiHttpResponse<Customer> cutomerResponse, Throwable t) -> {

                            if (t instanceof ApiClientException || ExceptionUtils.getRootCause(t) instanceof ApiClientException) {
                                logger.error("An exception was caught: {}", t.getMessage());
                                return Optional.<Customer>empty();
                            } else if (t != null) {
                                logger.error("An unknown error was caught: {}", t.getMessage());
                                return Optional.<Customer>empty();
                            }
                            return Optional.of(cutomerResponse.getBody());
                        })
                        .toCompletableFuture()
                        .get()
                        .map(Customer::getId)
                        .orElse("<not exists>")
        );

    }
}
