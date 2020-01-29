package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.query.view.converter.results.*;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.slf4j.Logger;

public enum ResultCode {

    FVEBD(fromString("5edd3a3a-8dc7-43e4-96c4-10fed16278ac"), FVEBDResultConverter::new),
    EXPEN(fromString("fcb26a5f-28cc-483e-b430-d823fac808df"), EXPENConverter::new),
    FO(fromString("969f150c-cd05-46b0-9dd9-30891efcc766"), FOResultCodeConverter::new),
    FCOMP(fromString("ae89b99c-e0e3-47b5-b218-24d4fca3ca53"), FCOMPResultCodeConverter::new),
    FCOST(fromString("76d43772-0660-4a33-b5c6-8f8ccaf6b4e3"), FCOSTResultCodeConverter::new),
    FVS(fromString("e866cd11-6073-4fdf-a229-51c9d694e1d0"), FVSResultCodeConverter::new),
    GPTAC( fromString("9161f3cb-e821-44e5-a9ee-4680b358a037"), GPTACResultCodeConverter::new),
    CD(fromString("554c2622-c1cc-459e-a98d-b7f317ab065c"), CDResultCodeConverter::new),
    WDRNNOT( fromString("6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc"), WDRNNOTResultCodeConverter::new),
    RLSUM( fromString("a09bbfa0-5dd5-11e8-9c2d-fa7ae01bbebc"), RLSUMResultCodeConverter::new),
    RLSUMI( fromString("d6e93aae-5dd7-11e8-9c2d-fa7ae01bbebc"), RLSUMIResultCodeConverter::new),
    RINSTL( fromString("9ba8f03a-5dda-11e8-9c2d-fa7ae01bbebc"), RINSTLResultCodeConverter::new),
    LSUM(fromString("bcb5a496-f7cf-11e8-8eb2-f2801f1b9fd1"), LSUMResultCodeConverter::new),
    LSUMI(fromString("272d1ec2-634b-11e8-adc0-fa7ae01bbebc"), LSUMIResultCodeConverter::new),
    INSTL(fromString("6d76b10c-64c4-11e8-adc0-fa7ae01bbebc"), INSTLResultCodeConverter::new),
    ABDC(fromString("f7dfefd2-64c6-11e8-adc0-fa7ae01bbebc"), ABDCResultCodeConverter::new),
    NOVS(fromString("204fc6b8-d6c9-4fb8-acd0-47d23c087625"), NOVSResultCodeConverter::new),
    NCR(fromString("29e02fa1-42ce-4eec-914e-e62508397a16"), NCRResultCodeConverter::new),
    NCOSTS(fromString("baf94928-04ae-4609-8e96-efc9f081b2be"), NCOSTSResultCodeConverter::new),
    AEOC(fromString("bdb32555-8d55-4dc1-b4b6-580db5132496"), AEOCResultCodeConverter::new),
    AD( fromString("b9c6047b-fb84-4b12-97a1-2175e4b8bbac"), ADResultCodeConverter::new),
    COLLO(fromString("9ea0d845-5096-44f6-9ce0-8ae801141eac"), COLLOResultCodeConverter::new),
    NCOLLO(fromString("615313b5-0647-4d61-b7b8-6b36265d8929"), NCOLLOResultCodeConverter::new),
    D(fromString("14d66587-8fbe-424f-a369-b1144f1684e3"), DResultCodeConverter::new),
    NSP(fromString("49939c7c-750f-403e-9ce1-f82e3e568065"), NSPResultCodeConverter::new),
    TFOOUT(fromString("1e96d1a9-9618-4ddd-a925-ca6a0ef86018"), TFOOUTResultCodeConverter::new),
    SUMRCC(fromString("600edfc3-a584-4f9f-a52e-5bb8a99646c1"), SUMRCCResultCodeConverter::new),
    ADJOURNSJP(fromString("f7784e82-20b5-4d2c-b174-6fd57ebf8d7c"), ADJOURNSJPResultCodeConverter::new),
    RSJP(fromString("60ac9c98-eeec-4e48-823e-cd3f9fadd854"), RSJPResultCodeConverter::new),
    SUMRTO(fromString("3d2c05b3-fcd6-49c2-b5a9-52855be7f90a"), SUMRTOResultCodeConverter::new),
    SJPR(fromString("0149ab92-5466-11e8-9c2d-fa7ae01bbebc"), RSJPResultCodeConverter::new);


    private static final Logger LOGGER = getLogger(ResultCode.class);
    private UUID resultDefinitionId;
    private Supplier<ResultCodeConverter> converter;

    ResultCode(final UUID resultDefinitionId, final Supplier<ResultCodeConverter> converter) {
        this.resultDefinitionId = resultDefinitionId;
        this.converter = converter;
    }

    public UUID getResultDefinitionId() {
        return resultDefinitionId;
    }

    public static Optional<ResultCode> parse(String resultCode) {

        try {
            return of(ResultCode.valueOf(resultCode));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("No result code present for {}", resultCode);
        }
        return Optional.empty();
    }

    public JsonArrayBuilder createPrompts( final JsonObject result, final OffenceDataSupplier offenceDataSupplier){
        return converter.get().createPrompts(result, offenceDataSupplier);
    }
}
