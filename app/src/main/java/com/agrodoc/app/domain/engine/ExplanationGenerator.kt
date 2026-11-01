package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.DiseaseInfo
import com.agrodoc.app.domain.model.WeatherData

object ExplanationGenerator {

    data class DiagnosticExplanation(
        val title: String,
        val summary: String,
        val whatCausedIt: String,
        val weatherInfluence: String,
        val spreadMechanism: String,
        val immediateSteps: List<String>,
        val longTermPrevention: List<String>,
        val officerAdvisory: String,
    )

    fun generateExplanation(
        crop: CropInfo?,
        disease: DiseaseInfo,
        weather: WeatherData?,
        langCode: String = "en",
        confidence: Float = 0.85f,
    ): DiagnosticExplanation {
        val diseaseName = when (langCode.lowercase()) {
            "kn" -> disease.nameKn.ifEmpty { disease.nameEn }
            "hi" -> disease.nameHi.ifEmpty { disease.nameEn }
            "mr" -> disease.nameMr.ifEmpty { disease.nameEn }
            else -> disease.nameEn
        }

        val cropName = when (langCode.lowercase()) {
            "kn" -> crop?.nameKn ?: disease.cropKey
            "hi" -> crop?.nameHi ?: disease.cropKey
            "mr" -> crop?.nameMr ?: disease.cropKey
            else -> crop?.nameEn ?: disease.cropKey
        }

        val symptoms = when (langCode.lowercase()) {
            "kn" -> disease.symptomsKn.ifEmpty { disease.symptomsEn }
            "hi" -> disease.symptomsHi.ifEmpty { disease.symptomsEn }
            "mr" -> disease.symptomsMr.ifEmpty { disease.symptomsEn }
            else -> disease.symptomsEn
        }

        val spread = when (langCode.lowercase()) {
            "kn" -> disease.spreadKn.ifEmpty { disease.spreadEn }
            "hi" -> disease.spreadHi.ifEmpty { disease.spreadEn }
            "mr" -> disease.spreadMr.ifEmpty { disease.spreadEn }
            else -> disease.spreadEn
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

        // Weather signal evaluation (Secondary signal only)
        val risk = DiseaseRiskEngine.evaluateRisk(disease, weather, langCode)
        val weatherInfluence = when (langCode.lowercase()) {
            "kn" -> when (risk.riskLevel) {
                com.agrodoc.app.domain.model.RiskLevel.HIGH ->
                    "ಪ್ರಸ್ತುತ ಹವಾಮಾನವು ಈ ರೋಗ ಹರಡಲು ಬಹಳ ಪೂರಕವಾಗಿದೆ. ಮುಂಜಾಗ್ರತಾ ಕ್ರಮಗಳನ್ನು ತಕ್ಷಣವೇ ಕೈಗೊಳ್ಳಿ."
                com.agrodoc.app.domain.model.RiskLevel.MODERATE ->
                    "ಹವಾಮಾನವು ರೋಗ ಬೆಳವಣಿಗೆಗೆ ಮಧ್ಯಮ ಅನುಕೂಲಕರವಾಗಿದೆ."
                com.agrodoc.app.domain.model.RiskLevel.LOW ->
                    if (confidence < 0.60f) {
                        "ಪ್ರಸ್ತುತ ಹವಾಮಾನವು ಈ ರೋಗಕ್ಕೆ ಪೂರಕವಾಗಿಲ್ಲ. ಕಡಿಮೆ ನಿಖರತೆಯಿರುವುದರಿಂದ ಸರಿಯಾದ ಬೆಳಕಿನಲ್ಲಿ ಮತ್ತೆ ಸ್ಕ್ಯಾನ್ ಮಾಡಲು ಪರಿಗಣಿಸಿ."
                    } else {
                        "ಹವಾಮಾನವು ಸದ್ಯಕ್ಕೆ ರೋಗ ಹರಡುವಿಕೆಗೆ ಅನುಕೂಲಕರವಾಗಿಲ್ಲ."
                    }
            }
            "hi" -> when (risk.riskLevel) {
                com.agrodoc.app.domain.model.RiskLevel.HIGH ->
                    "वर्तमान मौसम इस बीमारी के तेजी से फैलने के अत्यधिक अनुकूल है। तुरंत रोकथाम के उपाय करें।"
                com.agrodoc.app.domain.model.RiskLevel.MODERATE ->
                    "मौसम बीमारी के प्रसार के लिए मध्यम रूप से अनुकूल है।"
                com.agrodoc.app.domain.model.RiskLevel.LOW ->
                    if (confidence < 0.60f) {
                        "वर्तमान मौसम रोग के अनुकूल नहीं है। विश्वास स्तर कम है, कृपया अच्छी रोशनी में दोबारा स्कैन करें।"
                    } else {
                        "मौसम फिलहाल इस बीमारी के प्रसार के अनुकूल नहीं है।"
                    }
            }
            "mr" -> when (risk.riskLevel) {
                com.agrodoc.app.domain.model.RiskLevel.HIGH ->
                    "सध्याचे हवामान हा रोग वेगाने पसरण्यासाठी अत्यंत पोषक आहे. तातडीने प्रतिबंधात्मक उपाय करा."
                com.agrodoc.app.domain.model.RiskLevel.MODERATE ->
                    "हवामान रोगाच्या वाढीसाठी मध्यम अनुकूल आहे."
                com.agrodoc.app.domain.model.RiskLevel.LOW ->
                    if (confidence < 0.60f) {
                        "सध्याचे हवामान रोगासाठी अनुकूल नाही. कृपया चांगल्या प्रकाशात पुन्हा स्कॅन करण्याचा विचार करा."
                    } else {
                        "सध्या हवामान रोगाच्या प्रसारास पोषक नाही."
                    }
            }
            else -> when (risk.riskLevel) {
                com.agrodoc.app.domain.model.RiskLevel.HIGH ->
                    "Current weather conditions strongly favour this disease. Immediate preventive action is advised."
                com.agrodoc.app.domain.model.RiskLevel.MODERATE ->
                    "Current weather is moderately conducive to disease development."
                com.agrodoc.app.domain.model.RiskLevel.LOW ->
                    if (confidence < 0.60f) {
                        "Current weather does not favour this disease. Because confidence is moderate, consider re-scanning in good lighting."
                    } else {
                        "Weather conditions do not currently favour rapid disease progression."
                    }
            }
        }

        val title = when (langCode.lowercase()) {
            "kn" -> "$cropName: $diseaseName ರೋಗದ ವಿವರಣೆ"
            "hi" -> "$cropName: $diseaseName रोग का विवरण"
            "mr" -> "$cropName: $diseaseName रोगाची माहिती"
            else -> "$cropName: Explanation of $diseaseName"
        }

        val summary = symptoms
        val whatCausedIt = when (langCode.lowercase()) {
            "kn" -> "ಇದು ${disease.cause} ರೋಗಕಾರಕದಿಂದ ಉಂಟಾಗುತ್ತದೆ. $spread"
            "hi" -> "यह ${disease.cause} रोगजनक के कारण होता है। $spread"
            "mr" -> "हा ${disease.cause} घटकांमुळे होतो. $spread"
            else -> "Caused by ${disease.cause} pathogen. $spread"
        }

        val immediateSteps = treatment.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val longTermPrevention = prevention.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        val officerAdvisory = disease.whenToConsultOfficer.ifEmpty {
            when (langCode.lowercase()) {
                "kn" -> "ಲಕ್ಷಣಗಳು 3-5 ದಿನಗಳಲ್ಲಿ ಕಡಿಮೆಯಾಗದಿದ್ದರೆ ಅಥವಾ 20% ಗಿಂತ ಹೆಚ್ಚು ಹರಡಿದರೆ ಹತ್ತಿರದ ರೈತ ಸಂಪರ್ಕ ಕೇಂದ್ರ ಅಥವಾ ಕೃಷಿ ಅಧಿಕಾರಿಯನ್ನು ಸಂಪರ್ಕಿಸಿ."
                "hi" -> "यदि लक्षण 3-5 दिनों में कम नहीं होते या 20% से अधिक फैलते हैं, तो नजदीकी कृषि अधिकारी या कृषि विज्ञान केंद्र से संपर्क करें।"
                "mr" -> "लक्षणे ३-५ दिवसांत कमी न झाल्यास किंवा २०% पेक्षा जास्त पसरल्यास कृषी अधिकाऱ्यांचा सल्ला घ्या."
                else -> "If symptoms persist beyond 3-5 days or damage exceeds 20% canopy, consult your local Raitha Samparka Kendra or Agricultural Officer."
            }
        }

        return DiagnosticExplanation(
            title = title,
            summary = summary,
            whatCausedIt = whatCausedIt,
            weatherInfluence = weatherInfluence,
            spreadMechanism = spread,
            immediateSteps = immediateSteps,
            longTermPrevention = longTermPrevention,
            officerAdvisory = officerAdvisory,
        )
    }
}

