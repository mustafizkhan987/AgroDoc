package com.agrodoc.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agrodoc.app.R

/**
 * Noto Sans font families bundled in res/font/.
 * Kannada and Devanagari glyphs are tall; line height is set generously (≥1.5×) as
 * required by the architecture spec.
 */
val NotoSansKannada = FontFamily(
    Font(R.font.noto_sans_kannada_regular, FontWeight.Normal),
    Font(R.font.noto_sans_kannada_bold, FontWeight.Bold),
)

val NotoSansDevanagari = FontFamily(
    Font(R.font.noto_sans_devanagari_regular, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari_bold, FontWeight.Bold),
)

/**
 * Typography scale.
 * The font families defined here are the Latin fallbacks; for Indic scripts the system
 * will automatically select the appropriate Noto variant once the fonts are registered
 * in the theme via fontFamily = NotoSansKannada / NotoSansDevanagari at the composable level.
 */
val AgroDocTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 54.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 27.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
)
