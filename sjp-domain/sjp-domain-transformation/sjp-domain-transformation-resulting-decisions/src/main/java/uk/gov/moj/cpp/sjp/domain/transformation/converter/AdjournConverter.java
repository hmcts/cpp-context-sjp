package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.OFFENCE_DECISION_INFORMATION;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.TYPE;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.VERDICT;

import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import javax.json.JsonObject;

import com.google.common.annotations.VisibleForTesting;

public class AdjournConverter implements Converter {

    private static AdjournConverter adjournConverter;

    private SjpViewStoreService sjpViewStoreService;

    private AdjournConverter() {
        this(SjpViewStoreService.getInstance());
    }

    @VisibleForTesting
    AdjournConverter(SjpViewStoreService sjpViewStoreService) {
        this.sjpViewStoreService = sjpViewStoreService;
    }

    public static synchronized AdjournConverter getInstance() {
        if (adjournConverter == null) {
            adjournConverter = new AdjournConverter();
        }
        return adjournConverter;
    }

    @Override
    public JsonObject convert(final JsonObject decisionPayload,
                              final String pleaType,
                              final String verdict) {

        return buildOffenceDecision(decisionPayload, verdict);
    }

    private JsonObject buildOffenceDecision(final JsonObject decisionPayload,
                                            final String verdict) {

        return createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DecisionTypeCode.ADJOURNSJP.getResultDecision())
                .add(OFFENCE_DECISION_INFORMATION, createArrayBuilder().add(createObjectBuilder()
                        .add(OFFENCE_ID, sjpViewStoreService.getOffenceId(decisionPayload.getString("caseId"))
                                .orElseThrow(() -> new TransformationException("OffenceId cannot be null")))
                        .add(VERDICT, verdict))
                )
                .add("reason", decisionPayload.getString("reason", null))
                .add("adjournTo", decisionPayload.getString("adjournedTo"))
                .build();
    }
}
