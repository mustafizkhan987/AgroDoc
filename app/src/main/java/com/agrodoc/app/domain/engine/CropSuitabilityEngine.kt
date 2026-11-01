package com.agrodoc.app.domain.engine

import com.agrodoc.app.domain.model.CropInfo
import com.agrodoc.app.domain.model.District
import com.agrodoc.app.domain.model.WeatherData
import java.util.Calendar

enum class SuitabilityStatus {
    GOOD,
    MARGINAL,
    NOT_RECOMMENDED,
}

data class CropSuitability(
    val cropId: String,
    val status: SuitabilityStatus,
    val headline: String,
    val reasons: List<String>,
)

object CropSuitabilityEngine {

    private val MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun evaluate(
        crop: CropInfo,
        district: District?,
        weather: WeatherData?,
        langCode: String = "en",
        currentMonthIndex: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-based
    ): CropSuitability {
        val currentMonthName = MONTH_NAMES.getOrElse(currentMonthIndex) { "January" }
        val reasons = mutableListOf<String>()
        var score = 0 // higher score -> better suitability

        // 1. Season & Sowing Window Check
        val isInWindow = crop.sowingWindows.any { window ->
            window.sowingMonths.contains(currentMonthName, ignoreCase = true)
        }

        if (isInWindow) {
            score += 3
            reasons.add(
                when (langCode.lowercase()) {
                    "kn" -> "$currentMonthName ತಿಂಗಳು ಬಿತ್ತನೆಗೆ ಸೂಕ್ತ ಸಮಯವಾಗಿದೆ."
                    "hi" -> "$currentMonthName का महीना बुवाई के लिए सही समय है।"
                    "mr" -> "$currentMonthName हा महिना पेरणीसाठी योग्य काळ आहे."
                    else -> "Current month ($currentMonthName) falls within the recommended sowing window."
                }
            )
        } else {
            val validWindows = crop.sowingWindows.joinToString("; ") { "${it.season}: ${it.sowingMonths}" }
            reasons.add(
                when (langCode.lowercase()) {
                    "kn" -> "ಪ್ರಸ್ತುತ ತಿಂಗಳು ($currentMonthName) ಶಿಫಾರಸು ಮಾಡಿದ ಬಿತ್ತನೆ ಅವಧಿಯಲ್ಲ ($validWindows)."
                    "hi" -> "वर्तमान माह ($currentMonthName) अनुशंसित बुवाई अवधि में नहीं है ($validWindows)।"
                    "mr" -> "सध्याचा महिना ($currentMonthName) पेरणीच्या मुख्य हंगामात नाही ($validWindows)."
                    else -> "Current month is outside standard sowing windows ($validWindows)."
                }
            )
        }

        // 2. Temperature Check (from WeatherData)
        if (weather != null) {
            val temp = weather.current.tempC
            val optMin = crop.tempOptMinC ?: crop.tempMinC ?: 15.0
            val optMax = crop.tempOptMaxC ?: crop.tempMaxC ?: 35.0

            if (temp in optMin..optMax) {
                score += 2
                reasons.add(
                    when (langCode.lowercase()) {
                        "kn" -> "ಪ್ರಸ್ತುತ ತಾಪಮಾನ (${temp.toInt()}°C) ಬೆಳೆಯ ಬೆಳವಣಿಗೆಗೆ ಸೂಕ್ತವಾಗಿದೆ (${optMin.toInt()}–${optMax.toInt()}°C)."
                        "hi" -> "वर्तमान तापमान (${temp.toInt()}°C) फसल के अनुकूल है (${optMin.toInt()}–${optMax.toInt()}°C)।"
                        "mr" -> "सध्याचे तापमान (${temp.toInt()}°C) वाढीसाठी योग्य आहे (${optMin.toInt()}–${optMax.toInt()}°C)."
                        else -> "Current temperature (${temp.toInt()}°C) is within optimal range (${optMin.toInt()}–${optMax.toInt()}°C)."
                    }
                )
            } else if (temp < (crop.tempMinC ?: 10.0) || temp > (crop.tempMaxC ?: 42.0)) {
                score -= 2
                reasons.add(
                    when (langCode.lowercase()) {
                        "kn" -> "ತಾಪಮಾನ (${temp.toInt()}°C) ಬೆಳೆಗೆ ತುಂಬಾ ಪ್ರತಿಕೂಲವಾಗಿದೆ."
                        "hi" -> "तापमान (${temp.toInt()}°C) फसल के लिए अत्यधिक प्रतिकूल है।"
                        "mr" -> "तापमान (${temp.toInt()}°C) पिकासाठी प्रतिकूल आहे."
                        else -> "Temperature (${temp.toInt()}°C) is outside acceptable survival limits."
                    }
                )
            } else {
                score += 1
                reasons.add(
                    when (langCode.lowercase()) {
                        "kn" -> "ತಾಪಮಾನ ತಕ್ಕಮಟ್ಟಿಗೆ ಸಹನೀಯವಾಗಿದೆ ಆದರೆ ಅತ್ಯುತ್ತಮವಲ್ಲ."
                        "hi" -> "तापमान सामान्य है पर सर्वोत्तम नहीं।"
                        "mr" -> "तापमान मध्यम आहे, सर्वोत्तम नाही."
                        else -> "Temperature is tolerable but slightly outside ideal range."
                    }
                )
            }
        }

        // 3. District Check
        if (district != null) {
            val isKnownDistrict = crop.karnatakaDistricts.any {
                it.contains(district.district, ignoreCase = true) || district.district.contains(it, ignoreCase = true)
            }
            if (isKnownDistrict || crop.karnatakaDistricts.isEmpty()) {
                score += 1
                reasons.add(
                    when (langCode.lowercase()) {
                        "kn" -> "${district.district} ಜಿಲ್ಲೆಯು ಈ ಬೆಳೆಗೆ ಶಿಫಾರಸು ಮಾಡಿದ ವಲಯಗಳಲ್ಲಿದೆ."
                        "hi" -> "${district.district} जिला इस फसल के अनुकूल कृषि क्षेत्र में आता है।"
                        "mr" -> "${district.district} हा जिल्हा या पिकासाठी शिफारसीत भागात येतो."
                        else -> "${district.district} district is an established agro-climatic zone for this crop."
                    }
                )
            }
        }

        val status = when {
            score >= 4 -> SuitabilityStatus.GOOD
            score in 2..3 -> SuitabilityStatus.MARGINAL
            else -> SuitabilityStatus.NOT_RECOMMENDED
        }

        val headline = when (status) {
            SuitabilityStatus.GOOD -> when (langCode.lowercase()) {
                "kn" -> "ಉತ್ತಮ ಸಮಯ: ಬಿತ್ತನೆಗೆ ಸೂಕ್ತವಾಗಿದೆ"
                "hi" -> "उत्तम समय: बुवाई के लिए उपयुक्त"
                "mr" -> "उत्तम वेळ: पेरणीसाठी योग्य"
                else -> "Good: Highly suitable to sow now"
            }
            SuitabilityStatus.MARGINAL -> when (langCode.lowercase()) {
                "kn" -> "ಮಧ್ಯಮ: ಸೀಮಿತ ಅನುಕೂಲಕರ ಪರಿಸ್ಥಿತಿ"
                "hi" -> "मध्यम: सीमित अनुकूल स्थिति"
                "mr" -> "मध्यम: मर्यादित अनुकूल परिस्थिती"
                else -> "Marginal: Sub-optimal conditions"
            }
            SuitabilityStatus.NOT_RECOMMENDED -> when (langCode.lowercase()) {
                "kn" -> "ಶಿಫಾರಸು ಮಾಡಲಾಗಿಲ್ಲ: ಪ್ರಸ್ತುತ ಬಿತ್ತಬೇಡಿ"
                "hi" -> "अनुशंसित नहीं: अभी बुवाई न करें"
                "mr" -> "शिफारस नाही: सध्या पेरणी करू नका"
                else -> "Not Recommended: Delay sowing"
            }
        }

        return CropSuitability(
            cropId = crop.id,
            status = status,
            headline = headline,
            reasons = reasons,
        )
    }
}

