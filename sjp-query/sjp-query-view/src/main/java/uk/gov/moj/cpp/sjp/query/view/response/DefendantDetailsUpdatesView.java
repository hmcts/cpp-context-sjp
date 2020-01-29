package uk.gov.moj.cpp.sjp.query.view.response;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the instance returned when defendant personal detail updates are requested.
 */
public class DefendantDetailsUpdatesView {

    /**
     * The total number of defendants with details changes.
     */
    private final int total;

    /**
     * Contains one entry for each defendant with a personal detail change.
     */
    private final List<DefendantDetailsUpdate> defendantDetailsUpdates;

    public DefendantDetailsUpdatesView(
            final int total,
            final List<DefendantDetailsUpdate> defendantDetailsUpdates) {

        this.total = total;
        this.defendantDetailsUpdates = copyOf(defendantDetailsUpdates);
    }

    public int getTotal() {
        return total;
    }

    public List<DefendantDetailsUpdate> getDefendantDetailsUpdates() {
        return copyOf(defendantDetailsUpdates);
    }

    public static DefendantDetailsUpdatesView of(
            int total,
            List<UpdatedDefendantDetails> updatedDefendantDetails) {

        return new DefendantDetailsUpdatesView(
                total,
                updatedDefendantDetails.stream()
                        .map(DefendantDetailsUpdate::of)
                        .collect(toList()));
    }

    /**
     * Defines the details regarding personal detail updates for a defendant.
     */
    public static class DefendantDetailsUpdate {

        /**
         * The defendant first name.
         */
        private final String firstName;

        /**
         * The defendant last name.
         */
        private final String lastName;

        /**
         * The id of the defendant that the update is linked to.
         */
        private final String defendantId;

        /**
         * The case id for the defendant update.
         */
        private final String caseId;

        /**
         * The case URN for the defendant update.
         */
        private final String caseUrn;

        /**
         * The defendant date of birth.
         */
        private final String dateOfBirth;

        /**
         * Defines if the name was updated.
         */
        private boolean nameUpdated;

        /**
         * Defines if the date of birth was updated.
         */
        private boolean dateOfBirthUpdated;

        /**
         * Defines if the address was updated.
         */
        private boolean addressUpdated;

        /**
         * The date the update happened.
         */
        private final String updatedOn;

        private String region;

        @SuppressWarnings("squid:S00107")
        public DefendantDetailsUpdate(
                final String firstName,
                final String lastName,
                final String defendantId,
                final String caseId,
                final String caseUrn,
                final String dateOfBirth,
                final boolean nameUpdated,
                final boolean dateOfBirthUpdated,
                final boolean addressUpdated,
                final String updatedOn,
                final String region) {

            this.firstName = firstName;
            this.lastName = lastName;
            this.defendantId = defendantId;
            this.caseId = caseId;
            this.caseUrn = caseUrn;
            this.dateOfBirth = dateOfBirth;
            this.nameUpdated = nameUpdated;
            this.dateOfBirthUpdated = dateOfBirthUpdated;
            this.addressUpdated = addressUpdated;
            this.updatedOn = updatedOn;
            this.region = region;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getDefendantId() {
            return defendantId;
        }

        public String getCaseUrn() {
            return caseUrn;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public boolean isNameUpdated() {
            return nameUpdated;
        }

        public boolean isDateOfBirthUpdated() {
            return dateOfBirthUpdated;
        }

        public boolean isAddressUpdated() {
            return addressUpdated;
        }

        public String getUpdatedOn() {
            return updatedOn;
        }

        public String getCaseId() {
            return caseId;
        }

        public String getRegion() {
            return region;
        }

        public static DefendantDetailsUpdate of(UpdatedDefendantDetails defendantDetail) {
            return new DefendantDetailsUpdate(
                    defendantDetail.getFirstName(),
                    defendantDetail.getLastName(),
                    defendantDetail.getDefendantId().toString(),
                    defendantDetail.getCaseId().toString(),
                    defendantDetail.getCaseUrn(),
                    ofNullable(defendantDetail.getDateOfBirth())
                            .map(dob -> dob.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .orElse(null),
                    nonNull(defendantDetail.getNameUpdatedAt()),
                    nonNull(defendantDetail.getDateOfBirthUpdatedAt()),
                    nonNull(defendantDetail.getAddressUpdatedAt()),
                    defendantDetail.getMostRecentUpdateDate()
                            .map(DateTimeFormatter.ISO_LOCAL_DATE::format)
                            .orElse(DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.now())),
                    defendantDetail.getRegion());
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final DefendantDetailsUpdate that = (DefendantDetailsUpdate) o;

            final boolean personalDetailUpdateFlagsMatch = nameUpdated == that.nameUpdated &&
                    dateOfBirthUpdated == that.dateOfBirthUpdated &&
                    addressUpdated == that.addressUpdated;

            final boolean detailsMatch = Objects.equals(firstName, that.firstName) &&
                    Objects.equals(caseId, that.caseId) &&
                    Objects.equals(caseUrn, that.caseUrn) &&
                    Objects.equals(dateOfBirth, that.dateOfBirth);

            return personalDetailUpdateFlagsMatch &&
                    detailsMatch &&
                    Objects.equals(updatedOn, that.updatedOn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    firstName,
                    caseUrn,
                    caseId,
                    dateOfBirth,
                    nameUpdated,
                    dateOfBirthUpdated,
                    addressUpdated,
                    updatedOn);
        }
    }
}
