package io.dave.snowflake.domain.component

import io.dave.snowflake.domain.generator.Base62Encoder
import io.dave.snowflake.domain.generator.PooledIdGenerator
import io.dave.snowflake.domain.model.ShortUrl
import io.dave.snowflake.domain.port.outbound.UrlPort
import kotlin.random.Random

/** 고유하고 짧은 URL을 생성하는 도메인 서비스. 생성된 ID를 Base62로 인코딩하며, 중복을 방지하기 위해 URL 저장소에 존재 여부를 확인합니다. */
class ShortUrlGenerator(
        private val idGenerator: PooledIdGenerator,
        private val urlPort: UrlPort,
        private val random: Random = Random(System.currentTimeMillis())
) {
    private val alphabet = Base62Encoder.ALPHABET
    private val alphabetSize = alphabet.length

    /**
     * 고유한 ShortUrl을 생성하여 반환합니다. 생성된 Snowflake ID를 Base62로 인코딩한 후, 해당 ShortUrl이 이미 존재하는지 확인합니다. 중복될
     * 경우, 뒤에 임의의 알파벳을 추가하여 충돌을 회피합니다.
     *
     * @return 고유하게 생성된 ShortUrl 객체.
     */
    suspend fun generate(): ShortUrl {
        var shortUrlString: String
        var isUnique = false

        do {
            val id = idGenerator.nextId()
            shortUrlString = Base62Encoder.encode(id)

            // 중복될 경우 뒤에 임의의 문자를 붙여 재시도 (최대 3회)
            repeat(3) {
                if (!urlPort.existsByShortUrl(ShortUrl(shortUrlString))) {
                    isUnique = true
                    return@repeat
                }
                shortUrlString += alphabet[random.nextInt(alphabetSize)]
            }
            // 3회 재시도 후에도 중복이면 ID를 새로 받아서 재시도
        } while (!isUnique)

        return ShortUrl(shortUrlString)
    }
}
