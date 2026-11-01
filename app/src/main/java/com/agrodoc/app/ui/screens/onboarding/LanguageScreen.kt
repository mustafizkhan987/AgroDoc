package com.agrodoc.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agrodoc.app.R
import com.agrodoc.app.ui.theme.Green900

/**
 * First-launch language selection screen.
 * Shown exactly once; the choice is persisted in DataStore and applied via
 * AppCompatDelegate.setApplicationLocales so the rest of the app renders in
 * the selected language without restarting.
 *
 * Acceptance criterion (Phase 0): switching language changes the UI text and font.
 */
@Composable
fun LanguageScreen(
    onLanguageChosen: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel(),
) {
    val languages = listOf(
        Triple("kn", "ಕನ್ನಡ", stringResource(R.string.lang_kannada)),
        Triple("hi", "हिन्दी", stringResource(R.string.lang_hindi)),
        Triple("mr", "मराठी", stringResource(R.string.lang_marathi)),
        Triple("en", "English", stringResource(R.string.lang_english)),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4FBF4))
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AgroDoc",
                color = Green900,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lang_prompt),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF4A4A4A),
            )
            Spacer(Modifier.height(40.dp))

            languages.forEachIndexed { index, (tag, nativeLabel, cdLabel) ->
                val isFirst = index == 0
                Button(
                    onClick = {
                        viewModel.pickLanguage(tag, onLanguageChosen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(bottom = if (index < languages.lastIndex) 0.dp else 0.dp)
                        .semantics { contentDescription = cdLabel },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFirst) Green900 else Color(0xFF43A047),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Text(
                        text = nativeLabel,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (index < languages.lastIndex) Spacer(Modifier.height(16.dp))
            }
        }
    }
}
