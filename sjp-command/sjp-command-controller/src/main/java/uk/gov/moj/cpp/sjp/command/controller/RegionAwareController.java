package uk.gov.moj.cpp.sjp.command.controller;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public abstract class RegionAwareController extends BaseController {

    private static final String POSTCODE = "postcode";
    private static final String REGION = "region";
    private static final String ADDRESS = "address";
    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";

    @Inject
    protected ReferenceDataService referenceDataService;

    private String getNationalCourtCode(final String defendentPostcode) {
        final Optional<JsonObject> jsonObject = ofNullable(referenceDataService.getEnforcementArea(defendentPostcode));
        return jsonObject.map(e -> e.getJsonObject(LOCAL_JUSTICE_AREA))
                .map(obj -> obj.getString(NATIONAL_COURT_CODE))
                .orElse(null);
    }

    private String getLocalJusticeAreaRegion(final String nationalCourtCode) {
        final JsonObject jsonObject = referenceDataService.getLocalJusticeAreas(nationalCourtCode);
        final List<JsonObject> jsonObjectList = jsonObject.getJsonArray("localJusticeAreas").getValuesAs(JsonObject.class);
        return jsonObjectList.stream()
                .filter(e -> nationalCourtCode.equals(e.getString(NATIONAL_COURT_CODE)))
                .map(o -> o.getString(REGION, null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String getRegionFromDefendantPostCode(final String postcode) {
        return ofNullable(postcode)
                .map(this::getNationalCourtCode)
                .map(this::getLocalJusticeAreaRegion)
                .orElse(null);
    }

    JsonObject enrichJsonWithRegion(final JsonObject jsonObject) {
        if (jsonObject.containsKey(ADDRESS)) {
            final String defendantPostcode = jsonObject.getJsonObject(ADDRESS).getString(POSTCODE);
            final String region = getRegionFromDefendantPostCode(defendantPostcode);
            if (nonNull(region)) {
                return addJsonField(jsonObject, REGION, region);
            }
        }
        return jsonObject;
    }
}
