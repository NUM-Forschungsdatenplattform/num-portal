package org.highmed.numportal.service.ehrbase;

import org.highmed.numportal.service.exception.SystemException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nedap.archie.rm.composition.Composition;
import lombok.RequiredArgsConstructor;
import org.ehrbase.openehr.sdk.client.templateprovider.ClientTemplateProvider;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatFormat;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatJasonProvider;
import org.ehrbase.openehr.sdk.serialisation.flatencoding.FlatJson;
import org.ehrbase.openehr.sdk.util.exception.SdkException;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.templateprovider.CachedTemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_PARSE_RESULTS;
import static org.highmed.numportal.domain.templates.ExceptionsTemplate.CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID;

@Component
@RequiredArgsConstructor
public class CompositionFlattener {

  private static final String FLAT_JSON_CACHE = "flatJsonCache";
  private static final String OPERATIONAL_TEMPLATE_CACHE = "operationalTemplateCache";
  private static final String WEB_TEMPLATE_CACHE = "webTemplateCache";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ClientTemplateProvider clientTemplateProvider;
  private CachedTemplateProvider cachedTemplateProvider;
  private Cache<String, FlatJson> flatJsonCache;

  public Map<String, Object> flatten(Composition composition) {

    validateComposition(composition);

    try {
      String templateId = composition.getArchetypeDetails().getTemplateId().getValue();
      return objectMapper.readValue(getFlatJson(templateId).marshal(composition), Map.class);
    } catch (JsonProcessingException e) {
      throw new SystemException(CompositionFlattener.class, CANNOT_PARSE_RESULTS,
          String.format(CANNOT_PARSE_RESULTS, e.getMessage()));
    } catch (SdkException e) {
      throw new SystemException(SdkException.class, e.getMessage());
    }
  }

  private FlatJson getFlatJson(String templateId) {
    Optional<FlatJson> cachedFlatJson = Optional.ofNullable(flatJsonCache.get(templateId));

    if (cachedFlatJson.isEmpty()) {
      FlatJson flatJson =
          (FlatJson) new FlatJasonProvider(cachedTemplateProvider)
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
      throw new SystemException(CompositionFlattener.class, CANNOT_PARSE_RESULTS_COMPOSITION_MISSING_TEMPLATE_ID);
    }
  }

  @PostConstruct
  public void initializeTemplateCache() {
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
        new CachedTemplateProvider(clientTemplateProvider, templateCache, introspectCache);

    MutableConfiguration<String, FlatJson> flatJsonCacheConfig =
        new MutableConfiguration<String, FlatJson>()
            .setTypes(String.class, FlatJson.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_DAY))
            .setStoreByValue(false);

    flatJsonCache = cacheManager.createCache(FLAT_JSON_CACHE, flatJsonCacheConfig);
  }

  @PreDestroy
  public void clearCaches() {
    CachingProvider provider = Caching.getCachingProvider();
    CacheManager cacheManager = provider.getCacheManager();
    cacheManager.destroyCache(FLAT_JSON_CACHE);
    cacheManager.destroyCache(WEB_TEMPLATE_CACHE);
    cacheManager.destroyCache(OPERATIONAL_TEMPLATE_CACHE);
  }
}
