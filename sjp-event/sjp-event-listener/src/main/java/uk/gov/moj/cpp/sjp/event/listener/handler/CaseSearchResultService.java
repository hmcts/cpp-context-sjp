package uk.gov.moj.cpp.sjp.event.listener.handler;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResultList;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class CaseSearchResultService {

    @Inject
    private CaseSearchResultRepository repository;

    @Transactional
    public void onDefendantDetailsUpdated(final UUID caseId, final String newFirstName, final String newLastName, final LocalDate newDateOfBirth) {
        final CaseSearchResultList exisingEntryList = new CaseSearchResultList(repository.findByCaseId(caseId));

        if (exisingEntryList.hasDateOfBirthChanged(newDateOfBirth)) {
            exisingEntryList.setDateOfBirth(newDateOfBirth);
        }
        if (exisingEntryList.hasNameChanged(newFirstName, newLastName)) {
            exisingEntryList.setName(newFirstName, newLastName);
            exisingEntryList.add(caseId, newFirstName, newLastName, newDateOfBirth);
        }

        exisingEntryList.forEach(repository::save);
    }

    @Transactional
    public void caseAssigned(final UUID caseId) {
        updateCaseAssignment(caseId, true);
    }

    @Transactional
    public void caseUnassigned(final UUID caseId) {
        updateCaseAssignment(caseId, false);
    }

    @Transactional
    public void updatePleaReceivedDate(final UUID caseId, final LocalDate pleaReceived) {
        repository.findByCaseId(caseId).forEach(searchResult -> searchResult.setPleaDate(pleaReceived));
    }

    private void updateCaseAssignment(final UUID caseId, boolean assigned) {
        repository.findByCaseId(caseId).forEach(result -> result.setAssigned(assigned));
    }
}
