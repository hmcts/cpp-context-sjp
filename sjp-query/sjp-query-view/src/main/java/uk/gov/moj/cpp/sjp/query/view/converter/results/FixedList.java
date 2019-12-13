package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static java.util.UUID.fromString;

import uk.gov.moj.cpp.sjp.query.view.converter.fixedlists.CollectionOrderFixedListConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.fixedlists.FixedListConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.fixedlists.NoActionFixedListConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.fixedlists.PayWithinDaysFixedListConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.fixedlists.SUMRCCFixedListConverter;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public enum FixedList {

    CREDITOR_NAME(fromString("6e5f1afe-e35f-11e8-9f32-f2801f1b9fd1"),  NoActionFixedListConverter::new),
    PAY_WITHIN_DAYS(fromString("cd4cc782-5dd6-11e8-9c2d-fa7ae01bbebc"), PayWithinDaysFixedListConverter::new) ,
    INSTALMENTS_PAYMENT_FREQUENCY(fromString("e555e078-5dd8-11e8-9c2d-fa7ae01bbebc"), NoActionFixedListConverter::new),
    COLLECTION_ORDER_TYPE(fromString("d7d75420-aace-11e8-98d0-529269fb1459"), CollectionOrderFixedListConverter::new),
    SUMRCC_REFERRAL_REASONS(fromString("a47a312a-79fe-4f3e-84ab-63a39f52bc75"), SUMRCCFixedListConverter::new) ;

    private final UUID id;
    private final Supplier<FixedListConverter> supplier;

    FixedList(final UUID id, final Supplier<FixedListConverter> fixedListConverterSupplier) {
        this.id = id;
        this.supplier = fixedListConverterSupplier;
    }

    public UUID getId() {
        return this.id;
    }

    public Optional<String> mapValue(final String value){
        return supplier.get().convert(value);
    }


}
