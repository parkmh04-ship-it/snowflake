package io.dave.snowflake.config

import io.dave.snowflake.domain.component.ShortUrlGenerator
import io.dave.snowflake.domain.generator.PooledIdGenerator
import io.dave.snowflake.domain.port.outbound.UrlPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainComponentConfig {

    @Bean
    fun shortUrlGenerator(idGenerator: PooledIdGenerator, urlPort: UrlPort): ShortUrlGenerator {
        return ShortUrlGenerator(idGenerator, urlPort)
    }
}
