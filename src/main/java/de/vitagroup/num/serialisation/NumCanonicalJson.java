package de.vitagroup.num.serialisation;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nedap.archie.json.DurationDeserializer;
import com.nedap.archie.rm.RMObject;
import java.io.IOException;
import java.time.temporal.TemporalAmount;
import org.ehrbase.serialisation.exception.UnmarshalException;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.serialisation.jsonencoding.JacksonUtil;

public class NumCanonicalJson extends CanonicalJson {

  private final Module module;

  public NumCanonicalJson() {
    SimpleModule sm = new SimpleModule();
    sm.addDeserializer(TemporalAmount.class, new DurationDeserializer());
    this.module = sm;
  }

  @Override
  public <T extends RMObject> T unmarshal(String value, Class<T> clazz) {
    try {
      return (T) JacksonUtil.getObjectMapper()
          .registerModule(this.module)
          .readValue(value, clazz);
    } catch (IOException var4) {
      throw new UnmarshalException(var4.getMessage(), var4);
    }
  }
}
