package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.CaseDocumentProcessor;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import javax.inject.Inject;
import javax.inject.Named;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO remove - ATCM-4293

/**
 * @deprecated required for legacy public events, replaced by {@link CaseDocumentProcessor}
 */
@Deprecated
@Named
public class AddMaterialToCase implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddMaterialToCase.class);

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private MetadataHelper metadataHelper;

    @Override
    public void execute(final DelegateExecution execution) {

        LOGGER.info("Task '{}' started for process {}", execution.getCurrentActivityName(), execution.getId());

        final String metadataAsString = execution.getVariable("metadata", String.class);
        final String caseId = execution.getVariable("caseId", String.class);
        final String documentReference = execution.getVariable("documentReference", String.class);
        final String documentType = execution.getVariable("documentType", String.class);
        final String materialId = execution.getVariable("materialId", String.class);

        final Metadata metadata = metadataHelper.metadataFromString(metadataAsString);

        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("id", documentReference)
                .add("caseId", caseId)
                .add("materialId", materialId)
                .add("documentType", documentType)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadata).withName("sjp.command.add-case-document"), payload);

        sender.send(envelope);
    }

}
