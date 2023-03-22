package de.vitagroup.num.serialisation;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nedap.archie.json.DurationDeserializer;
import com.nedap.archie.json.JacksonUtil;
import com.nedap.archie.rm.RMObject;
import org.ehrbase.serialisation.exception.UnmarshalException;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;

import java.io.IOException;
import java.time.temporal.TemporalAmount;

public class NumCanonicalJson extends CanonicalJson {

    private final Module module;

    public NumCanonicalJson() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(TemporalAmount.class, new DurationDeserializer());
        this.module = simpleModule;
    }


    @Override
    public <T extends RMObject> T unmarshal(String value, Class<T> clazz) {
        try {
            return JacksonUtil.getObjectMapper()
                    .registerModule(this.module)
                    .readValue(value, clazz);
        } catch (IOException ioe) {
            throw new UnmarshalException(ioe.getMessage(), ioe);
        }
    }
}
