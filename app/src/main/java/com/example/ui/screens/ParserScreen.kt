package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.parser.ArabicGrammarLexicon
import com.example.parser.GrammaticalCase
import com.example.parser.ParsedWord
import com.example.ui.theme.CaseMabniAmber
import com.example.ui.theme.CaseMajroorRed
import com.example.ui.theme.CaseMajzoomPurple
import com.example.ui.theme.CaseMansoobGreen
import com.example.ui.theme.CaseMarfooBlue
import com.example.ui.viewmodel.IrabViewModel

@Composable
fun ParserScreen(
    viewModel: IrabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val query by viewModel.searchQuery.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()
    val selectedIndex by viewModel.selectedWordIndex.collectAsState()
    val isFavorite by viewModel.isCurrentFavorite.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()

    val diacritics = listOf("َ", "ً", "ُ", "ٌ", "ِ", "ٍ", "ْ", "ّ")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("parser_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة الترويسة / Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.arabic_hero_1790370437326),
                            contentDescription = "فن الخط والنحو العربي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "المُعْرِبُ الشَّامِل",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "إعراب دقيق للكلمات والجمل بدون إنترنت مع النطق الصوتي",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFEF3C7)
                            )
                        }
                    }
                }
            }
        }

        // حقل الإدخال والبحث
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("اكتب كلمة أو جملة لإعرابها بالتفصيل...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearSearch() },
                                    modifier = Modifier.testTag("clear_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // شريط التشكيل السريع
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تشكيل:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        diacritics.forEach { haraka ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.insertDiacritic(haraka) }
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(
                                    text = haraka,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.parseText() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إِعْرَابٌ مُفَصَّل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // أمثلة سريعة جاهزة
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نماذج إعرابية شهيرة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ArabicGrammarLexicon.FAMOUS_SENTENCES) { sentence ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable { viewModel.parseText(sentence) }
                        ) {
                            Text(
                                text = sentence,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // نتائج الإعراب
        parsedResult?.let { result ->
            // بطاقة ملخص الجملة
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = result.sentenceType,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // زر النطق الصوتي للجملة
                                IconButton(
                                    onClick = { viewModel.speak(result.vocalizedSentence) },
                                    modifier = Modifier.testTag("speak_sentence_button")
                                ) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "نطق الجملة",
                                        tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // زر الحفظ في المفضلة
                                IconButton(
                                    onClick = { viewModel.toggleFavorite() },
                                    modifier = Modifier.testTag("favorite_button")
                                ) {
                                    Icon(
                                        if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "المفضلة",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // زر النسخ
                                IconButton(
                                    onClick = {
                                        val fullCopy = buildString {
                                            append("النص: ${result.vocalizedSentence}\n")
                                            append("النوع: ${result.sentenceType}\n")
                                            append("التركيب: ${result.structureSummary}\n\n")
                                            result.words.forEach { w ->
                                                append("• ${w.vocalizedText} (${w.grammaticalRole}): ${w.fullIrab}\n")
                                            }
                                        }
                                        clipboardManager.setText(AnnotatedString(fullCopy))
                                        Toast.makeText(context, "تم نسخ الإعراب بالكامل!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الإعراب")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // الجملة المشكولة بخط بارز
                        Text(
                            text = result.vocalizedSentence,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = result.structureSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // إذا كانت الجملة مكونة من عدة كلمات، اعرض شريط اختيار الكلمات التفاعلي
            if (result.words.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "اضغط على أي كلمة لعرض تفاصيلها النحوية والصرفية:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(result.words) { idx, word ->
                                val isSelected = idx == selectedIndex
                                val caseColor = getCaseColor(word.grammaticalCase)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) caseColor else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) caseColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.selectWord(idx) }
                                        .testTag("word_chip_$idx")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = word.vocalizedText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = word.grammaticalRole,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // بطاقة الكلمة المحددة بالتفصيل الكامل
            val selectedWord = result.words.getOrNull(selectedIndex) ?: result.words.firstOrNull()
            selectedWord?.let { word ->
                item {
                    WordDetailCard(
                        word = word,
                        onSpeak = { viewModel.speak(word.vocalizedText) },
                        onCopy = {
                            val copyWord = "${word.vocalizedText}: ${word.fullIrab}"
                            clipboardManager.setText(AnnotatedString(copyWord))
                            Toast.makeText(context, "تم نسخ إعراب الكلمة", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WordDetailCard(
    word: ParsedWord,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val caseColor = getCaseColor(word.grammaticalCase)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, caseColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("word_detail_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ترويسة الكلمة مع شارة الحالة وأزرار الإجراء
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.vocalizedText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // شارة الحالة الإعرابية
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = caseColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, caseColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = word.grammaticalCase.arabicName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = caseColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onSpeak, modifier = Modifier.testTag("speak_word_button")) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "استمع للكلمة", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ إعراب الكلمة", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // بطاقات البيانات الصرفية السريعة (النوع، الجذر، الميزان، العلامة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickTag(title = "نَوْعُ الكَلِمَة", value = word.partOfSpeech.arabicName, modifier = Modifier.weight(1f))
                QuickTag(title = "المَوْقِعُ الإِعْرَابِي", value = word.grammaticalRole, modifier = Modifier.weight(1.3f))
                QuickTag(title = "الجَذْرُ اللُّغَوِي", value = word.root, modifier = Modifier.weight(0.9f))
                QuickTag(title = "المِيزَانُ الصَّرْفِي", value = word.morphologyWeight, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.4f))

            // الإعراب النموذجي المفصل
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "الإِعْرَابُ التَّفْصِيلِيُّ الكَامِل:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = word.fullIrab,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // تجزئة السوابق واللواحق والضمائر إذا وجدت
            if (word.prefixes.isNotEmpty() || word.suffixes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "الزَّوَائِدُ وَالضَّمَائِرُ المُتَّصِلَةُ:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (word.prefixes + word.suffixes).forEach { affix ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = affix.part,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${affix.title})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = affix.irabDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }
            }

            // شرح القاعدة النحوية
            if (word.ruleExplanation.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "الشَّرْحُ النَّحْوِيُّ وَالقَاعِدَة:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = word.ruleExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickTag(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

fun getCaseColor(caseVal: GrammaticalCase): Color {
    return when (caseVal) {
        GrammaticalCase.MARFOO -> CaseMarfooBlue
        GrammaticalCase.MANSOOB -> CaseMansoobGreen
        GrammaticalCase.MAJROOR -> CaseMajroorRed
        GrammaticalCase.MAJZOOM -> CaseMajzoomPurple
        GrammaticalCase.MABNI -> CaseMabniAmber
    }
}
