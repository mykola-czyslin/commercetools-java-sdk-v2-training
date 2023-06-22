package handson.impl;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.AddressBuilder;
import com.commercetools.api.models.customer.*;

import com.commercetools.api.models.customer_group.CustomerGroup;
import com.commercetools.api.models.customer_group.CustomerGroupDraftBuilder;
import com.commercetools.api.models.customer_group.CustomerGroupResourceIdentifierBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.TypeResourceIdentifier;
import io.vrap.rmf.base.client.ApiHttpResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * This class provides operations to work with {@link Customer}s.
 */
public class CustomerService {

    final ProjectApiRoot apiRoot;

    public CustomerService(final ProjectApiRoot client) {
        this.apiRoot = client;
    }

    public CompletableFuture<ApiHttpResponse<Customer>> getCustomerByKey(final String customerKey) {
        return
                this.apiRoot
                        .customers()
                        .withKey(customerKey)
                        .get().execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerPagedQueryResponse>> getCustomerByCustomerNumber(String customerNumber) {
        String whereClause = "customerNumber=\"" + customerNumber + "\"";
        return this.apiRoot.customers().get().withWhere(whereClause).execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerSignInResult>> createCustomer(
            final String email,
            final String password,
            final String customerKey,
            final String firstName,
            final String lastName,
            final String country) {

        return this.apiRoot.customers().post(
                        CustomerDraft.builder()
                                .email(email)
                                .password(password)
                                .key(customerKey)
                                .firstName(firstName)
                                .lastName(lastName)
                                .addresses(
                                        AddressBuilder.of()
                                                .country(country)
                                                .build()
                                )
                                .defaultShippingAddress(0)
                                .build()
                )
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerToken>> createEmailVerificationToken(
            final ApiHttpResponse<CustomerSignInResult> customerSignInResultApiHttpResponse,
            final long timeToLiveInMinutes
    ) {

        final Customer customer = customerSignInResultApiHttpResponse.getBody().getCustomer();

//        CustomerCreateEmailToken.builder()
        return
//                this.apiRoot.customers().post()
                this.createEmailVerificationToken(customer, timeToLiveInMinutes);
    }

    public CompletableFuture<ApiHttpResponse<CustomerToken>> createEmailVerificationToken(final Customer customer, final long timeToLiveInMinutes) {

        return
                apiRoot
                        .customers()
                        .emailToken()
                        .post(
                                CustomerCreateEmailTokenBuilder.of()
                                        .id(customer.getId())
                                        .ttlMinutes(timeToLiveInMinutes)
                                        .build()
                        )
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Customer>> verifyEmail(final ApiHttpResponse<CustomerToken> customerTokenApiHttpResponse) {

        final CustomerToken customerToken = customerTokenApiHttpResponse.getBody();

        return
                verifyEmail(customerToken);
    }

    public CompletableFuture<ApiHttpResponse<Customer>> verifyEmail(final CustomerToken customerToken) {


        return
                apiRoot
                        .customers()
                        .emailConfirm()
                        .post(
                                CustomerEmailVerifyBuilder.of()
                                        .tokenValue(customerToken.getValue())
                                        .build()
                        )
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerGroup>> createGroup(String groupKey, String groupName) {
        return apiRoot
                .customerGroups()
                .post(
                        CustomerGroupDraftBuilder.of()
                                .key(groupKey)
                                .groupName(groupName)
                                .build()
                )
                .execute();
    }

    public CompletableFuture<ApiHttpResponse<CustomerGroup>> getCustomerGroupByKey(final String customerGroupKey) {
        return
                apiRoot
                        .customerGroups()
                        .withKey(customerGroupKey)
                        .get()
                        .execute();
    }

    public CompletableFuture<ApiHttpResponse<Customer>> assignCustomerToCustomerGroup(
            final ApiHttpResponse<Customer> customerApiHttpResponse,
            final ApiHttpResponse<CustomerGroup> customerGroupApiHttpResponse) {

        final Customer customer = customerApiHttpResponse.getBody();
        final CustomerGroup customerGroup = customerGroupApiHttpResponse.getBody();

        return
                apiRoot
                        .customers()
                        .withKey(customer.getKey())
                        .post(
                                CustomerUpdateBuilder.of()
                                        .version(customer.getVersion())
                                        .actions(
                                                CustomerSetCustomerGroupActionBuilder.of()
                                                        .customerGroup(
                                                                CustomerGroupResourceIdentifierBuilder.of()
                                                                        .key(customerGroup.getKey())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .execute();
    }

}
