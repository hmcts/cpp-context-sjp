package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;

import java.util.Optional;

import javax.json.JsonObject;

public class OffenceDataSupplier {
    private final JsonEnvelope sourceEnvelope;
    private final CaseView caseView;
    private final ReferenceDataService referenceDataService;
    private JsonObject allFixedList;
    private Employer employer;

    private OffenceDataSupplier(final JsonEnvelope sourceEnvelope, final CaseView caseView, final Employer employer, final ReferenceDataService referenceDataService) {
        this.sourceEnvelope = sourceEnvelope;
        this.caseView = caseView;
        this.employer = employer;
        this.referenceDataService = referenceDataService;
    }

    public Optional<JsonObject> getAllFixedList() {
        if (allFixedList == null) {
            allFixedList = referenceDataService
                    .getAllFixedList(sourceEnvelope)
                    .orElse(null);
        }
        return ofNullable(allFixedList);
    }

    private Employer getEmployerDetails() {
        return employer;
    }

    public CaseView getCaseView() {
        return caseView;
    }

    public Employer getEmployerSupplier() {
        return getEmployerDetails();
    }

    public JsonEnvelope getSourceEnvelope() {
        return sourceEnvelope;
    }

    public static OffenceDataSupplier create(final JsonEnvelope sourceEnvelope, final CaseView caseView, Employer employer, final ReferenceDataService referenceDataService) {
        return new OffenceDataSupplier(sourceEnvelope, caseView, employer, referenceDataService);
    }
}
