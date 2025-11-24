package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EnterpriseIdAssociatedListenerTest {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String ENTERPRISE_ID_PROPERTY = "enterpriseId";

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private EnterpriseIdAssociatedListener enterpriseIdAssociatedListener;

    private CaseDetail caseDetail = new CaseDetail();

    @Test
    public void shouldStoreEnterpriseIdOnCaseDetails() {
        final UUID caseId = randomUUID();
        final String enterpriseId = "2K2SLYFC743H";
        final JsonEnvelope event = createEnterpriseIdAssociatedEventEnvelope(caseId, enterpriseId);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        assertThat(caseDetail.getEnterpriseId(), is(nullValue()));
        enterpriseIdAssociatedListener.associateEnterpriseIdToCase(event);

        assertThat(caseDetail.getEnterpriseId(), equalTo(enterpriseId));
    }

    private JsonEnvelope createEnterpriseIdAssociatedEventEnvelope(final UUID caseId, final String enterpriseId) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add(CASE_ID_PROPERTY, caseId.toString())
                .add(ENTERPRISE_ID_PROPERTY, enterpriseId);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.events.enterprise-id-associated"),
                payload.build());
    }
}