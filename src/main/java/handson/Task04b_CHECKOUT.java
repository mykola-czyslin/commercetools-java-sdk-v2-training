package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.customer_group.CustomerGroupUpdateBuilder;
import com.commercetools.api.models.order.OrderState;
import com.commercetools.api.models.state.State;
import handson.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static handson.impl.ClientService.createApiClient;


/**
 * Create a cart for a customer, add a product to it, create an order from the cart and change the order state.
 * <p>
 * See:
 */
public class Task04b_CHECKOUT {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        final String apiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();

        final ProjectApiRoot client = createApiClient(apiClientPrefix);

        CustomerService customerService = new CustomerService(client);
        CartService cartService = new CartService(client);
        OrderService orderService = new OrderService(client);
        PaymentService paymentService = new PaymentService(client);
        Logger logger = LoggerFactory.getLogger(Task04b_CHECKOUT.class.getName());


        // TODO: Fetch a channel if your inventory mode will not be NONE
        //
        Channel channel = client
                .channels()
                .get()
                .withWhere("key=\"test_chan\"")
                .execute()
                .toCompletableFuture()
                .get()
                .getBody()
                .getResults()
                .get(0);

        final State orderPacked = client
                .states()
                .withKey("mczOrderPacked")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
        final State orderDispatched = client
                .states()
                .withKey("mczOrderDispatched")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
        final State orderSuspected = client
                .states()
                .withKey("mczOrderSuspected")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
        final State orderVerified = client
                .states()
                .withKey("mczOrderVerified")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
        final State orderRejected =
            client
                .states()
                .withKey("mczOrderRejected")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();
        final State orderShipped = client
                .states()
                .withKey("mczOrderShipped")
                .get()
                .execute()
                .toCompletableFuture()
                .get()
                .getBody();

        // TODO: Perform cart operations:
        //      Get Customer, create cart, add products, add inventory mode
        //      add discount codes, perform a recalculation
        // TODO: Convert cart into an order, set order status, set state in custom work
        //
        // TODO: add payment
        // TAKE CARE: Take off payment for second or third try OR change the interfaceID with a timestamp
        //
        // TODO additionally: add custom line items, add shipping method
        //
        logger.info("Created cart/order ID: {}",
                customerService.getCustomerByKey("mykola-czyslin")
                        .thenComposeAsync(
                                customer -> cartService.createCart(customer)
                        )
                        .thenComposeAsync(
                                cart -> cartService.addProductToCartBySkusAndChannel(cart, channel, "PALECHAMOMILLE", "WHITECHAMOMILLE")
                        )
                        .thenComposeAsync(
                                cart -> cartService.addDiscountToCart(cart, "ALDISCO")
                        )
                        .thenComposeAsync(
                                cart -> cartService.setShipping(cart)
                        )
                        .thenComposeAsync(cart -> cartService.recalculate(cart))
                        .thenComposeAsync(
                        cart -> paymentService.createPaymentAndAddToCart(cart, "payment_provider", "creadit_card", "adyen" + Math.random(), "inter" + Math.random())
                        )
                        .thenComposeAsync(
                                cart -> orderService.createOrder(cart)
                        )
                        .thenComposeAsync(
                                order -> orderService.changeState(order, OrderState.CONFIRMED)
                        )
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderPacked)
                        )
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderSuspected)
                        )
/*
transitions from the mczOrderRejected to any other state is forbidden
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderRejected)
                        )
*/
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderVerified)
                        )
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderDispatched)
                        )
                        .thenComposeAsync(
                                order -> orderService.changeWorkflowState(order, orderShipped)
                        )
                        .get()
                        .getBody()
                        .getId()
        );

        client.close();
    }
}
