# Script to add new AgroDoc strings across all 4 locales with exact parity.

NEW_STRINGS = [
    # Navigation
    ("nav_crops", "Crops", "ಬೆಳೆಗಳು", "फसलें", "पिके"),
    ("nav_assistant", "Assistant", "ಸಹಾಯಕ", "सहायक", "मार्गदर्शक"),
    ("nav_fertilizer", "Fertilizer", "ರಸಗೊಬ್ಬರ", "उर्वरक", "खत"),

    # Weather
    ("weather_title", "Weather Forecast", "ಹವಾಮಾನ ಮುನ್ಸೂಚನೆ", "मौसम पूर्वानुमान", "हवामान अंदाज"),
    ("weather_humidity", "Humidity", "ಆರ್ದ್ರತೆ", "नमी", "आर्द्रता"),
    ("weather_rain", "Rain", "ಮಳೆ", "बारिश", "पाऊस"),
    ("weather_wind", "Wind", "ಗಾಳಿ", "हवा", "वारा"),
    ("weather_today", "Today", "ಇಂದು", "आज", "आज"),
    ("weather_updated", "Updated %1$s ago", "%1$s ಹಿಂದೆ ನವೀಕರಿಸಲಾಗಿದೆ", "%1$s पहले अपडेट हुआ", "%1$s पूर्वी अद्ययावत"),
    ("weather_offline_notice", "Weather is offline or cached", "ಆಫ್‌ಲೈನ್‌ನಲ್ಲಿದೆ — ಕೊನೆಯ ಮಾಹಿತಿ", "ऑफ़लाइन — कैश्ड जानकारी", "ऑफलाइन — संग्रहित माहिती"),
    ("weather_pull_to_refresh", "Pull to refresh weather", "ನವೀಕರಿಸಲು ಎಳೆಯಿರಿ", "रिफ्रेश करने के लिए खींचें", "रिफ्रेश करण्यासाठी ओढा"),
    ("district_select", "Select District", "ಜಿಲ್ಲೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ", "जिला चुनें", "जिल्हा निवडा"),
    ("state_select", "Select State", "ರಾಜ್ಯವನ್ನು ಆಯ್ಕೆಮಾಡಿ", "राज्य चुनें", "राज्य निवडा"),
    ("disease_alerts", "Disease Alerts", "ರೋಗ ಎಚ್ಚರಿಕೆಗಳು", "रोग चेतावनी", "रोग सूचना"),
    ("no_alerts", "No high risk alerts for your crops this week.", "ಈ ವಾರ ನಿಮ್ಮ ಬೆಳೆಗಳಿಗೆ ಯಾವುದೇ ಹೆಚ್ಚಿನ ಅಪಾಯದ ಎಚ್ಚರಿಕೆಗಳಿಲ್ಲ.", "इस सप्ताह आपकी फसलों के लिए कोई गंभीर चेतावनी नहीं है।", "या आठवड्यात तुमच्या पिकांसाठी कोणतीही गंभीर सूचना नाही."),
    ("risk_high", "High Risk", "ಹೆಚ್ಚಿನ ಅಪಾಯ", "उच्च जोखिम", "उच्च धोका"),
    ("risk_moderate", "Moderate Risk", "ಮಧ್ಯಮ ಅಪಾಯ", "मध्यम जोखिम", "मध्यम धोका"),
    ("risk_low", "Low Risk", "ಕಡಿಮೆ ಅಪಾಯ", "कम जोखिम", "कमी धोका"),

    # Crop Category
    ("crops_title", "Crops Library", "ಬೆಳೆಗಳ ಮಾಹಿತಿ", "फसल जानकारी", "पिकांची माहिती"),
    ("crops_search", "Search crops…", "ಬೆಳೆಗಳನ್ನು ಹುಡುಕಿ…", "फसलें खोजें…", "पिके शोधा…"),
    ("filter_season", "Season", "ಹಂಗಾಮು", "मौसम", "हंगाम"),
    ("filter_all_seasons", "All Seasons", "ಎಲ್ಲಾ ಹಂಗಾಮುಗಳು", "सभी मौसम", "सर्व हंगाम"),
    ("my_crops", "My Crops", "ನನ್ನ ಬೆಳೆಗಳು", "मेरी फसलें", "माझी पिके"),
    ("add_to_my_crops", "Add to My Crops", "ನನ್ನ ಬೆಳೆಗಳಿಗೆ ಸೇರಿಸಿ", "मेरी फसलों में जोड़ें", "माझ्या पिकांमध्ये जोडा"),
    ("remove_from_my_crops", "Remove from My Crops", "ನನ್ನ ಬೆಳೆಗಳಿಂದ ತೆಗೆದುಹಾಕಿ", "मेरी फसलों से हटाएँ", "माझ्या पिकांतून काढा"),
    ("suitable_climate", "Suitable Climate &amp; Soil", "ಸೂಕ್ತ ಹವಾಮಾನ ಮತ್ತು ಮಣ್ಣು", "अनुकूल जलवायु व मिट्टी", "योग्य हवामान व जमीन"),
    ("growth_stages", "Growth Stages", "ಬೆಳವಣಿಗೆ ಹಂತಗಳು", "विकास के चरण", "वाढीच्या अवस्था"),
    ("suitability_badge", "Sowing Suitability", "ಬಿತ್ತನೆ ಸೂಕ್ತತೆ", "बुवाई उपयुक्तता", "पेरणी योग्यता"),
    ("calculate_fertilizer", "Calculate Fertilizer", "ರಸಗೊಬ್ಬರ ಲೆಕ್ಕಹಾಕಿ", "खाद की गणना करें", "खताचे प्रमाण काढा"),
    ("view_diseases", "Common Diseases", "ಸಾಮಾನ್ಯ ರೋಗಗಳು", "प्रमुख रोग", "मुख्य रोग"),
    ("soil_types", "Soil Types", "ಮಣ್ಣಿನ ವಿಧಗಳು", "मिट्टी के प्रकार", "जमिनीचे प्रकार"),
    ("ideal_temp", "Ideal Temperature", "ಸೂಕ್ತ ತಾಪಮಾನ", "अनुकूल तापमान", "योग्य तापमान"),
    ("annual_rainfall", "Annual Rainfall", "ವಾರ್ಷಿಕ ಮಳೆ", "वार्षिक वर्षा", "वार्षिक पाऊस"),
    ("duration_days", "Duration", "ಅವಧಿ", "अवधि", "कालावधी"),
    ("days_range", "%1$d–%2$d days", "%1$d–%2$d ದಿನಗಳು", "%1$d–%2$d दिन", "%1$d–%2$d दिवस"),

    # Knowledge Assistant
    ("assistant_title", "Knowledge Assistant", "ಕೃಷಿ ಜ್ಞಾನ ಸಹಾಯಕ", "कृषि ज्ञान सहायक", "कृषी ज्ञान मार्गदर्शक"),
    ("assistant_ask_hint", "Ask about crops, diseases, sowing, or care…", "ಬೆಳೆ, ರೋಗ, ಬಿತ್ತನೆ ಅಥವಾ ಆರೈಕೆ ಬಗ್ಗೆ ಕೇಳಿ…", "फसल, रोग, बुवाई या देखरेख के बारे में पूछें…", "पीक, रोग, पेरणी किंवा काळजीबद्दल विचारा…"),
    ("assistant_sources", "Official Source", "ಅಧಿಕೃತ ಮೂಲ", "आधिकारिक स्रोत", "अधिकृत संदर्भ"),
    ("assistant_suggested", "Suggested Questions", "ಸೂಚಿಸಲಾದ ಪ್ರಶ್ನೆಗಳು", "सुझाए गए प्रश्न", "सुचवलेले प्रश्न"),
    ("assistant_send", "Ask", "ಕೇಳಿ", "पूछें", "विचारा"),
    ("assistant_empty", "Ask any question about Karnataka crops, disease symptoms, weather influence, or fertilizers.", "ಕರ್ನಾಟಕದ ಬೆಳೆಗಳು, ರೋಗ ಲಕ್ಷಣಗಳು, ಹವಾಮಾನ ಪ್ರಭಾವ ಅಥವಾ ರಸಗೊಬ್ಬರಗಳ ಬಗ್ಗೆ ಪ್ರಶ್ನೆ ಕೇಳಿ.", "कर्नाटक की फसलों, रोग लक्षणों, मौसम के प्रभाव या उर्वरकों के बारे में प्रश्न पूछें।", "कर्नाटकातील पिके, रोगांची लक्षणे, हवामान किंवा खतांबद्दल प्रश्न विचारा."),

    # Fertilizer
    ("fertilizer_title", "Fertilizer Schedule", "ರಸಗೊಬ್ಬರ ಶಿಫಾರಸು", "उर्वरक अनुसूची", "खत व्यवस्थापन"),
    ("select_crop", "Select Crop", "ಬೆಳೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ", "फसल चुनें", "पीक निवडा"),
    ("area_input", "Land Area", "ಭೂಮಿಯ ವಿಸ್ತೀರ್ಣ", "जमीन का क्षेत्रफल", "जमिनीचे क्षेत्र"),
    ("area_unit", "Unit", "ಘಟಕ", "इकाई", "एकक"),
    ("unit_acre", "Acre", "ಎಕರೆ", "एकड़", "एकर"),
    ("unit_ha", "Hectare", "ಹೆಕ್ಟೇರ್", "हेक्टेयर", "हेक्टर"),
    ("unit_guntha", "Guntha", "ಗುಂಟೆ", "गुंठा", "गुंठा"),
    ("fertilizer_schedule", "Stage-wise Application", "ಹಂತವಾರು ಗೊಬ್ಬರ ನೀಡಿಕೆ", "चरणबद्ध खाद वितरण", "टप्प्याटप्प्याने खत देणे"),
    ("organic_manure", "Organic Manure", "ಸಾವಯವ ಗೊಬ್ಬರ", "जैविक खाद", "सेंद्रिय खते"),
    ("micronutrients_title", "Micronutrients", "ಸೂಕ್ಷ್ಮ ಪೋಷಕಾಂಶಗಳು", "सूक्ष्म पोषक तत्व", "सूक्ष्म अन्नद्रव्ये"),
    ("caution_notes", "Precautions", "ಮುನ್ನೆಚ್ಚರಿಕೆಗಳು", "सावधानियाँ", "काळजी व सूचना"),
    ("save_plan", "Save Plan", "ಯೋಜನೆ ಉಳಿಸಿ", "योजना सहेजें", "नियोजन जतन करा"),
    ("plan_saved", "Plan saved to history", "ಯೋಜನೆಯನ್ನು ಉಳಿಸಲಾಗಿದೆ", "योजना सहेजी गई", "नियोजन जतन केले"),

    # Result Screen Extras
    ("weather_signal", "Weather Factor", "ಹವಾಮಾನ ಅಂಶ", "मौसम कारक", "हवामान घटक"),
    ("blend_slider", "Photo / Heatmap Blend", "ಫೋಟೋ / ಹೀಟ್‌ಮ್ಯಾಪ್ ಮಿಶ್ರಣ", "फोटो / हीटमैप मिश्रण", "फोटो / हीटमॅप मिश्रण"),
    ("explanation_dialog_title", "Why Did This Happen?", "ಇದು ಏಕೆ ಸಂಭವಿಸಿತು?", "यह बीमारी क्यों हुई?", "हा रोग का झाला?"),
    ("consult_officer", "When to consult an officer", "ಅಧಿಕಾರಿಯನ್ನು ಯಾವಾಗ ಸಂಪರ್ಕಿಸಬೇಕು", "अधिकारी से कब संपर्क करें", "अधिकाऱ्यांचा सल्ला कधी घ्यावा"),
    ("scan_manual_mode", "Information only — scanning not yet supported for this crop", "ಮಾಹಿತಿ ಮಾತ್ರ — ಈ ಬೆಳೆಗೆ ಸ್ಕ್ಯಾನಿಂಗ್ ಇನ್ನೂ ಲಭ್ಯವಿಲ್ಲ", "केवल जानकारी — इस फसल के लिए अभी स्कैनिंग उपलब्ध नहीं है", "केवळ माहिती — या पिकासाठी स्कॅनिंग अद्याप उपलब्ध नाही"),

    # About & Privacy Policy
    ("privacy_statement_offline", "100% Offline Promise: AgroDoc runs completely on your device. Zero internet permission, zero data uploaded.", "100% ಆಫ್‌ಲೈನ್ ಭರವಸೆ: ಆಗ್ರೋಡಾಕ್ ಸಂಪೂರ್ಣವಾಗಿ ನಿಮ್ಮ ಸಾಧನದಲ್ಲೇ ಕಾರ್ಯನಿರ್ವಹಿಸುತ್ತದೆ. ಇಂಟರ್ನೆಟ್ ಅನುಮತಿ ಇಲ್ಲ, ಯಾವುದೇ ಮಾಹಿತಿ ಹೊರಹೋಗುವುದಿಲ್ಲ.", "100% ऑफ़लाइन वादा: एग्रोडॉक पूरी तरह से आपके फोन पर चलता है। कोई इंटरनेट अनुमति नहीं, कोई डेटा अपलोड नहीं।", "100% ऑफलाइन वचन: ॲग्रोडॉक पूर्णपणे तुमच्या फोनवर चालते. इंटरनेट परवानगी नाही, कोणताही डेटा अपलोड केला जात नाही."),
    ("privacy_statement_connected", "Privacy First: Leaf photos, scans and farm records never leave your device. Internet access is used strictly and exclusively for local weather forecast data from Open-Meteo.", "ಗೌಪ್ಯತೆಗೆ ಮೊದಲ ಆದ್ಯತೆ: ಎಲೆಯ ಫೋಟೋಗಳು ಮತ್ತು ಸ್ಕ್ಯಾನ್ ಮಾಹಿತಿ ನಿಮ್ಮ ಸಾಧನದಲ್ಲೇ ಉಳಿಯುತ್ತವೆ. ಇಂಟರ್ನೆಟ್ ಅನ್ನು ಕೇವಲ ಓಪನ್-ಮೀಟಿಯೋ ಹವಾಮಾನ ಮಾಹಿತಿಗೆ ಮಾತ್ರ ಬಳಸಲಾಗುತ್ತದೆ.", "गोपनीयता सर्वोपरि: पत्तों की तस्वीरें और स्कैन कभी भी फोन से बाहर नहीं जाते। इंटरनेट केवल ओपन-मीटियो से स्थानीय मौसम जानकारी प्राप्त करने हेतु प्रयुक्त होता है।", "गोपनीयता सर्वोच्च: पानाच्या फोटो आणि स्कॅन कधीही फोनबाहेर जात नाहीत. इंटरनेटचा वापर केवळ ओपन-मीटिओवरून स्थानिक हवामान माहिती मिळवण्यासाठी होतो."),
    ("weather_attribution", "Weather data provided by Open-Meteo (under CC BY 4.0 license).", "ಓಪನ್-ಮೀಟಿಯೋ (CC BY 4.0) ವತಿಯಿಂದ ಹವಾಮಾನ ಮಾಹಿತಿ ಒದಗಿಸಲಾಗಿದೆ.", "ओपन-मीटियो (CC BY 4.0) द्वारा मौसम डेटा प्रदान किया गया है।", "ओपन-मीटिओ (CC BY 4.0) द्वारे हवामान माहिती प्रदान केली आहे."),
    ("offline_flavor_tag", "Offline Edition (Zero Network)", "ಆಫ್‌ಲೈನ್ ಆವೃತ್ತಿ (ನೆಟ್‌ವರ್ಕ್ ರಹಿತ)", "ऑफ़लाइन संस्करण (नेटवर्क मुक्त)", "ऑफलाइन आवृत्ती (नेटवर्क विरहित)"),
    ("connected_flavor_tag", "Connected Edition (Weather Enabled)", "ಕನೆಕ್ಟೆಡ್ ಆವೃತ್ತಿ (ಹವಾಮಾನ ಸಕ್ರಿಯ)", "कनेक्टेड संस्करण (मौसम सक्षम)", "कनेक्टेड आवृत्ती (हवामान सक्षम)"),
]

import os

locales = [
    ("", 1),
    ("-kn", 2),
    ("-hi", 3),
    ("-mr", 4),
]

for loc_suffix, col_idx in locales:
    file_path = os.path.join("app", "src", "main", "res", f"values{loc_suffix}", "strings.xml")
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Find the closing </resources> tag
    idx = content.rfind("</resources>")
    if idx == -1:
        print(f"Error in {file_path}")
        continue

    new_xml_lines = ["\n    <!-- Master Prompt Features Expansion -->\n"]
    for row in NEW_STRINGS:
        key = row[0]
        val = row[col_idx]
        # Check if key is already in content
        if f'name="{key}"' not in content:
            new_xml_lines.append(f'    <string name="{key}">{val}</string>\n')

    updated_content = content[:idx] + "".join(new_xml_lines) + content[idx:]
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(updated_content)

print(f"Successfully added {len(NEW_STRINGS)} new keys across all 4 locales.")

