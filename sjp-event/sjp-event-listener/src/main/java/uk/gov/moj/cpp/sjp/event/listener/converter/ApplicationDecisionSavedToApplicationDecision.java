package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import javax.inject.Inject;


public class ApplicationDecisionSavedToApplicationDecision implements Converter<ApplicationDecisionSaved, CaseApplicationDecision> {

    @Inject
    private SessionRepository sessionRepository;

    @Inject
    private CaseApplicationRepository applicationRepository;

    @Override
    public CaseApplicationDecision convert(final ApplicationDecisionSaved event) {
        final CaseApplicationDecision applicationDecision = new CaseApplicationDecision();
        applicationDecision.setDecisionId(event.getDecisionId());
        applicationDecision.setGranted(event.getApplicationDecision().getGranted());
        ofNullable(event.getApplicationDecision()).ifPresent(decision -> {
            applicationDecision.setOutOfTime(decision.getOutOfTime());
            applicationDecision.setOutOfTimeReason(decision.getOutOfTimeReason());
            applicationDecision.setRejectionReason(decision.getRejectionReason());
        });
        applicationDecision.setSavedAt(event.getSavedAt());
        applicationDecision.setSession(sessionRepository.findBy(event.getSessionId()));
        applicationDecision.setCaseApplication(applicationRepository.findBy(event.getApplicationId()));
        return applicationDecision;
    }
}
