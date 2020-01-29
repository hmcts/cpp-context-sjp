package uk.gov.moj.cpp.sjp.persistence.entity;

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

    private Optional<CaseSearchResult> getCurrent() {
        return caseSearchResults.stream().filter(r -> !r.isDeprecated()).findFirst();
    }

    public void setName(final UUID caseId, final UUID defendantId, final String newFirstName, final String newLastName,
                        final LocalDate newDateOfBirth, final ZonedDateTime dateAdded) {
        final CaseSearchResult newEntry = getNewEntry(caseId, defendantId, newFirstName, newLastName, newDateOfBirth, dateAdded);
        caseSearchResults.forEach(entry -> {
            entry.setDeprecated(true);
            ofNullable(newFirstName).ifPresent(entry::setCurrentFirstName);
            ofNullable(newLastName).ifPresent(entry::setCurrentLastName);
        });
        caseSearchResults.add(newEntry);
    }

    private CaseSearchResult getNewEntry(final UUID caseId, final UUID defendantId, final String newFirstName, final String newLastName,
                                         final LocalDate newDateOfBirth, final ZonedDateTime dateAdded) {
        final Optional<CaseSearchResult> latest = getCurrent();
        final CaseSearchResult newEntry = new CaseSearchResult(caseId, defendantId, newFirstName, newLastName, newDateOfBirth, dateAdded);
        latest.ifPresent(r -> {
            newEntry.setWithdrawalRequestedDate(r.getWithdrawalRequestedDate());
            newEntry.setFirstName(ofNullable(newFirstName).orElse(r.getFirstName()));
            newEntry.setLastName(ofNullable(newLastName).orElse(r.getLastName()));
        });
        return newEntry;
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

    public boolean hasNameChanged(final String newFirstName, final String newLastName) {
        return getCurrent()
                .map(old ->
                        !StringUtils.equalsIgnoreCase(newFirstName, old.getFirstName()) ||
                                !StringUtils.equalsIgnoreCase(newLastName, old.getLastName()))
                .orElse(true);
    }
}
