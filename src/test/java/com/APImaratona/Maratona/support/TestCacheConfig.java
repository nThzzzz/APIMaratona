package com.APImaratona.Maratona.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * MaratonaApplication tem @EnableCaching na classe principal, que o @WebMvcTest usa como
 * configuracao base. Isso ativa a infraestrutura de cache mesmo no slice de teste, mas o
 * CacheManager real (Redis) so existe em DatabasesConfig, fora do slice. Este bean de teste
 * evita a falha "No qualifying bean of type CacheManager" ao subir o contexto.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
