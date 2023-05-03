package handson.impl;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.error.BadRequestException;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.state.*;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.NotFoundException;
import io.vrap.rmf.base.client.http.HttpStatusCode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * .
 */
public class StateMachineService {

    final ProjectApiRoot apiRoot;

    private final Logger logger = LoggerFactory.getLogger(StateMachineService.class);

    public StateMachineService(final ProjectApiRoot client) {
        this.apiRoot = client;
    }

    public CompletableFuture<ApiHttpResponse<State>> createState(final String key, StateTypeEnum stateTypeEnum, final Boolean initial, final String name) throws InterruptedException, ExecutionException {

        Map<String, String> myNames = new HashMap<String, String>();
        myNames.put("DE", name);
        myNames.put("EN", name);

        return apiRoot
                .states()
                .withKey(key)
                .get()
//                .withExpand("transitions")
//                .withExpand("transitions[*].key")
//                .withExpand("transitions[*].name")
                .execute()
                .handle((resp, throwable) -> {
                    logger.info("\n!!!!handling create\nresponse: {};\nthrowable: {}", resp, throwable == null ? null : throwable.getClass());
                    if (resp == null || throwable instanceof NotFoundException) {
                        return apiRoot
                                .states()
                                .post(
                                        StateDraftBuilder.of()
                                                .key(key)
                                                .type(stateTypeEnum)
                                                .initial(initial)
                                                .name(
                                                        LocalizedStringBuilder.of()
                                                                .values(myNames)
                                                                .build()
                                                )
                                                .build()
                                )
                                .execute();
                    } else {
                        List<StateUpdateAction> actions = new ArrayList<>();
                        State state = resp.getBody();
                        if (!Objects.equals(state.getInitial(), initial)) {
                            actions.add(
                                    StateChangeInitialActionBuilder.of()
                                            .initial(initial)
                                            .build()
                            );
                        }
                        if (!Objects.equals(state.getType(), stateTypeEnum)) {
                            actions.add(
                                    StateChangeTypeActionBuilder.of()
                                            .type(stateTypeEnum)
                                            .build()
                            );
                        }
                        if (state.getName() == null || !Objects.equals(state.getName().get("EN"), myNames.get("EN")) || !Objects.equals(state.getName().get("DE"), myNames.get("DE"))) {
                            actions.add(
                                    StateSetNameActionBuilder.of()
                                            .name(
                                                    LocalizedStringBuilder.of()
                                                            .values(myNames)
                                                            .build()
                                            )
                                            .build()
                            );
                        }
                        return actions.isEmpty() ? CompletableFuture.supplyAsync(() -> resp) : apiRoot
                                .states()
                                .withId(state.getId())
                                .post(
                                        StateUpdateBuilder.of()
                                                .actions(actions)
                                                .version(state.getVersion())
                                                .build()
                                )
                                .execute();
                    }
                })
                .toCompletableFuture()
                .get();
    }

    public CompletableFuture<ApiHttpResponse<State>> setStateTransitions(final State stateToBeUpdated, final List<StateResourceIdentifier> states) {

        return
                apiRoot
                        .states()
                        .withId(stateToBeUpdated.getId())
                        .post(
                                StateUpdateBuilder.of()
                                        .actions(
                                                StateSetTransitionsActionBuilder.of()
                                                        .transitions(states)
                                                        .build()
                                        )
                                        .version(stateToBeUpdated.getVersion())
                                        .build()
                        )
                        .execute()
                        .handle((resp, throwable) -> {
                            logger.info("\n!!!!!handling states :\nresponse: {},\nthrowable: {}", resp, throwable == null ? null : throwable.getClass());
                            Throwable rootCause = ExceptionUtils.getRootCause(throwable);
                            if (rootCause instanceof BadRequestException && ((BadRequestException) rootCause).getErrorResponse().getMessage().equals("'transitions' has no changes.")) {
                                return new ApiHttpResponse<State>(HttpStatusCode.ACCEPTED_202, null, stateToBeUpdated);
                            } else {
                                logger.info("###returning {}", resp);
                                return resp;
                            }
                        });
    }

}
