package com.agrodoc.app.domain.repository

import com.agrodoc.app.domain.engine.AssistantAnswer

interface AdvisorEngine {
    suspend fun consult(
        question: String,
        retrievedContext: AssistantAnswer,
        langCode: String,
        activeCropId: String? = null,
    ): String
}
