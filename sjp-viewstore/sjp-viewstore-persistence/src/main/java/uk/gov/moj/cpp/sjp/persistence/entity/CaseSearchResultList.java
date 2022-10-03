package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public class CaseSearchResultList {

    private final List<CaseSearchResult> caseSearchResults;

    public CaseSearchResultList(final List<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    public void setName(final UUID caseId, final UUID defendantId, final String newFirstName, final String newLastName,
                        final LocalDate newDateOfBirth, final ZonedDateTime dateAdded, final String newLegalEntityName) {

        final Optional<CaseSearchResult> latest = getCurrent();

        final CaseSearchResult newEntry = new CaseSearchResult(caseId, defendantId, newFirstName, newLastName, newDateOfBirth, dateAdded, newLegalEntityName);
        latest.ifPresent(r -> {
            final String firstName = ofNullable(newFirstName).orElse(r.getFirstName());
            final String lastName = ofNullable(newLastName).orElse(r.getLastName());
            final String legalEntityName = ofNullable(newLegalEntityName).orElse(r.getLegalEntityName());
            newEntry.setFirstName(firstName);
            newEntry.setLastName(lastName);
            newEntry.setCurrentFirstName(firstName);
            newEntry.setCurrentLastName(lastName);
            newEntry.setLegalEntityName(legalEntityName);
            newEntry.setWithdrawalRequestedDate(r.getWithdrawalRequestedDate());
        });

        caseSearchResults.forEach(entry -> {
            entry.setDeprecated(true);
            entry.setCurrentFirstName(newEntry.getCurrentFirstName());
            entry.setCurrentLastName(newEntry.getCurrentLastName());
        });

        caseSearchResults.add(newEntry);
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        caseSearchResults.forEach(entry -> entry.setDateOfBirth(dateOfBirth));
    }

    public void forEach(final Consumer<CaseSearchResult> consumer) {
        caseSearchResults.forEach(consumer);
    }

    public boolean hasDateOfBirthChanged(final LocalDate newDateOfBirth) {
        return getCurrent()
                .map(old ->
                        !Objects.equals(old.getDateOfBirth(), newDateOfBirth))
                .orElse(true);
    }

    public boolean hasNameChanged(final String newFirstName, final String newLastName, final String newLegalEntityName) {
        return getCurrent()
                .map(old -> equalsIgnoreCaseAndNull(newFirstName, old.getFirstName()) ||
                        equalsIgnoreCaseAndNull(newLastName, old.getLastName()) ||
                        equalsIgnoreCaseAndNull(newLegalEntityName, old.getLegalEntityName()))
                .orElse(true);
    }

    private Optional<CaseSearchResult> getCurrent() {
        return caseSearchResults.stream().filter(r -> !r.isDeprecated()).findFirst();
    }

    private boolean equalsIgnoreCaseAndNull(final String newName, final String oldName) {
        return nonNull(newName) && !StringUtils.equalsIgnoreCase(newName, oldName);
    }
}
