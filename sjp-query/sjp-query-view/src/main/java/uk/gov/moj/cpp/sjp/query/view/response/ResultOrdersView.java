package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
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
        private OrderView order;

        public UUID getCaseId() {
            return caseId;
        }

        public String getUrn() {
            return urn;
        }

        public DefendantView getDefendant() {
            return defendant;
        }

        public OrderView getOrder() {
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

            public Builder setDefendant(final PersonalDetails personalDetails) {
                casesWithOrderView.defendant = new DefendantView(
                        personalDetails.getTitle(),
                        personalDetails.getFirstName(),
                        personalDetails.getLastName(),
                        personalDetails.getDateOfBirth(),
                        new DefendantView.DefendantAddressView(personalDetails.getAddress()));
                return this;
            }

            public Builder setOrder(final UUID documentId, final ZonedDateTime dateMaterialAdded) {
                casesWithOrderView.order = new OrderView(documentId, dateMaterialAdded);
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
            private final DefendantAddressView address;

            public DefendantView(final String title, final String firstName, final String lastName, final LocalDate dateOfBirth, final DefendantAddressView address) {
                this.title = title;
                this.firstName = firstName;
                this.lastName = lastName;
                this.dateOfBirth = dateOfBirth;
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

            public DefendantAddressView getAddress() {
                return address;
            }

            public static class DefendantAddressView {
                private final String address1;
                private final String address2;
                private final String address3;
                private final String address4;
                private final String postCode;

                public DefendantAddressView(final Address address) {
                    this.address1 = address.getAddress1();
                    this.address2 = address.getAddress2();
                    this.address3 = address.getAddress3();
                    this.address4 = address.getAddress4();
                    this.postCode = address.getPostcode();
                }

                public String getAddress1() {
                    return address1;
                }

                public String getAddress2() {
                    return address2;
                }

                public String getAddress3() {
                    return address3;
                }

                public String getAddress4() {
                    return address4;
                }

                public String getPostCode() {
                    return postCode;
                }
            }
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
