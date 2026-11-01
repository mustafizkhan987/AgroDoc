# Translation Review Checklist

**Status:** All translations are DRAFT. Must be reviewed by a native speaker and ideally
an agriculture student or officer before any public demo or academic submission.

| String key | English | Kannada | Hindi | Marathi | Reviewed |
|---|---|---|---|---|---|
| scan_leaf | Scan leaf | ಎಲೆಯನ್ನು ಸ್ಕ್ಯಾನ್ ಮಾಡಿ | पत्ती स्कैन करें | पान स्कॅन करा | ☐ |
| from_gallery | Pick from gallery | ಗ್ಯಾಲರಿಯಿಂದ ಆರಿಸಿ | गैलरी से चुनें | गॅलरीतून निवडा | ☐ |
| retake | Retake photo | ಮತ್ತೆ ಫೋಟೋ ತೆಗೆಯಿರಿ | फिर से फोटो लें | पुन्हा फोटो घ्या | ☐ |
| trust_high | Trust: High ✓ | ವಿಶ್ವಾಸ: ಹೆಚ್ಚು ✓ | भरोसा: ज़्यादा ✓ | विश्वास: जास्त ✓ | ☐ |
| trust_medium | Trust: Medium ⚠ | ವಿಶ್ವಾಸ: ಮಧ್ಯಮ ⚠ | भरोसा: मध्यम ⚠ | विश्वास: मध्यम ⚠ | ☐ |
| trust_low | Trust: Low ✗ | ವಿಶ್ವಾಸ: ಕಡಿಮೆ ✗ | भरोसा: कम ✗ | विश्वास: कमी ✗ | ☐ |
| reason_ok | The model mainly looked at the leaf. | ಮಾದರಿ ಮುಖ್ಯವಾಗಿ ಎಲೆಯ ಮೇಲೆ ಗಮನಹರಿಸಿದೆ. | मॉडल ने मुख्य रूप से पत्ती पर ध्यान दिया। | मॉडेलने मुख्यतः पानावर लक्ष दिले. | ☐ |
| reason_bg | The model looked at the background. Please retake the photo. | ಮಾದರಿ ಹಿನ್ನೆಲೆಯನ್ನು ನೋಡಿದೆ. ದಯವಿಟ್ಟು ಮತ್ತೆ ಫೋಟೋ ತೆಗೆಯಿರಿ. | मॉडल ने पृष्ठभूमि को देखा। कृपया फिर से फोटो लें। | मॉडेलने पार्श्वभूमी पाहिली. कृपया पुन्हा फोटो घ्या. | ☐ |
| reason_unsure | The model is not confident. The result may be wrong. | ಮಾದರಿಗೆ ಖಚಿತವಿಲ್ಲ. ಫಲಿತಾಂಶ ತಪ್ಪಾಗಿರಬಹುದು. | मॉडल को यक़ीन नहीं है। नतीजा गलत हो सकता है। | मॉडेलला खात्री नाही. निकाल चुकीचा असू शकतो. | ☐ |
| disclaimer | AgroDoc is a helper, not a replacement for an agriculture expert… | ಗಂಭೀರ ಸಮಸ್ಯೆಗಳನ್ನು… | गंभीर मामलों में… | गंभीर समस्या… | ☐ |
| explanation_template | The model focused on the %1$s of the leaf (%2$d%% of the highlighted area). | ಮಾದರಿ ಎಲೆಯ %1$s ಭಾಗದ ಮೇಲೆ… | मॉडल ने पत्ती के %1$s हिस्से पर… | मॉडेलने पानाच्या %1$s भागावर… | ☐ |

**Priority agronomy terms for specialist review (needs_review=true):**
Disease names, symptom descriptions and crop names in disease_info.json (to be created in Phase 5).

**Reviewer instructions:**
1. Check every string for natural phrasing — avoid literal machine-translation feel.
2. Verify agronomy terms against the Karnataka/Maharashtra/national agriculture extension vocabulary.
3. Check that format arguments (%1$s, %2$d) are in the right position for the language's word order.
4. Mark ✓ in the Reviewed column and sign off with name and date.
