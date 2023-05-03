package handson.impl;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.*;
import com.commercetools.api.models.channel.Channel;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.common.BaseAddressBuilder;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifier;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.shipping_method.ShippingMethod;
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifierBuilder;
import io.vrap.rmf.base.client.ApiHttpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class CartService {

    final ProjectApiRoot apiRoot;

    public CartService(final ProjectApiRoot client) {
        this.apiRoot = client;
    }


    /**
     * Creates a cart for the given customer.
     *
     * @return the customer creation completion stage
     */

    public CompletableFuture<ApiHttpResponse<Cart>> getCartById(final String cartId) {

        return
                apiRoot
                        .carts()
                        .withId(cartId)
                        .get()
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> createCart(final ApiHttpResponse<Customer> customerApiHttpResponse) {

        Customer customer = customerApiHttpResponse.getBody();
        return
                apiRoot
                        .carts()
                        .post(
                                CartDraftBuilder.of()
                                        .customerId(customer.getId())
                                        .customerEmail(customer.getEmail())
                                        .currency("UAH")
                                        .country("UA")
                                        .deleteDaysAfterLastModification(90L)
                                        .shippingAddress(
                                                customer.getAddresses().stream().findFirst().orElseThrow(IllegalStateException::new)
                                        )
                                         .inventoryMode(InventoryMode.RESERVE_ON_ORDER)
                                        .build()
                        )
                        .execute();
    }


    public CompletableFuture<ApiHttpResponse<Cart>> createAnonymousCart() {

        return
                apiRoot
                        .carts()
                        .post(
                                CartDraftBuilder.of()
                                        .currency("UAH")
                                        .deleteDaysAfterLastModification(90L)
                                        .anonymousId("an" + System.nanoTime())
                                        .country("UA")
                                        .build()
                        )
                        .execute();
    }


    public CompletableFuture<ApiHttpResponse<Cart>> addProductToCartBySkusAndChannel(
            final ApiHttpResponse<Cart> cartApiHttpResponse,
            final Channel channel,
            final String... skus) {

        final Cart cart = cartApiHttpResponse.getBody();

        return apiRoot
                .carts()
                .withId(cart.getId())
                .post(
                        CartUpdateBuilder.of()
                                .version(cart.getVersion())
                                .actions(
                                        Arrays.stream(skus)
                                                .map(
                                                        sku -> CartAddLineItemActionBuilder
                                                                .of()
                                                                .sku(sku)
                                                                .quantity(1L)
                                                                .supplyChannel(
                                                                        ChannelResourceIdentifierBuilder.of()
                                                                                .id(channel.getId())
                                                                                .build()
                                                                )
                                                                .build()
                                                )
                                                .collect(Collectors.toList())
                                )
                                .build()
                )
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> addDiscountToCart(
            final ApiHttpResponse<Cart> cartApiHttpResponse, final String code) {
        final Cart cart = cartApiHttpResponse.getBody();
        return
                apiRoot
                        .carts()
                        .withId(cart.getId())
                        .post(
                                CartUpdateBuilder.of()
                                        .version(cart.getVersion())
                                        .actions(
                                                CartAddDiscountCodeActionBuilder.of()
                                                        .code(code)
                                                        .build()
                                        )
                                        .build()
                        )
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> recalculate(final ApiHttpResponse<Cart> cartApiHttpResponse) {

        final Cart cart = cartApiHttpResponse.getBody();
        return apiRoot
                .carts()
                .withId(cart.getId())
                .post(
                        CartUpdateBuilder.of()
                                .version(cart.getVersion())
                                .actions(
                                        CartRecalculateActionBuilder
                                                .of()
                                                .updateProductData(true)
                                                .build()
                                )
                                .build()
                )
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<Cart>> setShipping(final ApiHttpResponse<Cart> cartApiHttpResponse) {
        final Cart cart = cartApiHttpResponse.getBody();
        final ShippingMethod shippingMethod = apiRoot
                .shippingMethods()
                .matchingCart()
                .get()
                .withCartId(cart.getId())
                .executeBlocking()
                .getBody()
                .getResults()
                .get(0);
        return apiRoot
                .carts()
                .withId(cart.getId())
                .post(
                        CartUpdateBuilder.of()
                                .version(cart.getVersion())
                                .actions(
                                        CartSetShippingMethodActionBuilder
                                                .of()
                                                .shippingMethod(
                                                        ShippingMethodResourceIdentifierBuilder.of()
                                                                .key(shippingMethod.getKey())
                                                                .build()
                                                )
                                                .build()/*,
                                        CartSetShippingAddressActionBuilder.of()
                                                .address(
                                                        BaseAddressBuilder.of()
                                                                .key("{shipping-address-key}")
                                                                .country(cart.getCountry())
                                                                .state("{state}")
                                                                .city("{city}")
                                                                .streetName("{streetName}")
                                                                .streetNumber("{streetNumber}")
                                                                .building("{building}")
                                                                .apartment("{apartment}")
                                                                .postalCode("{postalCode}")
                                                                .build()
                                                )
                                                .build()*/
                                )
                                .build()
                )
                .execute();
    }


}
