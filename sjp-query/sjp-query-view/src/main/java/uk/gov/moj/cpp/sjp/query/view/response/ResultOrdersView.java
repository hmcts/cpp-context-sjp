package uk.gov.moj.cpp.sjp.query.view.response;

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

            public Builder setDefendant(UUID personId) {
                casesWithOrderView.defendant = new DefendantView(personId);
                return this;
            }

            public Builder setOrder(UUID materialId, ZonedDateTime dateMaterialAdded) {
                casesWithOrderView.order = new OrderView(materialId, dateMaterialAdded);
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
            private UUID personId;

            public DefendantView(UUID personId) {
                this.personId = personId;
            }

            public UUID getPersonId() {
                return personId;
            }
        }

        public static class OrderView {
            private UUID materialId;

            private ZonedDateTime addedAt;

            public OrderView(UUID materialId, ZonedDateTime addedAt) {
                this.materialId = materialId;
                this.addedAt = addedAt;
            }

            public UUID getMaterialId() {
                return materialId;
            }

            public ZonedDateTime getAddedAt() {
                return addedAt;
            }
        }
    }
}
