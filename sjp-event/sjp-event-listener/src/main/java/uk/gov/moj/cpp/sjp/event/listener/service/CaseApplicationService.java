package uk.gov.moj.cpp.sjp.event.listener.service;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class CaseApplicationService {

    @Inject
    private CaseApplicationRepository caseApplicationRepository;

    @Inject
    private CaseApplicationDecisionRepository caseApplicationDecisionRepository;

    @Transactional
    public void saveCaseApplication(final CaseApplication caseApplication) {
        caseApplicationRepository.save(caseApplication);
    }

    @Transactional
    public void saveCaseApplicationDecision(final CaseApplicationDecision caseApplicationDecision) {
        caseApplicationDecisionRepository.save(caseApplicationDecision);
    }

    public CaseApplication findById(final UUID applicationId) {
        return caseApplicationRepository.findBy(applicationId);
    }
}