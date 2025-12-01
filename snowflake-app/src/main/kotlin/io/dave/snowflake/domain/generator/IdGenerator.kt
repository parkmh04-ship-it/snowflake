package io.dave.snowflake.domain.generator

fun interface IdGenerator {
    suspend fun nextId(): Long
}
