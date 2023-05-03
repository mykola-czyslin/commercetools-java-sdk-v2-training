package handson;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.state.State;
import com.commercetools.api.models.state.StateResourceIdentifierBuilder;
import com.commercetools.api.models.state.StateTypeEnum;
import handson.impl.ApiPrefixHelper;
import handson.impl.StateMachineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static handson.impl.ClientService.createApiClient;


public class Task04a_STATEMACHINE {

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        final String apiClientPrefix = ApiPrefixHelper.API_DEV_CLIENT_PREFIX.getPrefix();

        Logger logger = LoggerFactory.getLogger(Task04a_STATEMACHINE.class.getName());
        final ProjectApiRoot client = createApiClient(apiClientPrefix);
        final StateMachineService stateMachineService = new StateMachineService(client);

        // TODO
        // Use StateMachineService.java to create your designed order state machine
        //
        State orderPackedState =
                stateMachineService.createState("mczOrderPacked", StateTypeEnum.ORDER_STATE, true, "MCz Order Packed")
                        .toCompletableFuture().get()
                        .getBody();
        logger.info("\norder packed state:\n\tid = {}\n\t transitions = {}\n", orderPackedState.getId(), orderPackedState.getTransitions());
        State orderDispatchedState =
                stateMachineService.createState("mczOrderDispatched", StateTypeEnum.ORDER_STATE, false, "MCz Order Dispatched")
                        .toCompletableFuture().get()
                        .getBody();
        logger.info("\norder dispatched state:\n\tid = {}\n\t transitions = {}\n", orderDispatchedState.getId(), orderDispatchedState.getTransitions());
        State orderSuspectedState =
                stateMachineService.createState("mczOrderSuspected", StateTypeEnum.ORDER_STATE, false, "MCz Order Suspected")
                        .toCompletableFuture()
                        .get()
                        .getBody();
        logger.info("\norder suspected state:\n\tid = {}\n\t transitions = {}\n", orderSuspectedState.getId(), orderSuspectedState.getTransitions());
        State orderVerifiedState =
                stateMachineService.createState("mczOrderVerified", StateTypeEnum.ORDER_STATE, false, "MCz Order Verified")
                        .toCompletableFuture()
                        .get()
                        .getBody();
        logger.info("\norder verified state:\n\tid = {}\n\t transitions = {}\n", orderVerifiedState.getId(), orderVerifiedState.getTransitions());
        State orderRejectedState =
                stateMachineService.createState("mczOrderRejected", StateTypeEnum.ORDER_STATE, false, "MCz Order Rejected")
                        .toCompletableFuture()
                        .get()
                        .getBody();
        logger.info("\norder rejected state:\n\tid = {}\n\t transitions = {}\n", orderRejectedState.getId(), orderRejectedState.getTransitions());
        State orderShippedState =
                stateMachineService.createState("mczOrderShipped", StateTypeEnum.ORDER_STATE, false, "MCz Order Shipped")
                        .toCompletableFuture().get()
                        .getBody();

        logger.info("\nState info:\n{}",
                stateMachineService
                        .setStateTransitions(
                                orderPackedState,
                                Arrays.asList(
                                                StateResourceIdentifierBuilder.of().
                                                        id(orderDispatchedState.getId())
                                                        .build(),
                                                StateResourceIdentifierBuilder.of()
                                                        .id(orderSuspectedState.getId())
                                                        .build()
                                        )

                        )
                        .toCompletableFuture()
                        .get()
        );

        logger.info("\nState info:\n{}",
                stateMachineService
                        .setStateTransitions(
                                orderSuspectedState,
                                Arrays.asList(
                                        StateResourceIdentifierBuilder.of().
                                                id(orderVerifiedState.getId())
                                                .build(),
                                        StateResourceIdentifierBuilder.of()
                                                .id(orderRejectedState.getId())
                                                .build()
                                )

                        )
                        .toCompletableFuture()
                        .get()
        );

        logger.info("\nState Info:\n{}", stateMachineService
                .setStateTransitions(orderRejectedState, Collections.emptyList())
        );

        logger.info("\nState info:\n{}", stateMachineService
                .setStateTransitions(
                        orderDispatchedState,
                        Arrays.asList(
                                StateResourceIdentifierBuilder.of().
                                        id(orderPackedState.getId())
                                        .build(),
                                StateResourceIdentifierBuilder.of().
                                        id(orderShippedState.getId())
                                        .build()
                        )
                )
                .toCompletableFuture()
                .get()
        );

        logger.info("\nState info:\n{}",
                stateMachineService.setStateTransitions(
                                orderShippedState,
                                new ArrayList<>()
                        )
                        .toCompletableFuture().get()
        );

        client.close();
    }
}
