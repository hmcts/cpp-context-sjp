package uk.gov.moj.cpp.sjp.query.view.converter;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.query.view.converter.results.ABDCResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.ADJOURNSJPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.ADResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.AEOCResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.CDResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.COLLOResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.D45ResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.DDDResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.DDOResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.DDPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.DPRResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.DResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.EXPENConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FCOMPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FCOSTResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FOResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FVEBDResultConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FVSResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.GPTACResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.INSTLResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.LEAResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.LENResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.LEPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.LSUMIResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.LSUMResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.NCOLLOResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.NCOSTSResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.NCRResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.NOVSResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.NSPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.RINSTLResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.RLSUMIResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.RLSUMResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.RSJPResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.ResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.SETASIDEResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.SUMRCCResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.SUMRTOResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.TFOOUTResultCodeConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.results.WDRNNOTResultCodeConverter;
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
    GPTAC(fromString("9161f3cb-e821-44e5-a9ee-4680b358a037"), GPTACResultCodeConverter::new),
    CD(fromString("554c2622-c1cc-459e-a98d-b7f317ab065c"), CDResultCodeConverter::new),
    WDRNNOT(fromString("6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc"), WDRNNOTResultCodeConverter::new),
    RLSUM(fromString("a09bbfa0-5dd5-11e8-9c2d-fa7ae01bbebc"), RLSUMResultCodeConverter::new),
    RLSUMI(fromString("d6e93aae-5dd7-11e8-9c2d-fa7ae01bbebc"), RLSUMIResultCodeConverter::new),
    RINSTL(fromString("9ba8f03a-5dda-11e8-9c2d-fa7ae01bbebc"), RINSTLResultCodeConverter::new),
    LSUM(fromString("bcb5a496-f7cf-11e8-8eb2-f2801f1b9fd1"), LSUMResultCodeConverter::new),
    LSUMI(fromString("272d1ec2-634b-11e8-adc0-fa7ae01bbebc"), LSUMIResultCodeConverter::new),
    INSTL(fromString("6d76b10c-64c4-11e8-adc0-fa7ae01bbebc"), INSTLResultCodeConverter::new),
    ABDC(fromString("f7dfefd2-64c6-11e8-adc0-fa7ae01bbebc"), ABDCResultCodeConverter::new),
    NOVS(fromString("204fc6b8-d6c9-4fb8-acd0-47d23c087625"), NOVSResultCodeConverter::new),
    NCR(fromString("29e02fa1-42ce-4eec-914e-e62508397a16"), NCRResultCodeConverter::new),
    NCOSTS(fromString("baf94928-04ae-4609-8e96-efc9f081b2be"), NCOSTSResultCodeConverter::new),
    AEOC(fromString("bdb32555-8d55-4dc1-b4b6-580db5132496"), AEOCResultCodeConverter::new),
    AD(fromString("b9c6047b-fb84-4b12-97a1-2175e4b8bbac"), ADResultCodeConverter::new),
    COLLO(fromString("9ea0d845-5096-44f6-9ce0-8ae801141eac"), COLLOResultCodeConverter::new),
    NCOLLO(fromString("615313b5-0647-4d61-b7b8-6b36265d8929"), NCOLLOResultCodeConverter::new),
    D(fromString("14d66587-8fbe-424f-a369-b1144f1684e3"), DResultCodeConverter::new),
    LEN(fromString("b0aeb4fc-df63-4e2f-af88-97e3f23e847f"), LENResultCodeConverter::new),
    LEA(fromString("3fa139cc-efe0-422b-93d6-190a5be50953"), LEAResultCodeConverter::new),
    LEP(fromString("cee54856-4450-4f28-a8a9-72b688726201"), LEPResultCodeConverter::new),
    DDD(fromString("ccfc452e-ebe4-4cd7-b8a0-4f90768447b4"), DDDResultCodeConverter::new),
    DDO(fromString("b2d06bbc-e90e-4df8-8851-6e4a70894828"), DDOResultCodeConverter::new),
    DDP(fromString("73fe22ca-76bd-4aba-bdea-6dfef8ee03a2"), DDPResultCodeConverter::new),
    NSP(fromString("49939c7c-750f-403e-9ce1-f82e3e568065"), NSPResultCodeConverter::new),
    TFOOUT(fromString("1e96d1a9-9618-4ddd-a925-ca6a0ef86018"), TFOOUTResultCodeConverter::new),
    SUMRCC(fromString("600edfc3-a584-4f9f-a52e-5bb8a99646c1"), SUMRCCResultCodeConverter::new),
    ADJOURNSJP(fromString("f7784e82-20b5-4d2c-b174-6fd57ebf8d7c"), ADJOURNSJPResultCodeConverter::new),
    RSJP(fromString("60ac9c98-eeec-4e48-823e-cd3f9fadd854"), RSJPResultCodeConverter::new),
    SUMRTO(fromString("3d2c05b3-fcd6-49c2-b5a9-52855be7f90a"), SUMRTOResultCodeConverter::new),
    SJPR(fromString("0149ab92-5466-11e8-9c2d-fa7ae01bbebc"), RSJPResultCodeConverter::new),
    D45(fromString("fcbf777d-1a73-47e7-ab9b-7c51091a022c"), D45ResultCodeConverter::new),
    DPR(fromString("b27b42bf-e20e-46ec-a6e3-5c2e8a076c20"), DPRResultCodeConverter::new),
    SETASIDE(fromString("af590f98-21cb-43e7-b992-2a9d444acb2b"), SETASIDEResultCodeConverter::new);


    private static final Logger LOGGER = getLogger(ResultCode.class);
    private UUID resultDefinitionId;
    private Supplier<ResultCodeConverter> converter;

    ResultCode(final UUID resultDefinitionId, final Supplier<ResultCodeConverter> converter) {
        this.resultDefinitionId = resultDefinitionId;
        this.converter = converter;
    }

    public static Optional<ResultCode> parse(String resultCode) {

        try {
            return of(ResultCode.valueOf(resultCode));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("No result code present for {}", resultCode);
        }
        return Optional.empty();
    }

    public UUID getResultDefinitionId() {
        return resultDefinitionId;
    }

    public JsonArrayBuilder createPrompts(final JsonObject result, final OffenceDataSupplier offenceDataSupplier) {
        return converter.get().createPrompts(result, offenceDataSupplier);
    }
}
