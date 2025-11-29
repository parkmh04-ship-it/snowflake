package io.dave.shortener.domain.generator

object Base62Encoder {
    // 예측 불가능성을 높이기 위해 문자열의 순서를 섞었습니다.
    val ALPHABET = "wFjR2pTqYx4Ua7sKv9dHnC0mZl1bGeOi3u6I8E5rBAcWJdXPQfyMLzNtVkGS"
    private val BASE = ALPHABET.length.toLong()

    fun encode(input: Long): String {
        var n = input
        val result = StringBuilder()

        if (n == 0L) {
            return "0"
        }

        while (n > 0) {
            result.insert(0, ALPHABET[(n % BASE).toInt()])
            n /= BASE
        }
        return result.toString()
    }
}