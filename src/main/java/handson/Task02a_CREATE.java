package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerSignInResult;
import handson.impl.ApiPrefixHelper;
import handson.impl.ClientService;
import handson.impl.CustomerService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static handson.impl.ClientService.createApiClient;


/**
 * Configure sphere client and get project information.
 * <p>
 * See:
 *  TODO dev.properties
 *  TODO {@link ClientService#createApiClient(String prefix)}
 */
public class Task02a_CREATE {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        /**
         * TODO:
         * UPDATE the ApiPrefixHelper with your prefix from dev.properties (e.g. "mh-dev-admin.")
         */
        final String apiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();

        Logger logger = LoggerFactory.getLogger(Task02a_CREATE.class.getName());
        final ProjectApiRoot client = createApiClient(apiClientPrefix);
        CustomerService customerService = new CustomerService(client);
        Customer customer = null;

        try {
            customer = customerService.getCustomerByKey("mykola-czyslin")
                    .toCompletableFuture()
                    .get()
                    .getBody();
            logger.info("Customer fetch: [key: {}; firstName: {}; lastName: {}; email: {}]",
                    customer.getKey(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getEmail()
            );
        } catch (NotFoundException e) {
            logger.info("Fail to find customer by key ");
        }

        // TODO:
        //  CREATE a customer
        //  CREATE a email verification token
        //  Verify customer
        //
        if (customer == null) {
            customer = customerService.createCustomer(
                            "mykola_czyslin@epam.com",
                            "__aaaBbbbcCccccD",
                            "mykola-czyslin",
                            "Mykola",
                            "Czyslin",
                            "UA"
                    )
                    .thenComposeAsync(r -> customerService.createEmailVerificationToken(r, 5L))
                    .thenComposeAsync(r -> customerService.verifyEmail(r))
                    .toCompletableFuture()
                    .get()
                    .getBody();
            logger.info(
                    "Customer created: [key: {}; firstName: {}; lastName: {}; email: {}]",
                    customer.getKey(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getEmail()
            );
        } else {
            logger.info("Customer already exists");
        }


        client.close();
    }
}
