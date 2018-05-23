package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class CaseSearchResultList {

    private final List<CaseSearchResult> caseSearchResults;

    public CaseSearchResultList(final List<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    private Optional<CaseSearchResult> getCurrent() {
        return caseSearchResults.stream().filter(r -> !r.isDeprecated()).findFirst();
    }

    public void setName(final UUID caseId, final String newFirstName, final String newLastName,
                        final LocalDate newDateOfBirth, final ZonedDateTime dateAdded) {
        final Optional<CaseSearchResult> latest = getCurrent();

        caseSearchResults.forEach(entry -> {
            entry.setDeprecated(true);
            entry.setCurrentFirstName(newFirstName);
            entry.setCurrentLastName(newLastName);
        });

        final CaseSearchResult newEntry = new CaseSearchResult(caseId, newFirstName, newLastName, newDateOfBirth, dateAdded);

        latest.ifPresent(r -> {
            newEntry.setPleaDate(r.getPleaDate());
            newEntry.setWithdrawalRequestedDate(r.getWithdrawalRequestedDate());
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

    public boolean hasNameChanged(final String newFirstName, final String newLastName) {
        return getCurrent()
                .map(old ->
                        !newLastName.equalsIgnoreCase(old.getLastName()) ||
                                !newFirstName.equalsIgnoreCase(old.getFirstName()))
                .orElse(true);
    }
}
