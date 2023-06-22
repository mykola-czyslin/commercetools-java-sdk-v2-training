package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartResourceIdentifierBuilder;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelPagedQueryResponse;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerPagedQueryResponse;
import com.commercetools.api.models.quote.*;
import com.commercetools.api.models.quote_request.QuoteRequest;
import com.commercetools.api.models.quote_request.QuoteRequestDraftBuilder;
import com.commercetools.api.models.quote_request.QuoteRequestResourceIdentifierBuilder;
import com.commercetools.api.models.staged_quote.StagedQuote;
import com.commercetools.api.models.staged_quote.StagedQuoteDraftBuilder;
import com.commercetools.api.models.staged_quote.StagedQuoteResourceIdentifierBuilder;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StatePagedQueryResponse;
import com.commercetools.api.models.state.StateReferenceBuilder;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import handson.impl.ApiPrefixHelper;
import handson.impl.CartService;
import handson.impl.CustomerService;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static handson.impl.ClientService.createApiClient;
import static java.lang.String.format;

public class Task10b_CreateQuote {

    public static void main(String[] args) throws Exception {
        final String apiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();

        final ProjectApiRoot client = createApiClient(apiClientPrefix);
        CustomerService customerService = new CustomerService(client);
        CartService cartService = new CartService(client);
        Logger logger = LoggerFactory.getLogger(Task10b_CreateQuote.class.getName());

        List<Customer> customers = client.customers()
                .get()
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenApplyAsync(CustomerPagedQueryResponse::getResults)
                .join();
        customers.forEach(cust -> {
            logger.info("customer: id = {}, key = {}, customer number = {}", cust.getId(), cust.getKey(), cust.getCustomerNumber());
        });

        Customer customer = customerService.getCustomerByCustomerNumber("CHARLES03WINDSORE")
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenApplyAsync(CustomerPagedQueryResponse::getResults)
                .thenApplyAsync(List::stream)
                .thenApplyAsync(Stream::findFirst)
                .thenApplyAsync(opt -> opt.orElseThrow(() -> new IllegalArgumentException("CHARLES03WINDSORE")))
                .join();



        Cart cart = cartService.createCart(customer)
                .thenApplyAsync(ApiHttpResponse::getBody)
                .toCompletableFuture()
                .join();
        logger.info("cart-id: " + cart.getId());
        Channel channel = client
                .channels()
                .get()
                .withWhere("key=\"mc-default\"")
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .thenApplyAsync(ChannelPagedQueryResponse::getResults)
                .thenApplyAsync(List::stream)
                .thenApplyAsync(Stream::findFirst)
                .thenApplyAsync(opt -> opt.orElseThrow(IllegalStateException::new))
                .join();
        logger.info("Channel: {}", channel.getKey());
        if (!"mc-default".equalsIgnoreCase(channel.getKey())) {
            throw new IllegalArgumentException(format("Expected \"mc-default\", obtained: %s", channel.getKey()));
        }
        cart = cartService.addProductToCart(cart, channel, "XSWHITETSHIRT001", "XSBLUETSHIRT001", "XSGRAYTSHIRT001")
                .thenApplyAsync(ApiHttpResponse::getBody)
                .join();

        logger.info("Products have been added to cart");

        QuoteRequest quoteRequest = client
                .quoteRequests()
                .post(
                        QuoteRequestDraftBuilder.of()
                                .cart(
                                        CartResourceIdentifierBuilder
                                                .of()
                                                .id(cart.getId())
                                                .build()
                                )
                                .cartVersion(cart.getVersion())
                                .comment("Quote for cart " + cart.getId())
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .toCompletableFuture()
                .join();

        logger.info("Quote request has been created: {}", quoteRequest.getId());

        StagedQuote stagedQuote = client
                .stagedQuotes()
                .post(
                        StagedQuoteDraftBuilder.of()
                                .quoteRequest(
                                        QuoteRequestResourceIdentifierBuilder
                                                .of()
                                                .id(quoteRequest.getId())
                                                .build()
                                )
                                .quoteRequestVersion(quoteRequest.getVersion())
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .join();
        logger.info("Staged quote has been created: {}", stagedQuote.getId());

//        State initialState = client
//                .states()
//                .get()
//                .withWhere("key=\"WaitingPrintout\"")
//                .execute()
//                .thenApplyAsync(ApiHttpResponse::getBody)
//                .thenApplyAsync(StatePagedQueryResponse::getResults)
//                .thenApplyAsync(List::stream)
//                .thenApplyAsync(Stream::findFirst)
//                .thenApplyAsync(opt -> opt.orElseThrow(() -> new IllegalArgumentException("Cannot find WaitingPrintout state")))
//                .join();


        Quote quote = client
                .quotes()
                .post(
                        QuoteDraftBuilder.of()
                                .stagedQuote(
                                        StagedQuoteResourceIdentifierBuilder
                                                .of()
                                                .id(stagedQuote.getId())
                                                .build()
                                )
                                .stagedQuoteVersion(stagedQuote.getVersion())
//                                .state(
//                                        StateReferenceBuilder.of()
//                                                .id(initialState.getId())
//                                                .build()
//                                )
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .join();

        logger.info("The quote was created: {}, state: {}, quoteState: {}", quote.getId(), quote.getState(), quote.getQuoteState());

        quote = client
                .quotes()
                .withId(quote.getId())
                .post(
                        QuoteUpdateBuilder.of()
                                .version(quote.getVersion())
                                .actions(
                                        QuoteTransitionStateAction
                                                .builder()
                                                .state(
                                                        StateResourceIdentifierBuilder
                                                                .of()
                                                                .key("WaitingPrintout")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .join();

        logger.info("The quote was updated: {}, state: {}, quoteState: {}", quote.getId(), quote.getState(), quote.getQuoteState());


        quote = client
                .quotes()
                .withId(quote.getId())
                .post(
                        QuoteUpdateBuilder.of()
                                .version(quote.getVersion())
                                .actions(
                                        QuoteChangeQuoteStateActionBuilder.of()
                                                .quoteState(QuoteState.ACCEPTED)
                                                .build()
                                )
                                .build()
                )
                .execute()
                .thenApplyAsync(ApiHttpResponse::getBody)
                .join();
        logger.info("The quote's quoteState was updated: {}, state: {}, quoteState: {}", quote.getId(), quote.getState(), quote.getQuoteState());
    }
}
