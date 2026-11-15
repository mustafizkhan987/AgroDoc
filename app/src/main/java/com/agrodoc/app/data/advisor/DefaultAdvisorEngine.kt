package com.agrodoc.app.data.advisor

import com.agrodoc.app.domain.engine.AssistantAnswer
import com.agrodoc.app.domain.repository.AdvisorEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAdvisorEngine @Inject constructor() : AdvisorEngine {
    override suspend fun consult(
        question: String,
        retrievedContext: AssistantAnswer,
        langCode: String,
        activeCropId: String?,
    ): String {
        return retrievedContext.answer
    }
}
