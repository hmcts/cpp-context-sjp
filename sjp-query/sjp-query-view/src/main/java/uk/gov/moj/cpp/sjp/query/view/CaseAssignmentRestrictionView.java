package uk.gov.moj.cpp.sjp.query.view;


import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAssignmentRestrictionRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class CaseAssignmentRestrictionView {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAssignmentRestrictionView.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    private CaseAssignmentRestrictionRepository caseAssignmentRestrictionRepository;

    @Handles("sjp.query.case-assignment-restriction")
    public JsonEnvelope getCaseAssignmentRestriction(final JsonEnvelope query) {
        final String prosecutingAuthority = query.payloadAsJsonObject().getString("prosecutingAuthority");
        final List<CaseAssignmentRestriction> caseAssignmentRestrictionList = caseAssignmentRestrictionRepository.findByProsecutingAuthority(prosecutingAuthority, LocalDate.now());

        Optional<JsonObject> restriction = Optional.empty();

        if(!caseAssignmentRestrictionList.isEmpty()) {
            restriction = Optional.of(caseAssignmentRestrictionList.get(0))
                    .map(assignmentRestriction -> Json.createObjectBuilder()
                            .add("prosecutingAuthority", assignmentRestriction.getProsecutingAuthority())
                            .add("dateTimeCreated", assignmentRestriction.getDateTimeCreated().toString())
                            .add("exclude", getArrayBuilder(assignmentRestriction.getExclude()))
                            .add("includeOnly", getArrayBuilder(assignmentRestriction.getIncludeOnly()))
                            .build());
        }

        return enveloper.withMetadataFrom(query, "sjp.query.case-assignment-restriction").apply(restriction.orElse(null));
    }

    private JsonArrayBuilder getArrayBuilder(final String data) {

        try {
            final List<String> list = OBJECT_MAPPER.readValue(data, List.class);
            final JsonArrayBuilder builder = createArrayBuilder();
            list.forEach(builder::add);
            return builder;
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IllegalArgumentException("Unable to get jsonb value");
        }

    }
}
