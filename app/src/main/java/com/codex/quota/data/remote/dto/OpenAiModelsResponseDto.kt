package com.codex.quota.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenAiModelsResponseDto(
    val `object`: String? = null,
    val data: List<ModelDto> = emptyList()
)

@Serializable
data class ModelDto(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null
)
