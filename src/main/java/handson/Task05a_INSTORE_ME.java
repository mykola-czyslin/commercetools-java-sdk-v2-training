package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartBuilder;
import com.commercetools.api.models.cart.CartDraftBuilder;
import com.commercetools.api.models.common.BaseAddressBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.me.MyCartDraftBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import handson.impl.ApiPrefixHelper;

import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static handson.impl.ClientService.*;


/**
 *
 */
public class Task05a_INSTORE_ME {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        try (InStoreDemo demo = new InStoreDemo(Arrays.stream(args).anyMatch("+err"::equalsIgnoreCase))) {
            demo.runDemo();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}

class InStoreDemo implements Closeable {
    public static final String STORE_KEY = "mc-store-lviv";
    public static final String ONLY_STORE_CUSTOMER_KEY = "mykola-czyslin-store-lviv-global";

    public static final String GLOBAL_CUSTOMER_KEY = "mykola-czyslin-global";
    public static final String PASS_PHRASE = "__$1password!!!";

    private final Logger logger = LoggerFactory.getLogger(Task05a_INSTORE_ME.class.getName());
    private ProjectApiRoot globalClient = null;

    private ProjectApiRoot storeClient = null;

    private ProjectApiRoot meClient = null;

    private final boolean testErroneousCases;
    private ProjectApiRoot storeMeClient;

    InStoreDemo(boolean testErroneousCases) {
        this.testErroneousCases = testErroneousCases;
    }

    public void runDemo() throws IOException, ExecutionException, InterruptedException {

        //  Create in-store cart with global API client
        //  Provide an API client with global permissions
        //  Provide a customer who is restricted to a store
        //  Note: A global cart creation should fail but an in-store cart should work
        //
        Cart inStoreCartThroughGlobal = createGlobalInStoreCart();

        logger.info("Created in-store cart with a global api client: {}", inStoreCartThroughGlobal.getId());


        //  Create in-store Cart with in-store API client
        //  Update the ApiPrefixHelper with the prefix for Store API Client
        //  Provide an API client with scope limited to a store
        //  Provide a customer with only store permissions
        //  Try creating a global cart with a global customer and check the error message

        Cart inStoreCartInStore = createInStoreCart();
        logger.info("Created in-store cart with a store api client: {}", inStoreCartInStore.getId());

        //  Verify on impex that the carts are holding the same information
        //
        logger.info(
                "\nid\tcustomer id\te-mail\tgroup\tstore\tcountry\tcurrency\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n",
                inStoreCartThroughGlobal.getId(),
                inStoreCartThroughGlobal.getCustomerId(),
                inStoreCartThroughGlobal.getCustomerEmail(),
                inStoreCartThroughGlobal.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartThroughGlobal.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartThroughGlobal.getCountry(),
                inStoreCartThroughGlobal.getTotalPrice().getCurrencyCode(),
                inStoreCartInStore.getId(),
                inStoreCartInStore.getCustomerId(),
                inStoreCartInStore.getCustomerEmail(),
                inStoreCartInStore.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartInStore.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartInStore.getCountry(),
                inStoreCartInStore.getTotalPrice().getCurrencyCode()
        );


        //  Create a cart via /me endpoint
        //  Provide API client with SPA for customer with global permissions
        //  Update the ApiPrefixHelper with the prefix for Me(SPA) API Client
        //  You can also create in-store customer-bound cart
        //  Visit impex to inspect the carts created

        Cart globalCustomerBoundCart = createGlobalCustomerBoundCart();
        logger.info("Get cart for customer via me endpoint: " +
                globalCustomerBoundCart.getId()
        );

        logger.info(
                "\nid\tCustomerId\te-mail\tgroup\tstore\tcountry\tcurrency\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n",
                inStoreCartThroughGlobal.getId(),
                inStoreCartThroughGlobal.getCustomerId(),
                inStoreCartThroughGlobal.getCustomerEmail(),
                inStoreCartThroughGlobal.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartThroughGlobal.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartThroughGlobal.getCountry(),
                inStoreCartThroughGlobal.getTotalPrice().getCurrencyCode(),
                inStoreCartInStore.getId(),
                inStoreCartInStore.getCustomerId(),
                inStoreCartInStore.getCustomerEmail(),
                inStoreCartInStore.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartInStore.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartInStore.getCountry(),
                inStoreCartInStore.getTotalPrice().getCurrencyCode(),
                globalCustomerBoundCart.getId(),
                globalCustomerBoundCart.getCustomerId(),
                globalCustomerBoundCart.getCustomerEmail(),
                globalCustomerBoundCart.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                globalCustomerBoundCart.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                globalCustomerBoundCart.getCountry(),
                globalCustomerBoundCart.getTotalPrice().getCurrencyCode()
        );

        //  Create in-store customer-bound Cart with in-store-me API client
        //  Update the ApiPrefixHelper with the prefix for Me(SPA) API Client
        //  Provide in-store-me API client with scope for a store and me endpoint
        //  Try creating a global cart without me and check the error message
        //  Visit impex to inspect the carts created
        Cart inStoreCustomerBoundCart = createInStoreCustomerBoundCart();

        logger.info("Created in-store cart with a store api client: " +
                inStoreCustomerBoundCart.getId()
        );
        logger.info(
                "\nid\tCustomerId\te-mail\tgroup\tstore\tcountry\tcurrency\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n{}\t{}\t{}\t{}\t{}\t{}\t{}\n",
                inStoreCartThroughGlobal.getId(),
                inStoreCartThroughGlobal.getCustomerId(),
                inStoreCartThroughGlobal.getCustomerEmail(),
                inStoreCartThroughGlobal.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartThroughGlobal.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartThroughGlobal.getCountry(),
                inStoreCartThroughGlobal.getTotalPrice().getCurrencyCode(),
                inStoreCartInStore.getId(),
                inStoreCartInStore.getCustomerId(),
                inStoreCartInStore.getCustomerEmail(),
                inStoreCartInStore.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCartInStore.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCartInStore.getCountry(),
                inStoreCartInStore.getTotalPrice().getCurrencyCode(),
                globalCustomerBoundCart.getId(),
                globalCustomerBoundCart.getCustomerId(),
                globalCustomerBoundCart.getCustomerEmail(),
                globalCustomerBoundCart.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                globalCustomerBoundCart.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                globalCustomerBoundCart.getCountry(),
                globalCustomerBoundCart.getTotalPrice().getCurrencyCode(),
                inStoreCustomerBoundCart.getId(),
                inStoreCustomerBoundCart.getCustomerId(),
                inStoreCustomerBoundCart.getCustomerEmail(),
                inStoreCustomerBoundCart.getCustomerGroup() == null ? null : inStoreCartThroughGlobal.getCustomerGroup().getTypeId() + ":" + inStoreCartThroughGlobal.getCustomerGroup().getId(),
                inStoreCustomerBoundCart.getStore() == null ? null : inStoreCartThroughGlobal.getStore().getTypeId() + ":" + inStoreCartThroughGlobal.getStore().getKey(),
                inStoreCustomerBoundCart.getCountry(),
                inStoreCustomerBoundCart.getTotalPrice().getCurrencyCode()
        );
    }


    private Cart createGlobalInStoreCart() throws InterruptedException, ExecutionException, IOException {
        initGlobalApiClient();
        //  Create in-store cart with global API client
        //  Provide an API client with global permissions
        //  Provide a customer who is restricted to a store
        //  Note: A global cart creation should fail but an in-store cart should world
        //
        final Customer storeCustomer = ensureInStoreCustomer();
        // Create global cart
        if (testErroneousCases) {
            Cart nullCart = this.globalClient.carts()
                    .post(
                            CartDraftBuilder.of()
                                    .customerId(storeCustomer.getId())
                                    .customerEmail(storeCustomer.getEmail())
                                    .country("UA")
                                    .currency("UAH")
                                    .build()
                    )
                    .execute()
                    .toCompletableFuture()
                    .exceptionally(t -> {
                        logger.info("An exception was caught: {}", t.getMessage());
                        return new ApiHttpResponse<>(HttpStatusCode.OK_200, null, null);
                    })
                    .get()
                    .getBody();
            if (nullCart == null) {
                logger.info("Fail to create global cart for store-restricted customer");
            } else {
                logger.info("It's strange, but the global cart was created for store-restricted customer");
            }
        }
        // Create in-store cart
        return this.globalClient.carts()
                .post(
                        CartDraftBuilder.of()
                                .store(
                                        StoreResourceIdentifierBuilder.of()
                                                .key(STORE_KEY)
                                                .build()
                                )
                                .customerId(storeCustomer.getId())
                                .customerEmail(storeCustomer.getEmail())
                                .country("UA")
                                .currency("UAH")
                                .build()
                )
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
    }

    private Customer ensureInStoreCustomer() throws InterruptedException, ExecutionException {
        Customer customer = this.globalClient
                .customers()
                .withKey(ONLY_STORE_CUSTOMER_KEY)
                .get()
                .execute()
                .exceptionally(t -> new ApiHttpResponse<>(HttpStatusCode.OK_200, null, null))
                .toCompletableFuture()
                .get()
                .getBody();
        if (customer == null) {
            customer = createInStoreCustomerWithGlobalClient();
        }
        return customer;
    }

    private Customer createInStoreCustomerWithGlobalClient() throws ExecutionException, InterruptedException {
        return this.globalClient
                .customers()
                .post(
                        CustomerDraftBuilder.of()
                                .key(ONLY_STORE_CUSTOMER_KEY)
                                .email(ONLY_STORE_CUSTOMER_KEY + "@gmail.com")
                                .password(PASS_PHRASE)
                                .addresses(
                                        BaseAddressBuilder.of()
                                                .country("UA")
                                                .city("Lviv")
                                                .postalCode("41108")
                                                .streetName("Politechniczna")
                                                .streetNumber("7")
                                                .apartment("9")
                                                .build()
                                )
                                .defaultShippingAddress(0)
                                .firstName("Mykolla")
                                .lastName("Czyslinn")
                                .stores(
                                        StoreResourceIdentifierBuilder.of()
                                                .key(STORE_KEY)
                                                .build()
                                )
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenApplyAsync(CustomerSignInResult::getCustomer)
                .toCompletableFuture()
                .get();
    }

    private Customer ensureGlobalCustomer() throws ExecutionException, InterruptedException {
        Customer customer = this.globalClient
                .customers()
                .withKey(GLOBAL_CUSTOMER_KEY)
                .get()
                .execute()
                .exceptionally(t -> new ApiHttpResponse<>(HttpStatusCode.OK_200, null, null))
                .toCompletableFuture()
                .get()
                .getBody();
        if (customer == null) {
            customer = createGlobalCustomerWithGlobalClient();
        }
        return customer;

    }

    private Customer createGlobalCustomerWithGlobalClient() throws ExecutionException, InterruptedException {
        return this.globalClient
                .customers()
                .post(
                        CustomerDraftBuilder.of()
                                .key(GLOBAL_CUSTOMER_KEY)
                                .email(GLOBAL_CUSTOMER_KEY + "@gmail.com")
                                .password(PASS_PHRASE)
                                .addresses(
                                        BaseAddressBuilder.of()
                                                .country("UA")
                                                .city("Lviv")
                                                .postalCode("41108")
                                                .streetName("Politechniczna")
                                                .streetNumber("4")
                                                .apartment("8")
                                                .build()
                                )
                                .defaultShippingAddress(0)
                                .firstName("Mykolasz")
                                .lastName("Czyszlin")
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenApplyAsync(CustomerSignInResult::getCustomer)
                .toCompletableFuture()
                .get();
    }

    private Cart createInStoreCart() throws IOException, InterruptedException, ExecutionException {

        initStoreClient();

        //  Create in-store Cart with in-store API client
        //  Update the ApiPrefixHelper with the prefix for Store API Client
        //  Provide an API client with scope limited to a store
        //  Provide a customer with only store permissions
        //  Try creating a global cart with a global customer and check the error message

        // Try to create a global cart with global customer

        if (this.testErroneousCases) {
            final Customer globalCustomer = ensureGlobalCustomer();

            Cart globalCart = storeClient
                    .carts()
                    .post(
                            CartDraftBuilder.of()
                                    .customerId(globalCustomer.getId())
                                    .customerEmail(globalCustomer.getEmail())
                                    .country("UA")
                                    .currency("UAH")
                                    .build()
                    )
                    .execute()
                    .exceptionally(t -> {
                        logger.info("An exception occurred: {}", t.getMessage());
                        return new ApiHttpResponse<>(HttpStatusCode.OK_200, null, null);
                    })
                    .get()
                    .getBody();
            if (globalCart == null) {
                logger.info("The global cart creation using In-Store client was failed as expected");
            } else {
                logger.error("The global cart creation has been succeeded while it should be failed");
            }
        }

        Customer inStoreCustomer = ensureInStoreCustomer();

        logger.info("Going to create in-store cart for store customer");

        return storeClient
                .inStore(STORE_KEY)
                .carts()
                .post(
                        CartDraftBuilder.of()
                                .customerId(inStoreCustomer.getId())
                                .customerEmail(inStoreCustomer.getEmail())
                                .country("UA")
                                .currency("UAH")
                                .build()
                )
                .execute()
                .exceptionally(t -> {
                    logger.error("An exception occurred while try to create in-store cart with in-store client: {}", t.getMessage());
                    return new ApiHttpResponse<>(HttpStatusCode.OK_200, null, null);
                })
                .get()
                .getBody();
    }

    private Cart createGlobalCustomerBoundCart() throws IOException, InterruptedException, ExecutionException {

        initMeClient();
        //  Create a cart via /me endpoint
        //  Provide API client with SPA for customer with global permissions
        //  Update the ApiPrefixHelper with the prefix for Me(SPA) API Client
        //  You can also create in-store customer-bound cart
        //  Visit impex to inspect the carts created

        final Customer globalPermissionCustomer = ensureGlobalCustomer();

        return meClient
                .me()
                .carts()
                .post(
                        MyCartDraftBuilder.of()
                                .currency("UAH")
                                .country("UA")
                                .deleteDaysAfterLastModification(90L)
                                .customerEmail(globalPermissionCustomer.getEmail())
                                .build()
                )
                .execute()
                .exceptionally(throwable -> {
                    logger.info(throwable.getLocalizedMessage());
                    return null;
                })
                .toCompletableFuture().get()
                .getBody();
    }

    private Cart createInStoreCustomerBoundCart() throws IOException, InterruptedException, ExecutionException {
        final String storeMeApiClientPrefix = ApiPrefixHelper.API_STORE_ME_CLIENT_PREFIX.getPrefix();
        storeMeClient = createStoreMeApiClient(storeMeApiClientPrefix);

        //  Create in-store customer-bound Cart with in-store-me API client
        //  Update the ApiPrefixHelper with the prefix for Me(SPA) API Client
        //  Provide in-store-me API client with scope for a store and me endpoint
        //  Try creating a global cart without me and check the error message
        //  Visit impex to inspect the carts created
        Customer globalCustomer = ensureGlobalCustomer();

        if (testErroneousCases) {
            //  Try creating a global cart without me and check the error message
            Cart nullCart = storeMeClient
                    .carts()
                    .post(
                            CartDraftBuilder.of()
                                    .deleteDaysAfterLastModification(90L)
                                    .country("UA")
                                    .currency("UAH")
                                    .customerEmail(globalCustomer.getEmail())
                                    .build()
                    )
                    .execute()
                    .thenApplyAsync(ApiHttpResponse::getBody)
                    .exceptionally(throwable -> {
                        logger.info(throwable.getLocalizedMessage());
                        return null;
                    })
                    .toCompletableFuture().get();
            if (nullCart == null) {
                logger.info("Fail to create global cart as it's expected");
            } else {
                logger.error("The global cart was created despite it's expected creation should be failed");
            }
        }

        return storeMeClient
                .inStore(STORE_KEY)
                .me()
                .carts()
                .post(
                        MyCartDraftBuilder.of()
                                .deleteDaysAfterLastModification(90L)
                                .country("UA")
                                .currency("UAH")
                                .customerEmail(globalCustomer.getEmail())
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .exceptionally(throwable -> {
                    logger.info(throwable.getLocalizedMessage());
                    return CartBuilder.of().buildUnchecked();
                })
                .toCompletableFuture().get();
    }

    private void initGlobalApiClient() throws IOException {
        final String globalApiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();
        this.globalClient = createApiClient(globalApiClientPrefix);
    }

    private void initStoreClient() throws IOException {
        final String storeApiClientPrefix = ApiPrefixHelper.API_STORE_CLIENT_PREFIX.getPrefix();
        this.storeClient = createApiClient(storeApiClientPrefix);
    }

    private void initMeClient() throws IOException {
        final String meApiClientPrefix = ApiPrefixHelper.API_ME_CLIENT_PREFIX.getPrefix();
        this.meClient = createMeTokenApiClient(meApiClientPrefix);
    }

    @Override
    public void close() throws IOException {
        if (this.globalClient != null) {
            this.globalClient.close();
        }
        if (this.storeClient != null) {
            this.storeClient.close();
        }
        if (this.meClient != null) {
            this.meClient.close();
        }
        if (this.storeMeClient != null) {
            this.storeMeClient.close();
        }
    }
}