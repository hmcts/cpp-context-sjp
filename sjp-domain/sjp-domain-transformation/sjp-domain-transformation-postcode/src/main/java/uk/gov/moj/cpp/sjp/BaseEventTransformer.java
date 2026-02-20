package uk.gov.moj.cpp.sjp;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.sjp.service.AddressService.isPostcodeNormalized;
import static uk.gov.moj.cpp.sjp.service.AddressService.normalizePostcode;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public abstract class BaseEventTransformer implements EventTransformation {

    protected static final String POSTCODE = "postcode";
    protected static final String ADDRESS = "address";

    protected JsonObject replacePostcodeInAddress(final JsonObject objectToTransform) {
        return replacePostcodeInAddress(objectToTransform, ADDRESS);
    }

    protected JsonObject replacePostcodeInAddress(final JsonObject objectToTransform, final String addressObject) {
        final JsonObject addressToTransform = objectToTransform
                .getJsonObject(addressObject);

        final String postcode = addressToTransform
                .getString(POSTCODE);

        final JsonObjectBuilder transformedAddress = createObjectBuilderWithFilter(addressToTransform,
                field -> !POSTCODE.equalsIgnoreCase(field))
                .add(POSTCODE, normalizePostcode(postcode));

        return createObjectBuilderWithFilter(objectToTransform, field -> !addressObject.equalsIgnoreCase(field))
                .add(addressObject, transformedAddress)
                .build();
    }

    protected JsonObject transformObject(final String objectName, final JsonObject rootObject) {
        return createObjectBuilderWithFilter(rootObject,
                field -> !objectName.equalsIgnoreCase(field))
                .add(objectName, replacePostcodeInAddress(rootObject.getJsonObject(objectName)))
                .build();
    }

    protected JsonObject checkAndTransformObject(final String objectName, final JsonObject rootObject) {
        if (containsAddressWithPostcodeToTransform(ADDRESS, objectName, rootObject)) {
            return transformObject(objectName, rootObject);
        }
        return rootObject;
    }

    protected boolean containsAddressWithPostcodeToTransform(final String addressName, final String objectName, final JsonObject rootObject) {
        return getString(rootObject, objectName, addressName, POSTCODE).map(e -> !isPostcodeNormalized(e)).orElse(false);
    }

    protected boolean containsPostcodeToTransform(final String addressField, final JsonObject jsonObject) {
        return getString(jsonObject, addressField, POSTCODE).map(e -> !isPostcodeNormalized(e)).orElse(false);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
    }

    public abstract String getEventName();

}
