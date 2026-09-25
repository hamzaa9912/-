package com.example.parser

enum class PartOfSpeech(val arabicName: String) {
    ISM("اسْم"),
    FI_IL("فِعْل"),
    HARF("حَرْف")
}

enum class GrammaticalCase(val arabicName: String, val defaultMarker: String) {
    MARFOO("مَرْفُوع", "الضمة"),
    MANSOOB("مَنْصُوب", "الفتحة"),
    MAJROOR("مَجْرُور", "الكسرة"),
    MAJZOOM("مَجْزُوم", "السكون"),
    MABNI("مَبْنِيّ", "حسب حركة البناء")
}

data class AffixBreakdown(
    val part: String,
    val type: String, // سابقة، لاحقة، ضمير متصل، أداة تعريف...
    val title: String,
    val irabDetail: String
)

data class ParsedWord(
    val index: Int = 0,
    val originalText: String,
    val cleanText: String,
    val vocalizedText: String,
    val root: String,
    val partOfSpeech: PartOfSpeech,
    val partOfSpeechDetail: String,
    val grammaticalRole: String,
    val grammaticalCase: GrammaticalCase,
    val irabSign: String,
    val morphologyWeight: String,
    val prefixes: List<AffixBreakdown> = emptyList(),
    val suffixes: List<AffixBreakdown> = emptyList(),
    val fullIrab: String,
    val ruleExplanation: String,
    val exampleEvidence: String = ""
)

data class ParsedSentence(
    val rawText: String,
    val vocalizedSentence: String,
    val sentenceType: String, // جملة اسمية، جملة فعلية، شبه جملة
    val structureSummary: String,
    val words: List<ParsedWord>,
    val sentenceIrabNote: String = ""
)

data class GrammarTopic(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val summary: String,
    val detailedContent: String,
    val rules: List<String>,
    val examplesWithIrab: List<ExampleIrab>,
    val commonMistakes: String = ""
)

data class ExampleIrab(
    val sentence: String,
    val targetWord: String,
    val irab: String,
    val explanation: String
)

data class QuizQuestion(
    val id: Int,
    val question: String,
    val sentence: String,
    val highlightedWord: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val category: String
)
