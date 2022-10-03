package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ResultOrdersView {

    private List<ResultOrderView> resultOrders = new ArrayList<>();

    public List<ResultOrderView> getResultOrders() {
        return resultOrders;
    }

    public void addResultOrder(ResultOrderView resultOrderView) {
        resultOrders.add(resultOrderView);
    }

    public static ResultOrderView.Builder createResultOrderBuilder() {
        return new ResultOrderView.Builder();
    }

    public static class ResultOrderView {
        private UUID caseId;
        private String urn;
        private DefendantView defendant;
        private DefendantView.OrderView order;

        public UUID getCaseId() {
            return caseId;
        }

        public String getUrn() {
            return urn;
        }

        public DefendantView getDefendant() {
            return defendant;
        }

        public DefendantView.OrderView getOrder() {
            return order;
        }

        public static class Builder {
            private ResultOrderView casesWithOrderView = new ResultOrderView();

            public Builder setCaseId(UUID caseId) {
                casesWithOrderView.caseId = caseId;
                return this;
            }

            public Builder setUrn(String urn) {
                casesWithOrderView.urn = urn;
                return this;
            }

            public Builder setDefendant(final DefendantDetail defendantDetail) {
               final PersonalDetails personalDetails=defendantDetail.getPersonalDetails();
                casesWithOrderView.defendant = new DefendantView(
                        personalDetails.getTitle(),
                        personalDetails.getFirstName(),
                        personalDetails.getLastName(),
                        personalDetails.getDateOfBirth(),
                        null,
                        new AddressView(defendantDetail.getAddress()));
                return this;
            }

            public Builder setOrder(final UUID documentId, final ZonedDateTime dateMaterialAdded) {
                casesWithOrderView.order = new DefendantView.OrderView(documentId, dateMaterialAdded);
                return this;
            }

            public ResultOrderView build() {
                Objects.requireNonNull(casesWithOrderView.caseId, "caseId");
                Objects.requireNonNull(casesWithOrderView.urn, "urn");
                Objects.requireNonNull(casesWithOrderView.defendant, "defendant");
                Objects.requireNonNull(casesWithOrderView.order, "order");
                return casesWithOrderView;
            }
        }

        public static class DefendantView {
            private final String title;
            private final String firstName;
            private final String lastName;
            private final LocalDate dateOfBirth;
            private final String legalEntityName;
            private final AddressView address;

            public DefendantView(final String title, final String firstName, final String lastName, final LocalDate dateOfBirth, final String legalEntityName, final AddressView address) {
                this.title = title;
                this.firstName = firstName;
                this.lastName = lastName;
                this.dateOfBirth = dateOfBirth;
                this.legalEntityName = legalEntityName;
                this.address = address;
            }

            public String getTitle() {
                return title;
            }

            public String getFirstName() {
                return firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public LocalDate getDateOfBirth() {
                return dateOfBirth;
            }

            public AddressView getAddress() {
                return address;
            }

            public String getLegalEntityName() {
                return legalEntityName;
            }


            public static class OrderView {
                private UUID documentId;

                private ZonedDateTime addedAt;

                public OrderView(final UUID documentId, final ZonedDateTime addedAt) {
                    this.documentId = documentId;
                    this.addedAt = addedAt;
                }

                public UUID getDocumentId() {
                    return documentId;
                }

                public ZonedDateTime getAddedAt() {
                    return addedAt;
                }
            }
        }
    }
}
