package uk.gov.moj.cpp.sjp.query.view;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.CaseCourtExtractView;
import uk.gov.moj.cpp.sjp.query.view.service.CourtExtractDataService;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_VIEW)
public class SjpCaseCourtExtractView {

    @Inject
    private CourtExtractDataService courtExtractDataService;

    @Handles("sjp.query.case-court-extract")
    public Envelope<CaseCourtExtractView> getCaseCourtExtractData(final JsonEnvelope queryEnvelope) {
        final UUID caseId = fromString(queryEnvelope.payloadAsJsonObject().getString("caseId"));

        final CaseCourtExtractView caseCourtExtractView = courtExtractDataService.getCourtExtractData(caseId).orElse(null);

        return envelop(caseCourtExtractView)
                        .withName("sjp.query.case-court-extract")
                        .withMetadataFrom(queryEnvelope);

    }

}
