package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;

import java.util.List;

import javax.inject.Inject;

public class UserAndGroupsService {

    private static final List<String> SHOW_ONLINE_PLEA_FINANCES = asList("Legal Advisers", "Court Administrators");

    @Inject
    private UserAndGroupProvider userAndGroupProvider;

    public boolean canSeeOnlinePleaFinances(JsonEnvelope originalEnvelope) {
        return userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(new Action(originalEnvelope), SHOW_ONLINE_PLEA_FINANCES);
    }

}