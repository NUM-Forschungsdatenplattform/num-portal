package de.vitagroup.num.service.ehrbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import de.vitagroup.num.web.exception.SystemException;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import lombok.RequiredArgsConstructor;
import org.ehrbase.serialisation.flatencoding.FlatFormat;
import org.ehrbase.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.serialisation.flatencoding.FlatJson;
import org.ehrbase.util.exception.SdkException;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.templateprovider.CachedTemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompositionFlattener {

  private final ObjectMapper mapper = new ObjectMapper();

  private CachedTemplateProvider cachedTemplateProvider;
  private final RemoteEhrBaseTemplateProvider remoteEhrBaseTemplateProvider;

  private Cache<String, FlatJson> flatJsonCache;

  private static final String FLAT_JSON_CACHE = "flatJsonCache";
  private static final String OPERATIONAL_TEMPLATE_CACHE = "operationalTemplateCache";
  private static final String WEB_TEMPLATE_CACHE = "webTemplateCache";

  public Map<String, String> flatten(Composition composition) {

    validateComposition(composition);

    try {
      String templateId = composition.getArchetypeDetails().getTemplateId().getValue();
      return mapper.readValue(getFlatJson(templateId).marshal(composition), Map.class);
    } catch (JsonProcessingException e) {
      throw new SystemException("Cannot parse results", e);
    } catch (SdkException e) {
      throw new SystemException(e.getMessage());
    }
  }

  private FlatJson getFlatJson(String templateId) {
    Optional<FlatJson> cachedFlatJson = Optional.ofNullable(flatJsonCache.get(templateId));

    if (cachedFlatJson.isEmpty()) {
      FlatJson flatJson =
          new FlatJasonProvider(cachedTemplateProvider)
              .buildFlatJson(FlatFormat.SIM_SDT, templateId);

      flatJsonCache.put(templateId, flatJson);
      return flatJson;
    }

    return cachedFlatJson.get();
  }

  private void validateComposition(Composition composition) {
    if (composition.getArchetypeDetails() == null
        || composition.getArchetypeDetails().getTemplateId() == null
        || composition.getArchetypeDetails().getTemplateId().getValue() == null) {
      throw new SystemException(
          "Cannot parse results, composition missing template id");
    }
  }

  @PostConstruct
  private void initializeTemplateCache() {
    CachingProvider provider = Caching.getCachingProvider();
    CacheManager cacheManager = provider.getCacheManager();

    MutableConfiguration<String, OPERATIONALTEMPLATE> templateCacheConfig =
        new MutableConfiguration<String, OPERATIONALTEMPLATE>()
            .setTypes(String.class, OPERATIONALTEMPLATE.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_DAY))
            .setStoreByValue(false);

    Cache<String, OPERATIONALTEMPLATE> templateCache =
        cacheManager.createCache(OPERATIONAL_TEMPLATE_CACHE, templateCacheConfig);

    MutableConfiguration<String, WebTemplate> introspectCacheConfig =
        new MutableConfiguration<String, WebTemplate>()
            .setTypes(String.class, WebTemplate.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_DAY))
            .setStoreByValue(false);

    Cache<String, WebTemplate> introspectCache =
        cacheManager.createCache(WEB_TEMPLATE_CACHE, introspectCacheConfig);

    cachedTemplateProvider =
        new CachedTemplateProvider(remoteEhrBaseTemplateProvider, templateCache, introspectCache);

    MutableConfiguration<String, FlatJson> flatJsonCacheConfig =
        new MutableConfiguration<String, FlatJson>()
            .setTypes(String.class, FlatJson.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_DAY))
            .setStoreByValue(false);

    flatJsonCache = cacheManager.createCache(FLAT_JSON_CACHE, flatJsonCacheConfig);
  }
}
