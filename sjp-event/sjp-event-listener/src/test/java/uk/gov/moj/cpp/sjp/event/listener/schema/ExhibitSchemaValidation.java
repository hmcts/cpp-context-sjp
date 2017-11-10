package uk.gov.moj.cpp.sjp.event.listener.schema;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

public class ExhibitSchemaValidation {
    
    @Test
    public void shouldFailValidation_WhenExhibitNameIsGreaterThanPermitted() throws FileNotFoundException{
        JSONObject event = new JSONObject();
        event.put("caseId", "caseIdValue");
        event.put("id", "idValue");
        event.put("exhibitName", "This exhibit name exceeds the maximum permitted length. This exhibit name exceeds the maximum permitted length. "
                + "This exhibit name exceeds the maximum permitted length. This exhibit name exceeds the maximum permitted length.");
        event.put("source", "CPP");
        event.put("urn", "urnValue");
        
        Schema schema = loadSchema();
        
        try {
            schema.validate(event);
        } catch (ValidationException e){
            assertThat(e.getMessage(), containsString("exhibitName"));
            return;
        }
        
        fail("Should have failed validation.");
    }

    private Schema loadSchema() throws FileNotFoundException {
        JSONObject rawSchema = new JSONObject(new JSONTokener(new FileReader("src/raml/json/schema/structure.events.exhibit-added.json")));
        Schema schema = SchemaLoader.load(rawSchema);
        return schema;
    }
    
    @Test
    public void shouldFailValidation_WhenStorageReferenceIsGreaterThanPermitted() throws FileNotFoundException{
        JSONObject event = new JSONObject();
        event.put("caseId", "caseIdValue");
        event.put("id", "idValue");
        event.put("exhibitName", "Exibit name");
        event.put("storageReference", "This text exceeds the maximum permitted length. This text exceeds the maximum permitted length.");
        event.put("source", "CPP");
        event.put("urn", "urnValue");
        
        Schema schema = loadSchema();
        
        try {
            schema.validate(event);
        } catch (ValidationException e){
            assertThat(e.getMessage(), containsString("storageReference"));
            return;
        }
        
        fail("Should have failed validation.");
    }
    
}