package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings("squid:S1168")
public class CourtDocumentTransformer {

    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";
    public static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    public static final String SEQ_NUM = "seqNum";
    public static final String SECTION = "section";
    public static final String CPP_GROUP = "cppGroup";
    public static final String GROUP_NAME = "groupName";

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    public CourtDocument transform(final CourtDocumentView courtDocumentView, final JsonEnvelope jsonEnvelope) {

        final CourtDocument courtDocumentInput = jsonObjectToObjectConverter.convert(objectToJsonObjectConverter.convert(courtDocumentView), CourtDocument.class);

        final JsonObject documentTypeDataJson = referenceDataService
                .getDocumentTypeAccessData(courtDocumentView.getDocumentTypeId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException(SECTION, courtDocumentView.getDocumentTypeId().toString()));

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withMaterials(courtDocumentInput.getMaterials())
                .withCourtDocumentId(courtDocumentInput.getCourtDocumentId())
                .withDocumentCategory(courtDocumentInput.getDocumentCategory())
                .withDocumentTypeId(courtDocumentInput.getDocumentTypeId())
                .withName(courtDocumentInput.getName())
                .withMimeType(courtDocumentInput.getMimeType())
                .withContainsFinancialMeans(courtDocumentInput.getContainsFinancialMeans())
                .build();

        return buildCourtDocumentWithMaterialUserGroups(courtDocument, documentTypeDataJson);
    }

    private CourtDocument buildCourtDocumentWithMaterialUserGroups(final CourtDocument courtDocument, final JsonObject documentTypeData) {

        final JsonObject documentTypeRBACData = documentTypeData.getJsonObject(COURT_DOCUMENT_TYPE_RBAC);
        final Integer seqNum = Integer.parseInt(documentTypeData.getJsonNumber(SEQ_NUM) == null ? "0" : documentTypeData.getJsonNumber(SEQ_NUM).toString());

        final List<Material> materials = courtDocument.getMaterials().stream()
                .map(material -> enrichMaterial(material, documentTypeRBACData)).collect(Collectors.toList());

        return CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocument.getCourtDocumentId())
                .withDocumentCategory(courtDocument.getDocumentCategory())
                .withDocumentTypeDescription(documentTypeData.getString(SECTION))
                .withDocumentTypeId(courtDocument.getDocumentTypeId())
                .withName(courtDocument.getName())
                .withMimeType(courtDocument.getMimeType())
                .withMaterials(materials)
                .withContainsFinancialMeans(courtDocument.getContainsFinancialMeans())
                .withSeqNum(seqNum)
                .withDocumentTypeRBAC(DocumentTypeRBAC.
                        documentTypeRBAC()
                        .withUploadUserGroups(getRBACUserGroups(documentTypeRBACData, UPLOAD_ACCESS))
                        .withReadUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                        .withDownloadUserGroups(getRBACUserGroups(documentTypeRBACData, DOWNLOAD_ACCESS))
                        .withDeleteUserGroups(getRBACUserGroups(documentTypeRBACData, DELETE_ACCESS))
                        .build())
                .build();
    }

    private Material enrichMaterial(Material material, final JsonObject documentTypeRBACData) {
        return Material.material()
                .withId(material.getId())
                .withGenerationStatus(material.getGenerationStatus())
                .withName(material.getName())
                .withUploadDateTime(material.getUploadDateTime() != null ? material.getUploadDateTime() : ZonedDateTime.now(ZoneOffset.UTC))
                .withReceivedDateTime(material.getReceivedDateTime())
                .withUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                .build();
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeData, final String accessLevel) {

        final JsonArray documentTypeRBACJsonArray = documentTypeData.getJsonArray(accessLevel);
        if (null == documentTypeRBACJsonArray || documentTypeRBACJsonArray.isEmpty()) {
            return null;
        }

        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject(CPP_GROUP).getString(GROUP_NAME)).collect(toList());
    }


}
