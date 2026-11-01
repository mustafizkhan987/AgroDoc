package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo

data class AssistantAnswer(
    val query: String,
    val answer: String,
    val matchedCrop: CropInfo? = null,
    val matchedDisease: DiseaseInfo? = null,
    val source: String = "",
    val confidence: Float = 0.0f,
    val suggestedFollowUps: List<String> = emptyList(),
)

object KnowledgeRetriever {

    fun answerQuery(
        query: String,
        crops: List<CropInfo>,
        diseases: List<DiseaseInfo>,
        langCode: String = "en",
    ): AssistantAnswer {
        val cleanQuery = query.trim().lowercase()

        if (cleanQuery.isEmpty()) {
            return buildFallbackAnswer(query, langCode)
        }

        // 1. Identify Crop Entity
        val matchedCrop = crops.find { crop ->
            cleanQuery.contains(crop.id.replace("_", " ")) ||
                cleanQuery.contains(crop.nameEn.lowercase()) ||
                cleanQuery.contains(crop.nameKn.lowercase()) ||
                cleanQuery.contains(crop.nameHi.lowercase()) ||
                cleanQuery.contains(crop.nameMr.lowercase()) ||
                crop.localNamesKn.any { cleanQuery.contains(it.lowercase()) }
        }

        // 2. Identify Disease Entity
        val matchedDisease = diseases.find { disease ->
            cleanQuery.contains(disease.nameEn.lowercase()) ||
                cleanQuery.contains(disease.nameKn.lowercase()) ||
                cleanQuery.contains(disease.nameHi.lowercase()) ||
                cleanQuery.contains(disease.nameMr.lowercase()) ||
                (matchedCrop != null && disease.cropKey == matchedCrop.id &&
                    (cleanQuery.contains("disease") || cleanQuery.contains("blight") ||
                        cleanQuery.contains("rot") || cleanQuery.contains("ರೋಗ") ||
                        cleanQuery.contains("ಕಾಯಿಲೆ") || cleanQuery.contains("रोग")))
        }

        // 3. Determine Intent and construct response
        if (matchedDisease != null) {
            val responseText = buildDiseaseAnswer(matchedDisease, matchedCrop, cleanQuery, langCode)
            return AssistantAnswer(
                query = query,
                answer = responseText,
                matchedCrop = matchedCrop,
                matchedDisease = matchedDisease,
                source = matchedDisease.source,
                confidence = 0.95f,
                suggestedFollowUps = getSuggestedDiseaseQuestions(matchedDisease, langCode),
            )
        }

        if (matchedCrop != null) {
            val responseText = buildCropAnswer(matchedCrop, cleanQuery, langCode)
            return AssistantAnswer(
                query = query,
                answer = responseText,
                matchedCrop = matchedCrop,
                matchedDisease = null,
                source = matchedCrop.source,
                confidence = 0.90f,
                suggestedFollowUps = getSuggestedCropQuestions(matchedCrop, langCode),
            )
        }

        // 4. Try fuzzy match across symptoms in diseases
        val symptomMatch = diseases.find { d ->
            val symptoms = when (langCode.lowercase()) {
                "kn" -> d.symptomsKn
                "hi" -> d.symptomsHi
                "mr" -> d.symptomsMr
                else -> d.symptomsEn
            }.lowercase()
            cleanQuery.split(" ").any { word -> word.length > 4 && symptoms.contains(word) }
        }

        if (symptomMatch != null) {
            val responseText = buildDiseaseAnswer(symptomMatch, null, cleanQuery, langCode)
            return AssistantAnswer(
                query = query,
                answer = responseText,
                matchedCrop = null,
                matchedDisease = symptomMatch,
                source = symptomMatch.source,
                confidence = 0.70f,
                suggestedFollowUps = getSuggestedDiseaseQuestions(symptomMatch, langCode),
            )
        }

        return buildFallbackAnswer(query, langCode)
    }

    private fun buildDiseaseAnswer(
        disease: DiseaseInfo,
        crop: CropInfo?,
        query: String,
        langCode: String,
    ): String {
        val dName = when (langCode.lowercase()) {
            "kn" -> disease.nameKn.ifEmpty { disease.nameEn }
            "hi" -> disease.nameHi.ifEmpty { disease.nameEn }
            "mr" -> disease.nameMr.ifEmpty { disease.nameEn }
            else -> disease.nameEn
        }
        val cName = crop?.let {
            when (langCode.lowercase()) {
                "kn" -> it.nameKn
                "hi" -> it.nameHi
                "mr" -> it.nameMr
                else -> it.nameEn
            }
        } ?: disease.cropKey

        val symptoms = when (langCode.lowercase()) {
            "kn" -> disease.symptomsKn.ifEmpty { disease.symptomsEn }
            "hi" -> disease.symptomsHi.ifEmpty { disease.symptomsEn }
            "mr" -> disease.symptomsMr.ifEmpty { disease.symptomsEn }
            else -> disease.symptomsEn
        }
        val prevention = when (langCode.lowercase()) {
            "kn" -> disease.preventionKn.ifEmpty { disease.preventionEn }
            "hi" -> disease.preventionHi.ifEmpty { disease.preventionEn }
            "mr" -> disease.preventionMr.ifEmpty { disease.preventionEn }
            else -> disease.preventionEn
        }
        val treatment = when (langCode.lowercase()) {
            "kn" -> disease.treatmentKn.ifEmpty { disease.treatmentEn }
            "hi" -> disease.treatmentHi.ifEmpty { disease.treatmentEn }
            "mr" -> disease.treatmentMr.ifEmpty { disease.treatmentEn }
            else -> disease.treatmentEn
        }
        val conditions = when (langCode.lowercase()) {
            "kn" -> disease.favourableConditions.descriptionKn.ifEmpty { disease.favourableConditions.descriptionEn }
            "hi" -> disease.favourableConditions.descriptionHi.ifEmpty { disease.favourableConditions.descriptionEn }
            "mr" -> disease.favourableConditions.descriptionMr.ifEmpty { disease.favourableConditions.descriptionEn }
            else -> disease.favourableConditions.descriptionEn
        }

        val asksTreatment = query.contains("treatment") || query.contains("control") ||
            query.contains("cure") || query.contains("ಔಷಧ") || query.contains("ನಿಯಂತ್ರಣ") ||
            query.contains("दवा") || query.contains("उपाय")

        val asksSymptoms = query.contains("symptom") || query.contains("identify") ||
            query.contains("ಲಕ್ಷಣ") || query.contains("ಗುರುತು") || query.contains("लक्षण")

        return when (langCode.lowercase()) {
            "kn" -> when {
                asksTreatment -> "$cName ಬೆಳೆಯಲ್ಲಿ $dName ರೋಗ ನಿಯಂತ್ರಣ:\n\n• ಚಿಕಿತ್ಸೆ: $treatment\n\n• ತಡೆಗಟ್ಟುವಿಕೆ: $prevention"
                asksSymptoms -> "$cName ಬೆಳೆಯಲ್ಲಿ $dName ರೋಗದ ಲಕ್ಷಣಗಳು:\n\n$symptoms\n\nಅನುಕೂಲಕರ ವಾತಾವರಣ: $conditions"
                else -> "$cName ಬೆಳೆಯಲ್ಲಿ $dName:\n\nಲಕ್ಷಣಗಳು: $symptoms\n\nಚಿಕಿತ್ಸೆ: $treatment\n\nಪೂರಕ ವಾತಾವರಣ: $conditions"
            }
            "hi" -> when {
                asksTreatment -> "$cName में $dName रोग का नियंत्रण एवं उपचार:\n\n• उपचार: $treatment\n\n• रोकथाम: $prevention"
                asksSymptoms -> "$cName में $dName के लक्षण:\n\n$symptoms\n\nअनुकूल मौसम: $conditions"
                else -> "$cName में $dName:\n\nलक्षण: $symptoms\n\nउपचार: $treatment\n\nअनुकूल वातावरण: $conditions"
            }
            "mr" -> when {
                asksTreatment -> "$cName पिकातील $dName रोगाचे नियंत्रण:\n\n• उपाय: $treatment\n\n• प्रतिबंध: $prevention"
                asksSymptoms -> "$cName पिकातील $dName रोगाची लक्षणे:\n\n$symptoms\n\nपोषक वातावरण: $conditions"
                else -> "$cName मधील $dName:\n\nलक्षणे: $symptoms\n\nउपाय: $treatment\n\nपोषक वातावरण: $conditions"
            }
            else -> when {
                asksTreatment -> "Treatment & control for $dName in $cName:\n\n• Treatment: $treatment\n\n• Prevention: $prevention"
                asksSymptoms -> "Symptoms of $dName in $cName:\n\n$symptoms\n\nFavourable conditions: $conditions"
                else -> "$dName in $cName:\n\n• Symptoms: $symptoms\n\n• Management: $treatment\n\n• Conducive Weather: $conditions"
            }
        }
    }

    private fun buildCropAnswer(
        crop: CropInfo,
        query: String,
        langCode: String,
    ): String {
        val cName = when (langCode.lowercase()) {
            "kn" -> crop.nameKn
            "hi" -> crop.nameHi
            "mr" -> crop.nameMr
            else -> crop.nameEn
        }

        val sowingWindows = crop.sowingWindows.joinToString("; ") { "${it.season}: ${it.sowingMonths}" }
        val soils = crop.soilTypes.joinToString(", ")
        val tempRange = "${crop.tempOptMinC ?: crop.tempMinC ?: 18}–${crop.tempOptMaxC ?: crop.tempMaxC ?: 32}°C"

        val asksSowing = query.contains("sow") || query.contains("season") || query.contains("time") ||
            query.contains("ಬಿತ್ತನೆ") || query.contains("ಹಂಗಾಮು") || query.contains("ಬುवाई") || query.contains("पेरणी")

        val asksSoil = query.contains("soil") || query.contains("climate") || query.contains("weather") ||
            query.contains("ಮಣ್ಣು") || query.contains("ಹವಾಮಾನ") || query.contains("मिट्टी") || query.contains("जमीन")

        return when (langCode.lowercase()) {
            "kn" -> when {
                asksSowing -> "$cName ಬೆಳೆಗೆ ಸೂಕ್ತ ಬಿತ್ತನೆ ಅವಧಿ: $sowingWindows. ಬೆಳೆಯ ಅವಧಿ: ${crop.durationDaysMin ?: 90} ರಿಂದ ${crop.durationDaysMax ?: 120} ದಿನಗಳು."
                asksSoil -> "$cName ಬೆಳೆಗೆ ಸೂಕ್ತ ಪರಿಸ್ಥಿತಿ:\n\n• ಮಣ್ಣು: $soils\n• ತಾಪಮಾನ: $tempRange\n• ವಾರ್ಷಿಕ ಮಳೆ: ${crop.annualRainfallMm ?: 600} ಮಿ.ಮೀ."
                else -> "$cName ಬೆಳೆಯ ಪರಿಚಯ:\n\n• ಸೂಕ್ತ ಬಿತ್ತನೆ: $sowingWindows\n• ಮಣ್ಣು: $soils\n• ತಾಪಮಾನ: $tempRange\n• ಸಾಮಾನ್ಯ ರೋಗಗಳು: ${crop.commonDiseaseIds.size} ರೋಗಗಳನ್ನು ಪಟ್ಟಿ ಮಾಡಲಾಗಿದೆ."
            }
            "hi" -> when {
                asksSowing -> "$cName की बुवाई का समय: $sowingWindows। फसल अवधि: ${crop.durationDaysMin ?: 90}-${crop.durationDaysMax ?: 120} दिन।"
                asksSoil -> "$cName के लिए उपयुक्त मिट्टी एवं जलवायु:\n\n• मिट्टी: $soils\n• तापमान: $tempRange\n• वार्षिक वर्षा: ${crop.annualRainfallMm ?: 600} मिमी।"
                else -> "$cName विवरण:\n\n• बुवाई: $sowingWindows\n• मिट्टी: $soils\n• तापमान: $tempRange"
            }
            "mr" -> when {
                asksSowing -> "$cName पेरणीची वेळ: $sowingWindows. पिकाचा कालावधी: ${crop.durationDaysMin ?: 90} ते ${crop.durationDaysMax ?: 120} दिवस."
                asksSoil -> "$cName साठी जमीन व हवामान:\n\n• जमीन: $soils\n• तापमान: $tempRange\n• पाऊस: ${crop.annualRainfallMm ?: 600} मिमी."
                else -> "$cName माहिती:\n\n• पेरणी: $sowingWindows\n• जमीन: $soils\n• तापमान: $tempRange"
            }
            else -> when {
                asksSowing -> "Sowing windows for $cName: $sowingWindows. Crop duration: ${crop.durationDaysMin ?: 90} to ${crop.durationDaysMax ?: 120} days."
                asksSoil -> "Suitable conditions for $cName:\n\n• Soils: $soils\n• Temperature: $tempRange\n• Annual Rainfall: ${crop.annualRainfallMm ?: 600} mm."
                else -> "$cName Overview:\n\n• Sowing Windows: $sowingWindows\n• Soils: $soils\n• Ideal Temperature: $tempRange"
            }
        }
    }

    private fun buildFallbackAnswer(query: String, langCode: String): AssistantAnswer {
        val answer = when (langCode.lowercase()) {
            "kn" -> "ನನ್ನಲ್ಲಿರುವ ಪರಿಶೀಲಿಸಿದ ಆಫ್‌ಲೈನ್ ಮಾಹಿತಿಯಲ್ಲಿ ಇದಕ್ಕೆ ಉತ್ತರ ದೊರೆತಿಲ್ಲ. ದಯವಿಟ್ಟು ನಿಮ್ಮ ಸಮೀಪದ ರೈತ ಸಂಪರ್ಕ ಕೇಂದ್ರ ಅಥವಾ ಕೃಷಿ ಅಧಿಕಾರಿಯನ್ನು ಸಂಪರ್ಕಿಸಿ."
            "hi" -> "सत्यापित ऑफ़लाइन डेटाबेस में इसका उत्तर उपलब्ध नहीं है। कृपया अपने नजदीकी कृषि अधिकारी या कृषि विज्ञान केंद्र से संपर्क करें।"
            "mr" -> "माझ्याकडे असलेल्या सत्यापित ऑफलाइन माहितीमध्ये याचे उत्तर नाही. कृपया आपल्या जवळच्या कृषी अधिकाऱ्यांशी संपर्क साधावा."
            else -> "I am not sure based on the verified offline database. Please consult your local Agricultural Officer or Raitha Samparka Kendra."
        }
        return AssistantAnswer(
            query = query,
            answer = answer,
            confidence = 0.0f,
            suggestedFollowUps = listOf(
                "How to grow ragi?",
                "What is blast disease in paddy?",
                "Tomato early blight treatment",
                "Fertilizer for maize",
            ),
        )
    }

    private fun getSuggestedCropQuestions(crop: CropInfo, langCode: String): List<String> = when (langCode.lowercase()) {
        "kn" -> listOf(
            "${crop.nameKn} ಬೆಳೆಯ ಬಿತ್ತನೆ ಸಮಯ ಯಾವುದು?",
            "${crop.nameKn} ಬೆಳೆಗೆ ರಸಗೊಬ್ಬರ ಪ್ರಮಾಣ ಎಷ್ಟು?",
            "${crop.nameKn} ಬೆಳೆಯ ಪ್ರಮುಖ ರೋಗಗಳು ಯಾವುವು?",
        )
        "hi" -> listOf(
            "${crop.nameHi} की बुवाई कब करें?",
            "${crop.nameHi} के लिए खाद की मात्रा?",
            "${crop.nameHi} के मुख्य रोग कौन से हैं?",
        )
        "mr" -> listOf(
            "${crop.nameMr} पेरणीची योग्य वेळ कोणती?",
            "${crop.nameMr} खत व्यवस्थापन कसे करावे?",
            "${crop.nameMr} मुख्य रोग कोणते आहेत?",
        )
        else -> listOf(
            "What is the best sowing time for ${crop.nameEn}?",
            "What is the fertilizer schedule for ${crop.nameEn}?",
            "What are the major diseases affecting ${crop.nameEn}?",
        )
    }

    private fun getSuggestedDiseaseQuestions(disease: DiseaseInfo, langCode: String): List<String> = when (langCode.lowercase()) {
        "kn" -> listOf(
            "${disease.nameKn} ರೋಗಕ್ಕೆ ಔಷಧೋಪಚಾರವೇನು?",
            "ಈ ರೋಗ ಹರಡಲು ಯಾವ ಹವಾಮಾನ ಪೂರಕ?",
            "ಮುಂದಿನ ಬೆಳೆಗೆ ಇದನ್ನು ಹೇಗೆ ತಡೆಯುವುದು?",
        )
        "hi" -> listOf(
            "${disease.nameHi} की रोकथाम के उपाय?",
            "यह रोग किस मौसम में फैलता है?",
            "इसे दोबारा होने से कैसे रोकें?",
        )
        "mr" -> listOf(
            "${disease.nameMr} रोगावर काय उपाय करावा?",
            "हा रोग कोणत्या हवामानात वाढतो?",
            "पुढील पिकात हा रोग कसा टाळावा?",
        )
        else -> listOf(
            "What is the recommended treatment for ${disease.nameEn}?",
            "What weather conditions favour ${disease.nameEn}?",
            "How to prevent this disease in next season?",
        )
    }
}

