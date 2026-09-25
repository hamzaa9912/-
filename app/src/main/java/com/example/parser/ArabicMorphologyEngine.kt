package com.example.parser

object ArabicMorphologyEngine {

    // علامات التشكيل وحروف الزيادة
    private val HARAKAT_REGEX = Regex("[\\u064B-\\u065F\\u0670]")
    private val PUNCTUATION_REGEX = Regex("[،؛.؟!:\"]")

    fun removeDiacritics(text: String): String {
        return text.replace(HARAKAT_REGEX, "").replace("ـ", "").trim()
    }

    fun cleanInput(text: String): String {
        return text.replace(PUNCTUATION_REGEX, " ").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * إعراب جملة أو كلمة واحدة إعراباً شاملاً
     */
    fun parse(input: String): ParsedSentence {
        val cleaned = cleanInput(input)
        if (cleaned.isBlank()) {
            return ParsedSentence(
                rawText = input,
                vocalizedSentence = "",
                sentenceType = "فارغ",
                structureSummary = "يرجى كتابة كلمة أو جملة لإعرابها",
                words = emptyList()
            )
        }

        val rawTokens = cleaned.split(" ").filter { it.isNotBlank() }
        val parsedWords = mutableListOf<ParsedWord>()

        // سياق الجملة لتحديد العلاقات النحوية بين الكلمات المتتالية
        var prevWord: ParsedWord? = null
        var prevWasPreposition = false
        var prevWasNasikhInna = false
        var prevWasNasikhKana = false
        var prevWasJazim = false
        var prevWasNasib = false
        var foundVerb: ParsedWord? = null
        var foundSubject: ParsedWord? = null
        var foundInnaNoun: ParsedWord? = null
        var foundKanaNoun: ParsedWord? = null
        var foundMubtada: ParsedWord? = null

        for (i in rawTokens.indices) {
            val token = rawTokens[i]
            val parsed = parseSingleWordInContext(
                raw = token,
                index = i,
                isFirstWord = (i == 0),
                prevWord = prevWord,
                prevWasPreposition = prevWasPreposition,
                prevWasNasikhInna = prevWasNasikhInna,
                prevWasNasikhKana = prevWasNasikhKana,
                prevWasJazim = prevWasJazim,
                prevWasNasib = prevWasNasib,
                foundVerb = foundVerb,
                foundSubject = foundSubject,
                foundInnaNoun = foundInnaNoun,
                foundKanaNoun = foundKanaNoun,
                foundMubtada = foundMubtada
            )

            parsedWords.add(parsed)

            // تحديث مؤشرات السياق للكلمة التالية
            val clean = parsed.cleanText
            prevWasPreposition = ArabicGrammarLexicon.PREPOSITIONS.containsKey(clean) ||
                    ArabicGrammarLexicon.PREPOSITIONS.containsKey(parsed.originalText)
            prevWasNasikhInna = ArabicGrammarLexicon.INNA_SISTERS.containsKey(clean) ||
                    ArabicGrammarLexicon.INNA_SISTERS.containsKey(parsed.originalText)
            prevWasNasikhKana = ArabicGrammarLexicon.KANA_SISTERS.containsKey(clean) ||
                    ArabicGrammarLexicon.KANA_SISTERS.containsKey(parsed.originalText)
            prevWasJazim = ArabicGrammarLexicon.JAZIM_PARTICLES.containsKey(clean) ||
                    ArabicGrammarLexicon.JAZIM_PARTICLES.containsKey(parsed.originalText)
            prevWasNasib = ArabicGrammarLexicon.NASIB_PARTICLES.containsKey(clean) ||
                    ArabicGrammarLexicon.NASIB_PARTICLES.containsKey(parsed.originalText)

            if (parsed.partOfSpeech == PartOfSpeech.FI_IL && foundVerb == null && !prevWasNasikhKana) {
                foundVerb = parsed
            }
            if (parsed.grammaticalRole.contains("فاعل") && foundSubject == null) {
                foundSubject = parsed
            }
            if (parsed.grammaticalRole.contains("اسم إن") && foundInnaNoun == null) {
                foundInnaNoun = parsed
            }
            if (parsed.grammaticalRole.contains("اسم كان") && foundKanaNoun == null) {
                foundKanaNoun = parsed
            }
            if (parsed.grammaticalRole.contains("مبتدأ") && foundMubtada == null) {
                foundMubtada = parsed
            }

            prevWord = parsed
        }

        // نوع الجملة العام
        val sentenceType: String
        val structureSummary: String
        val sentenceNote: String

        if (parsedWords.size == 1) {
            val single = parsedWords.first()
            sentenceType = "كلمة مفردة (${single.partOfSpeech.arabicName})"
            structureSummary = "${single.partOfSpeechDetail} - ${single.grammaticalRole}"
            sentenceNote = "تحليل مفصل لبنية الكلمة وموقعها وحركاتها الإعرابية الصرفية والنحوية."
        } else {
            val first = parsedWords.first()
            if (first.partOfSpeech == PartOfSpeech.FI_IL ||
                (parsedWords.size > 1 && parsedWords[1].partOfSpeech == PartOfSpeech.FI_IL && first.partOfSpeech == PartOfSpeech.HARF)
            ) {
                sentenceType = "جُمْلَةٌ فِعْلِيَّةٌ"
                structureSummary = "تتكون من فعل مسند وفاعل مسند إليه ومتممات (مفعول به / شبه جملة)"
                sentenceNote = "الجملة الفعلية تبدأ بفعل وتدل على الحدوث والتجدد في زمن معين."
            } else if (ArabicGrammarLexicon.INNA_SISTERS.containsKey(first.cleanText)) {
                sentenceType = "جُمْلَةٌ اسْمِيَّةٌ مَنْسُوخَةٌ بِـ (إنَّ)"
                structureSummary = "حرف ناسخ + اسمه منصوب + خبره مرفوع"
                sentenceNote = "إن وأخواتها تدخل على الجملة الاسمية فتنصب المبتدأ وترفع الخبر."
            } else if (ArabicGrammarLexicon.KANA_SISTERS.containsKey(first.cleanText)) {
                sentenceType = "جُمْلَةٌ اسْمِيَّةٌ مَنْسُوخَةٌ بِـ (كانَ)"
                structureSummary = "فعل ناسخ ناقص + اسمه مرفوع + خبره منصوب"
                sentenceNote = "كان وأخواتها تدخل على الجملة الاسمية فترفع المبتدأ وتنصب الخبر."
            } else if (ArabicGrammarLexicon.PREPOSITIONS.containsKey(first.cleanText)) {
                sentenceType = "شِبْهُ جُمْلَةٍ (جَارٌّ وَمَجْرُورٌ)"
                structureSummary = "شبه جملة في محل رفع خبر مقدم أو متعلق بفعل محذوف"
                sentenceNote = "شبه الجملة لا يستقل بنفسه ويحتاج إلى متعلق يتمم المعنى."
            } else {
                sentenceType = "جُمْلَةٌ اسْمِيَّةٌ"
                structureSummary = "تتكون من مبتدأ مرفوع وخبر مرفوع متمم للفائدة"
                sentenceNote = "الجملة الاسمية تدل على الثبوت والاستقرار."
            }
        }

        val vocalized = parsedWords.joinToString(" ") { it.vocalizedText }

        return ParsedSentence(
            rawText = input,
            vocalizedSentence = vocalized,
            sentenceType = sentenceType,
            structureSummary = structureSummary,
            words = parsedWords,
            sentenceIrabNote = sentenceNote
        )
    }

    /**
     * إعراب كلمة مفردة بالنظر إلى السياق والخصائص الصرفية
     */
    private fun parseSingleWordInContext(
        raw: String,
        index: Int,
        isFirstWord: Boolean,
        prevWord: ParsedWord?,
        prevWasPreposition: Boolean,
        prevWasNasikhInna: Boolean,
        prevWasNasikhKana: Boolean,
        prevWasJazim: Boolean,
        prevWasNasib: Boolean,
        foundVerb: ParsedWord?,
        foundSubject: ParsedWord?,
        foundInnaNoun: ParsedWord?,
        foundKanaNoun: ParsedWord?,
        foundMubtada: ParsedWord?
    ): ParsedWord {
        val clean = removeDiacritics(raw)

        // 1. فحص الكلمات المعربة الجاهزة المشهورة بدقة عالية أولاً
        ArabicGrammarLexicon.CURATED_FULL_PARSES[clean]?.let { curated ->
            // إذا كان لفظ الجلالة في سياق بعد حرف جر مثلاً: "من الله"
            if (clean == "الله" && prevWasPreposition) {
                return curated.copy(
                    index = index,
                    vocalizedText = "اللَّهِ",
                    grammaticalRole = "لفظ الجلالة اسم مجرور",
                    grammaticalCase = GrammaticalCase.MAJROOR,
                    irabSign = "الكسرة الظاهرة على آخره",
                    fullIrab = "لفظ الجلالة: اسم مجرور بحرف الجر السابق وعلامة جره الكسرة الظاهرة على آخره تعظيماً وأدباً مع الله تعالى."
                )
            }
            if (clean == "الله" && prevWasNasikhInna) {
                return curated.copy(
                    index = index,
                    vocalizedText = "اللَّهَ",
                    grammaticalRole = "لفظ الجلالة اسم إنّ",
                    grammaticalCase = GrammaticalCase.MANSOOB,
                    irabSign = "الفتحة الظاهرة على آخره",
                    fullIrab = "لفظ الجلالة: اسم (إنّ) منصوب وعلامة نصبه الفتحة الظاهرة على آخره تعظيماً لله سبحانه."
                )
            }
            return curated.copy(index = index, originalText = raw)
        }

        // 2. فحص حروف الجر
        if (ArabicGrammarLexicon.PREPOSITIONS.containsKey(clean)) {
            val detail = ArabicGrammarLexicon.PREPOSITIONS[clean] ?: "حرف جر مبني لا محل له من الإعراب."
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeWord(raw, clean, PartOfSpeech.HARF),
                root = clean,
                partOfSpeech = PartOfSpeech.HARF,
                partOfSpeechDetail = "حرف جر أصلي",
                grammaticalRole = "حرف جر",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني على السكون لا محل له من الإعراب",
                morphologyWeight = "حرف غير مشتق",
                fullIrab = detail,
                ruleExplanation = "حروف الجر تدخل على الأسماء فقط وتجرها، وتتعلق بالفعل أو شبه الفعل أو الخبر المحذوف."
            )
        }

        // 3. فحص إن وأخواتها
        if (ArabicGrammarLexicon.INNA_SISTERS.containsKey(clean)) {
            val detail = ArabicGrammarLexicon.INNA_SISTERS[clean] ?: "حرف ناسخ مبني على الفتح."
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeWord(raw, clean, PartOfSpeech.HARF),
                root = clean,
                partOfSpeech = PartOfSpeech.HARF,
                partOfSpeechDetail = "حرف ناسخ مشبه بالفعل",
                grammaticalRole = "أداة توكيد ونصب ناسخة",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني على الفتح لا محل له من الإعراب",
                morphologyWeight = "حرف ناسخ",
                fullIrab = detail,
                ruleExplanation = "الحروف الناسخة تدخل على الجملة الاسمية، فتنصب المبتدأ ويسمى اسمها، وترفع الخبر ويسمى خبرها."
            )
        }

        // 4. فحص كان وأخواتها
        if (ArabicGrammarLexicon.KANA_SISTERS.containsKey(clean)) {
            val detail = ArabicGrammarLexicon.KANA_SISTERS[clean] ?: "فعل ماضٍ ناسخ ناقص."
            val isPresent = clean.startsWith("ي") || clean.startsWith("ت") || clean.startsWith("أ") || clean.startsWith("ن")
            val isOrder = clean == "كن" || clean == "كُن"
            val tense = if (isOrder) "فعل أمر ناسخ ناقص" else if (isPresent) "فعل مضارع ناسخ ناقص" else "فعل ماضٍ ناسخ ناقص"
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeWord(raw, clean, PartOfSpeech.FI_IL),
                root = "ك-و-ن",
                partOfSpeech = PartOfSpeech.FI_IL,
                partOfSpeechDetail = tense,
                grammaticalRole = "فعل ناسخ ناقص",
                grammaticalCase = if (isPresent) GrammaticalCase.MARFOO else GrammaticalCase.MABNI,
                irabSign = if (isOrder) "مبني على السكون" else if (isPresent) "مرفوع بالضمة الظاهرة" else "مبني على الفتح",
                morphologyWeight = "فَعَلَ",
                fullIrab = detail,
                ruleExplanation = "الأفعال الناسخة الناقصة تدخل على الجملة الاسمية فترفع المبتدأ اسماً لها وتنصب الخبر خبراً لها."
            )
        }

        // 5. فحص أدوات الجزم
        if (ArabicGrammarLexicon.JAZIM_PARTICLES.containsKey(clean)) {
            val detail = ArabicGrammarLexicon.JAZIM_PARTICLES[clean] ?: "حرف جزم مبني لا محل له من الإعراب."
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeWord(raw, clean, PartOfSpeech.HARF),
                root = clean,
                partOfSpeech = PartOfSpeech.HARF,
                partOfSpeechDetail = "أداة جزم الفعل المضارع",
                grammaticalRole = "حرف جزم ونفي وقلب",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني على السكون لا محل له من الإعراب",
                morphologyWeight = "أداة جزم",
                fullIrab = detail,
                ruleExplanation = "أدوات الجزم تجزم فعلاً مضارعاً واحداً (لم، لما، لا الناهية، لام الأمر) بالسكون أو بحذف النون أو بحذف حرف العلة."
            )
        }

        // 6. فحص أدوات النصب
        if (ArabicGrammarLexicon.NASIB_PARTICLES.containsKey(clean)) {
            val detail = ArabicGrammarLexicon.NASIB_PARTICLES[clean] ?: "حرف نصب مبني على السكون."
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeWord(raw, clean, PartOfSpeech.HARF),
                root = clean,
                partOfSpeech = PartOfSpeech.HARF,
                partOfSpeechDetail = "أداة نصب الفعل المضارع",
                grammaticalRole = "حرف مصدري ونصب",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني على السكون لا محل له من الإعراب",
                morphologyWeight = "أداة نصب",
                fullIrab = detail,
                ruleExplanation = "أدوات النصب تنصب الفعل المضارع بالفتحة الظاهرة أو المقدرة، أو بحذف النون في الأفعال الخمسة."
            )
        }

        // 7. فحص حروف العطف
        if (ArabicGrammarLexicon.CONJUNCTIONS.containsKey(clean) || (clean.length == 1 && (clean == "و" || clean == "ف"))) {
            val detail = ArabicGrammarLexicon.CONJUNCTIONS[clean] ?: "حرف عطف مبني لا محل له من الإعراب."
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = if (clean == "ثم") "ثُمَّ" else if (clean == "أو") "أَوْ" else "$cleanَ",
                root = clean,
                partOfSpeech = PartOfSpeech.HARF,
                partOfSpeechDetail = "حرف عطف",
                grammaticalRole = "حرف عطف",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني لا محل له من الإعراب",
                morphologyWeight = "حرف عطف",
                fullIrab = detail,
                ruleExplanation = "حرف العطف يربط بين معطوف ومعطوف عليه ويتبعه في الإعراب (رفعاً ونصباً وجراً وجزماً)."
            )
        }

        // 8. فحص الضمائر المنفصلة
        ArabicGrammarLexicon.PRONOUNS[clean]?.let { (type, irab, voc) ->
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = voc,
                root = clean,
                partOfSpeech = PartOfSpeech.ISM,
                partOfSpeechDetail = type,
                grammaticalRole = "ضمير منفصل في محل رفع مبتدأ",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني في محل رفع مبتدأ",
                morphologyWeight = "ضمير معرفة",
                fullIrab = irab,
                ruleExplanation = "الضمائر المنفصلة مبنية دائماً وتأتي في محل رفع مبتدأ في صدر الكلام، أو في محل نصب مع 'إياك'."
            )
        }

        // 9. فحص أسماء الإشارة
        ArabicGrammarLexicon.DEMONSTRATIVES[clean]?.let { irab ->
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = raw,
                root = clean,
                partOfSpeech = PartOfSpeech.ISM,
                partOfSpeechDetail = "اسم إشارة مبني",
                grammaticalRole = if (isFirstWord) "مبتدأ" else "اسم إشارة",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني في محل " + (if (prevWasPreposition) "جر" else if (isFirstWord) "رفع مبتدأ" else "نصب/رفع"),
                morphologyWeight = "اسم إشارة",
                fullIrab = irab,
                ruleExplanation = "أسماء الإشارة من المعارف، وتبنى على حركة آخرها في محل رفع أو نصب أو جر عدا المثنى فهما معربان."
            )
        }

        // 10. فحص الأسماء الموصولة
        ArabicGrammarLexicon.RELATIVE_PRONOUNS[clean]?.let { irab ->
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = raw,
                root = clean,
                partOfSpeech = PartOfSpeech.ISM,
                partOfSpeechDetail = "اسم موصول مبني",
                grammaticalRole = "اسم موصول (نعت أو بحسب موقعه)",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = "مبني في محل " + (if (prevWasPreposition) "جر" else "رفع/نصب"),
                morphologyWeight = "اسم موصول",
                fullIrab = irab,
                ruleExplanation = "الاسم الموصول يحتاج إلى جملة صلة توضحه ولا محل لها من الإعراب، ويكون له محل بحسب موقعه."
            )
        }

        // 11. فحص الأسماء الخمسة
        ArabicGrammarLexicon.FIVE_NOUNS[clean]?.let { irab ->
            val isWaw = clean.endsWith("و")
            val isAlif = clean.endsWith("ا") || clean.endsWith("ى")
            val isYaa = clean.endsWith("ي")
            val posCase = if (isWaw) GrammaticalCase.MARFOO else if (isAlif) GrammaticalCase.MANSOOB else GrammaticalCase.MAJROOR
            val sign = if (isWaw) "الواو نيابة عن الضمة" else if (isAlif) "الألف نيابة عن الفتحة" else "الياء نيابة عن الكسرة"
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = raw,
                root = clean.take(2),
                partOfSpeech = PartOfSpeech.ISM,
                partOfSpeechDetail = "اسم من الأسماء الخمسة المعربة بالحروف",
                grammaticalRole = if (posCase == GrammaticalCase.MARFOO) "مبتدأ / فاعل" else if (posCase == GrammaticalCase.MANSOOB) "مفعول به / اسم إن" else "اسم مجرور / مضاف إليه",
                grammaticalCase = posCase,
                irabSign = sign,
                morphologyWeight = "فَعَل",
                fullIrab = "$irab، وعلامة إعرابه $sign لأنه من الأسماء الخمسة، وما بعده مضاف إليه.",
                ruleExplanation = "تُعرب الأسماء الخمسة بالحروف: ترفع بالواو، وتنصب بالألف، وتجر بالياء بشرط أن تكون مفردة ومضافة لغير ياء المتكلم."
            )
        }

        // 12. تجريد السوابق واللواحق الصرفية والتحليل البنيوي
        val decomposition = decomposeWord(raw, clean)
        val strippedBase = decomposition.baseWord
        val prefixes = decomposition.prefixes
        val suffixes = decomposition.suffixes

        // فحص علامات الأسماء الواضحة
        val hasAlifLam = clean.startsWith("ال") || prefixes.any { it.part == "الـ" }
        val hasTanween = raw.contains("ً") || raw.contains("ٍ") || raw.contains("ٌ") || raw.endsWith("اً")
        val hasTaMarbuta = clean.endsWith("ة") || clean.endsWith("ه")
        val endsWithPluralWawNoon = clean.endsWith("ون")
        val endsWithDualAlifNoon = clean.endsWith("ان")
        val endsWithYaaNoon = clean.endsWith("ين")
        val endsWithAlifTa = clean.endsWith("ات")

        // فحص علامات الأفعال
        val isVerbIndicator = prevWasJazim || prevWasNasib ||
                clean.startsWith("سي") || clean.startsWith("ست") || clean.startsWith("سن") || clean.startsWith("سأ") ||
                suffixes.any { it.part.contains("تُ") || it.part.contains("تَ") || it.part.contains("وا") }

        // استنتاج نوع الكلمة (اسم / فعل)
        val isDefiniteNoun = hasAlifLam || hasTanween || hasTaMarbuta || prevWasPreposition
        val isProbableVerb = isVerbIndicator || (!isDefiniteNoun && isLikelyVerb(clean, strippedBase))

        if (isProbableVerb) {
            return parseVerb(
                raw = raw,
                clean = clean,
                strippedBase = strippedBase,
                index = index,
                prefixes = prefixes,
                suffixes = suffixes,
                prevWasJazim = prevWasJazim,
                prevWasNasib = prevWasNasib
            )
        } else {
            return parseNoun(
                raw = raw,
                clean = clean,
                strippedBase = strippedBase,
                index = index,
                isFirstWord = isFirstWord,
                prefixes = prefixes,
                suffixes = suffixes,
                prevWord = prevWord,
                prevWasPreposition = prevWasPreposition,
                prevWasNasikhInna = prevWasNasikhInna,
                prevWasNasikhKana = prevWasNasikhKana,
                foundVerb = foundVerb,
                foundSubject = foundSubject,
                foundInnaNoun = foundInnaNoun,
                foundKanaNoun = foundKanaNoun,
                foundMubtada = foundMubtada,
                endsWithPluralWawNoon = endsWithPluralWawNoon,
                endsWithDualAlifNoon = endsWithDualAlifNoon,
                endsWithYaaNoon = endsWithYaaNoon,
                endsWithAlifTa = endsWithAlifTa
            )
        }
    }

    /**
     * إعراب الأفعال (ماضٍ، مضارع، أمر) مع الضمائر والزوائد
     */
    private fun parseVerb(
        raw: String,
        clean: String,
        strippedBase: String,
        index: Int,
        prefixes: List<AffixBreakdown>,
        suffixes: List<AffixBreakdown>,
        prevWasJazim: Boolean,
        prevWasNasib: Boolean
    ): ParsedWord {
        val root = extractRoot(strippedBase)
        val hasWawGamaa = clean.endsWith("وا") || suffixes.any { it.part == "ـوا" }
        val hasTaFaill = clean.endsWith("ت") || clean.endsWith("تُ") || clean.endsWith("تَ") || clean.endsWith("تِ")
        val hasNoonNiswa = clean.endsWith("ن") && clean.length > 3
        val startsWithMudariLetter = clean.startsWith("ي") || clean.startsWith("ت") || clean.startsWith("أ") || clean.startsWith("ن")
        val startsWithSeen = prefixes.any { it.part == "سـ" } || clean.startsWith("س")

        // تحديد زمن الفعل
        val isMudari = startsWithMudariLetter || startsWithSeen || prevWasJazim || prevWasNasib
        val isAmr = clean.startsWith("ا") && !hasAlifLamPrefix(clean) && !isMudari

        if (isAmr) {
            val weight = deduceWeight(clean, root, true)
            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeVerb(clean, "امر"),
                root = root,
                partOfSpeech = PartOfSpeech.FI_IL,
                partOfSpeechDetail = "فعل أمر مبني للمخاطب",
                grammaticalRole = "فعل أمر",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = if (hasWawGamaa) "مبني على حذف النون لاتصاله بواو الجماعة" else "مبني على السكون الظاهر على آخره",
                morphologyWeight = weight,
                prefixes = prefixes,
                suffixes = suffixes,
                fullIrab = if (hasWawGamaa) {
                    "فعل أمر مبني على حذف النون لاتصاله بواو الجماعة، وواو الجماعة ضمير متصل مبني على السكون في محل رفع فاعل، والألف فارقة."
                } else {
                    "فعل أمر مبني على السكون الظاهر على آخره، والفاعل ضمير مستتر وجوباً تقديره (أنت)."
                },
                ruleExplanation = "فعل الأمر مبني دائماً، ويبنى على ما يجزم به مضارعه (السكون إذا كان صحيح الآخر، حذف حرف العلة للمعتل، حذف النون مع ألف الاثنين أو واو الجماعة أو ياء المخاطبة)."
            )
        } else if (isMudari) {
            val isAfalKhamsa = clean.endsWith("ون") || clean.endsWith("ين") || clean.endsWith("ان")
            val weight = deduceWeight(clean, root, false)

            val (caseVal, signVal, irabText) = when {
                prevWasJazim -> {
                    if (isAfalKhamsa) {
                        Triple(
                            GrammaticalCase.MAJZOOM,
                            "حذف النون لأنه من الأفعال الخمسة",
                            "فعل مضارع مجزوم بأداة الجزم السابقة وعلامة جزمه حذف النون لأنه من الأفعال الخمسة، والضمير المتصل في محل رفع فاعل."
                        )
                    } else {
                        Triple(
                            GrammaticalCase.MAJZOOM,
                            "السكون الظاهر على آخره",
                            "فعل مضارع مجزوم بأداة الجزم وعلامة جزمه السكون الظاهر على آخره، والفاعل ضمير مستتر أو اسم ظاهر يليه."
                        )
                    }
                }
                prevWasNasib -> {
                    if (isAfalKhamsa) {
                        Triple(
                            GrammaticalCase.MANSOOB,
                            "حذف النون لأنه من الأفعال الخمسة",
                            "فعل مضارع منصوب بأداة النصب وعلامة نصبه حذف النون لأنه من الأفعال الخمسة، والواو ضمير متصل في محل رفع فاعل."
                        )
                    } else {
                        Triple(
                            GrammaticalCase.MANSOOB,
                            "الفتحة الظاهرة على آخره",
                            "فعل مضارع منصوب بأداة النصب وعلامة نصبه الفتحة الظاهرة على آخره، والفاعل مستتر تقديره (هو/أنا/نحن)."
                        )
                    }
                }
                else -> {
                    if (isAfalKhamsa) {
                        Triple(
                            GrammaticalCase.MARFOO,
                            "ثبوت النون لأنه من الأفعال الخمسة",
                            "فعل مضارع مرفوع وعلامة رفعه ثبوت النون لأنه من الأفعال الخمسة، و(واو الجماعة / ألف الاثنين / ياء المخاطبة) ضمير متصل مبني في محل رفع فاعل."
                        )
                    } else {
                        Triple(
                            GrammaticalCase.MARFOO,
                            "الضمة الظاهرة على آخره",
                            "فعل مضارع مرفوع لتجرده من الناصب والجازم، وعلامة رفعه الضمة الظاهرة على آخره."
                        )
                    }
                }
            }

            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeVerb(clean, "مضارع"),
                root = root,
                partOfSpeech = PartOfSpeech.FI_IL,
                partOfSpeechDetail = "فعل مضارع معرب",
                grammaticalRole = "فعل مضارع " + caseVal.arabicName,
                grammaticalCase = caseVal,
                irabSign = signVal,
                morphologyWeight = weight,
                prefixes = prefixes,
                suffixes = suffixes,
                fullIrab = irabText,
                ruleExplanation = "الفعل المضارع معرب بالأصل؛ يرفع بالضمة وثبوت النون، وينصب بالفتحة وحذف النون إذا سبق بناصب، ويجزم بالسكون أو حذف العلة وحذف النون إذا سبق بجازم."
            )
        } else {
            // فعل ماضٍ
            val weight = deduceWeight(clean, root, false)
            val isMabniAlaDamm = hasWawGamaa
            val isMabniAlaSukoon = hasTaFaill || hasNoonNiswa || clean.endsWith("نا")
            val sign = if (isMabniAlaDamm) "مبني على الضم لاتصاله بواو الجماعة"
            else if (isMabniAlaSukoon) "مبني على السكون لاتصاله بضمير رفع متحرك"
            else "مبني على الفتح الظاهر على آخره"

            val irabText = if (isMabniAlaDamm) {
                "فعل ماضٍ مبني على الضم لاتصاله بواو الجماعة، وواو الجماعة ضمير متصل مبني على السكون في محل رفع فاعل."
            } else if (isMabniAlaSukoon) {
                val pronounName = if (hasTaFaill) "تاء الفاعل" else if (hasNoonNiswa) "نون النسوة" else "نا الفاعلين"
                "فعل ماضٍ مبني على السكون لاتصاله بـ($pronounName)، والضمير متصل مبني في محل رفع فاعل."
            } else {
                "فعل ماضٍ مبني على الفتح الظاهر على آخره لا محل له من الإعراب، والفاعل ضمير مستتر تقديره (هو) أو اسم ظاهر يليه."
            }

            return ParsedWord(
                index = index,
                originalText = raw,
                cleanText = clean,
                vocalizedText = vocalizeVerb(clean, "ماض"),
                root = root,
                partOfSpeech = PartOfSpeech.FI_IL,
                partOfSpeechDetail = "فعل ماضٍ مبني",
                grammaticalRole = "فعل ماضٍ",
                grammaticalCase = GrammaticalCase.MABNI,
                irabSign = sign,
                morphologyWeight = weight,
                prefixes = prefixes,
                suffixes = suffixes,
                fullIrab = irabText,
                ruleExplanation = "الفعل الماضي مبني دائماً؛ يبنى على الفتح كأصل، وعلى الضم مع واو الجماعة، وعلى السكون عند اتصاله بضمائر الرفع المتحركة (التاء، نا، نون النسوة)."
            )
        }
    }

    /**
     * إعراب الأسماء وتحديد الموقع الإعرابي في الجملة
     */
    private fun parseNoun(
        raw: String,
        clean: String,
        strippedBase: String,
        index: Int,
        isFirstWord: Boolean,
        prefixes: List<AffixBreakdown>,
        suffixes: List<AffixBreakdown>,
        prevWord: ParsedWord?,
        prevWasPreposition: Boolean,
        prevWasNasikhInna: Boolean,
        prevWasNasikhKana: Boolean,
        foundVerb: ParsedWord?,
        foundSubject: ParsedWord?,
        foundInnaNoun: ParsedWord?,
        foundKanaNoun: ParsedWord?,
        foundMubtada: ParsedWord?,
        endsWithPluralWawNoon: Boolean,
        endsWithDualAlifNoon: Boolean,
        endsWithYaaNoon: Boolean,
        endsWithAlifTa: Boolean
    ): ParsedWord {
        val root = extractRoot(strippedBase)
        val weight = deduceWeight(clean, root, false)
        val hasDefiniteArticle = clean.startsWith("ال") || prefixes.any { it.part == "الـ" }

        // تحديد الحالة الإعرابية والعلامة
        val posCase: GrammaticalCase
        val role: String
        val irabSign: String
        val fullIrabText: String
        val ruleExplanation: String

        when {
            // 1. مسبوق بحرف جر -> اسم مجرور
            prevWasPreposition -> {
                posCase = GrammaticalCase.MAJROOR
                role = "اسم مجرور بحرف الجر"
                irabSign = if (endsWithPluralWawNoon || endsWithYaaNoon) {
                    "الياء لأنه جمع مذكر سالم (أو مثنى)"
                } else if (endsWithAlifTa) {
                    "الكسرة الظاهرة على آخره"
                } else {
                    "الكسرة الظاهرة على آخره"
                }
                fullIrabText = "اسم مجرور بحرف الجر السابق وعلامة جره $irabSign، والجار والمجرور متعلق بالفعل أو بالخبر."
                ruleExplanation = "الاسم الواقع بعد حرف الجر يجر بالكسرة كعلامة أصلية، أو بالياء في المثنى وجمع المذكر السالم والأسماء الخمسة."
            }

            // 2. مسبوق بـ إن وأخواتها ولم يظهر اسمها بعد -> اسم إن منصوب
            prevWasNasikhInna && foundInnaNoun == null -> {
                posCase = GrammaticalCase.MANSOOB
                role = "اسم إنّ منصوب"
                irabSign = if (endsWithYaaNoon) "الياء لأنه جمع مذكر سالم / مثنى" else if (endsWithAlifTa) "الكسرة نيابة عن الفتحة لأنه جمع مؤنث سالم" else "الفتحة الظاهرة على آخره"
                fullIrabText = "اسم (إنّ) منصوب وعلامة نصبه $irabSign."
                ruleExplanation = "إن وأخواتها تنصب الاسم وترفع الخبر. وينصب جمع المؤنث السالم بالكسرة نيابة عن الفتحة."
            }

            // 3. جاء بعد اسم إن -> خبر إن مرفوع
            foundInnaNoun != null && prevWord?.grammaticalRole?.contains("اسم إن") == true -> {
                posCase = GrammaticalCase.MARFOO
                role = "خبر إنّ مرفوع"
                irabSign = if (endsWithPluralWawNoon) "الواو لأنه جمع مذكر سالم" else if (endsWithDualAlifNoon) "الألف لأنه مثنى" else "الضمة الظاهرة على آخره"
                fullIrabText = "خبر (إنّ) مرفوع وعلامة رفعه $irabSign وهو متمم لمعنى الجملة."
                ruleExplanation = "خبر إن وأخواتها يكون مرفوعاً دائماً بالضمة، أو بالواو في جمع المذكر السالم، أو بالألف في المثنى."
            }

            // 4. مسبوق بـ كان وأخواتها ولم يظهر اسمها بعد -> اسم كان مرفوع
            prevWasNasikhKana && foundKanaNoun == null -> {
                posCase = GrammaticalCase.MARFOO
                role = "اسم كانَ مرفوع"
                irabSign = if (endsWithPluralWawNoon) "الواو لأنه جمع مذكر سالم" else if (endsWithDualAlifNoon) "الألف لأنه مثنى" else "الضمة الظاهرة على آخره"
                fullIrabText = "اسم (كان) مرفوع وعلامة رفعه $irabSign."
                ruleExplanation = "كان وأخواتها ترفع الاسم وتنصب الخبر."
            }

            // 5. بعد اسم كان -> خبر كان منصوب
            foundKanaNoun != null && prevWord?.grammaticalRole?.contains("اسم كان") == true -> {
                posCase = GrammaticalCase.MANSOOB
                role = "خبر كانَ منصوب"
                irabSign = if (endsWithYaaNoon) "الياء لأنه جمع مذكر سالم / مثنى" else if (endsWithAlifTa) "الكسرة نيابة عن الفتحة" else "الفتحة الظاهرة على آخره"
                fullIrabText = "خبر (كان) منصوب وعلامة نصبه $irabSign."
                ruleExplanation = "خبر كان وأخواتها يكون منصوباً دائماً."
            }

            // 6. جملة فعلية: جاء بعد الفعل ولم يعين فاعل بعد
            foundVerb != null && foundSubject == null -> {
                posCase = GrammaticalCase.MARFOO
                role = "فاعل مرفوع"
                irabSign = if (endsWithPluralWawNoon) "الواو لأنه جمع مذكر سالم" else if (endsWithDualAlifNoon) "الألف لأنه مثنى" else "الضمة الظاهرة على آخره"
                fullIrabText = "فاعل مرفوع وعلامة رفعه $irabSign، وهو من قام بالفعل أو اتصف به."
                ruleExplanation = "الفاعل اسم مرفوع يتقدمه فعل تام مبني للمعلوم ويدل على من فعل الفعل أو قام به."
            }

            // 7. جملة فعلية: بعد الفعل والفاعل -> مفعول به منصوب
            foundVerb != null && foundSubject != null -> {
                posCase = GrammaticalCase.MANSOOB
                role = "مفعول به منصوب"
                irabSign = if (endsWithYaaNoon) "الياء لأنه جمع مذكر سالم أو مثنى" else if (endsWithAlifTa) "الكسرة نيابة عن الفتحة لأنه جمع مؤنث سالم" else "الفتحة الظاهرة على آخره"
                fullIrabText = "مفعول به منصوب وعلامة نصبه $irabSign، وقع عليه فعل الفاعل."
                ruleExplanation = "المفعول به اسم منصوب يدل على من وقع عليه فعل الفاعل دون تغيير في هيئة الفعل."
            }

            // 8. مبتدأ: أول اسم في الجملة ولم تسبقه أداة ناسخة أو فعل
            isFirstWord -> {
                posCase = GrammaticalCase.MARFOO
                role = "مبتدأ مرفوع"
                irabSign = if (endsWithPluralWawNoon) "الواو لأنه جمع مذكر سالم" else if (endsWithDualAlifNoon) "الألف لأنه مثنى" else "الضمة الظاهرة على آخره"
                fullIrabText = "مبتدأ مرفوع وعلامة رفعه $irabSign، ابتدئ به الكلام."
                ruleExplanation = "المبتدأ اسم صريح أو مؤول مجرد من العوامل اللفظية غير الزائدة، وحكمه الرفع بالضمة أو ما ينوب عنها."
            }

            // 9. خبر: بعد المبتدأ
            foundMubtada != null && !hasDefiniteArticle && prevWord?.grammaticalRole?.contains("مبتدأ") == true -> {
                posCase = GrammaticalCase.MARFOO
                role = "خبر المبتدأ مرفوع"
                irabSign = if (endsWithPluralWawNoon) "الواو لأنه جمع مذكر سالم" else if (endsWithDualAlifNoon) "الألف لأنه مثنى" else "الضمة الظاهرة على آخره"
                fullIrabText = "خبر المبتدأ مرفوع وعلامة رفعه $irabSign، به تتم الفائدة مع المبتدأ."
                ruleExplanation = "الخبر هو الركن الثاني من أركان الجملة الاسمية الذي يتمم معناها ومحكوم عليه بالرفع دائماً."
            }

            // 10. نعت أو مضاف إليه افتراضي بحسب المطابقة في التعريف والتنكير
            else -> {
                val prevWasDefinite = prevWord?.cleanText?.startsWith("ال") == true || prevWord?.prefixes?.any { it.part == "الـ" } == true
                if (hasDefiniteArticle && prevWasDefinite) {
                    val prevCase = prevWord?.grammaticalCase ?: GrammaticalCase.MARFOO
                    posCase = prevCase
                    role = "نعت (صفة) تابع لما قبله"
                    irabSign = "يتبع المنعوت في " + prevCase.arabicName
                    fullIrabText = "نعت (صفة) تابع للمنعوت قبله في إعرابه وتعريفه، وعلامة إعرابه بحسب متبوعه."
                    ruleExplanation = "النعت يتبع المنعوت في أربعة أمور: الإعراب (رفعاً ونصباً وجراً)، والتعريف أو التنكير، والإفراد أو التثنية أو الجمع، والتذكير أو التأنيث."
                } else if (hasDefiniteArticle && !prevWasDefinite && prevWord?.partOfSpeech == PartOfSpeech.ISM) {
                    posCase = GrammaticalCase.MAJROOR
                    role = "مضاف إليه مجرور"
                    irabSign = if (endsWithYaaNoon) "الياء لأنه جمع أو مثنى" else "الكسرة الظاهرة على آخره"
                    fullIrabText = "مضاف إليه مجرور وعلامة جره $irabSign، أضاف معنى التحديد والتخصيص لما قبله."
                    ruleExplanation = "المضاف إليه اسم يُنسب إليه اسم سابق عليه، وحكمه الجر دائماً بالإضافة."
                } else {
                    posCase = GrammaticalCase.MARFOO
                    role = "اسم معرب (بحسب موقعه)"
                    irabSign = if (endsWithPluralWawNoon) "الواو" else "الضمة الظاهرة"
                    fullIrabText = "اسم معرب مرفوع وعلامة رفعه $irabSign."
                    ruleExplanation = "الاسم في اللغة العربية يعرب بحسب موقعه من الجملة والعوامل الداخلة عليه."
                }
            }
        }

        // تشكيل الكلمة للنطق
        val vocalized = vocalizeNoun(clean, posCase, irabSign, hasDefiniteArticle)

        return ParsedWord(
            index = index,
            originalText = raw,
            cleanText = clean,
            vocalizedText = vocalized,
            root = root,
            partOfSpeech = PartOfSpeech.ISM,
            partOfSpeechDetail = if (hasDefiniteArticle) "اسم معرف بأل" else "اسم نكرة معرب",
            grammaticalRole = role,
            grammaticalCase = posCase,
            irabSign = irabSign,
            morphologyWeight = weight,
            prefixes = prefixes,
            suffixes = suffixes,
            fullIrab = fullIrabText,
            ruleExplanation = ruleExplanation
        )
    }

    /**
     * تجزئة الكلمة الصرفية وتحديد السوابق واللواحق
     */
    private fun decomposeWord(raw: String, clean: String): WordDecomposition {
        val prefixes = mutableListOf<AffixBreakdown>()
        val suffixes = mutableListOf<AffixBreakdown>()
        var base = clean

        // فحص السوابق (و، ف، ب، ك، ل، س، الـ)
        if (base.startsWith("و") && base.length > 2 && !base.startsWith("ورد") && !base.startsWith("وقى")) {
            prefixes.add(AffixBreakdown("و", "سابقة", "واو العطف / الاستئناف", "حرف عطف مبني على الفتح لا محل له من الإعراب"))
            base = base.substring(1)
        } else if (base.startsWith("ف") && base.length > 2 && !base.startsWith("فعل") && !base.startsWith("فرح")) {
            prefixes.add(AffixBreakdown("ف", "سابقة", "فاء العطف / السببية", "حرف مبني على الفتح لا محل له من الإعراب"))
            base = base.substring(1)
        }

        if (base.startsWith("س") && base.length > 3 && (base.startsWith("سي") || base.startsWith("ست") || base.startsWith("سن") || base.startsWith("سأ"))) {
            prefixes.add(AffixBreakdown("سـ", "سابقة", "سين الاستقبال والتنفيس", "حرف تنفيس مبني على الفتح لا محل له من الإعراب"))
            base = base.substring(1)
        }

        if (base.startsWith("ب") && base.length > 2 && (base.startsWith("بال") || base.startsWith("بـ"))) {
            prefixes.add(AffixBreakdown("بـ", "سابقة", "باء الجر", "حرف جر مبني على الكسر لا محل له من الإعراب"))
            base = base.substring(1)
        } else if (base.startsWith("ل") && base.length > 2 && (base.startsWith("لل") || base.startsWith("لت"))) {
            prefixes.add(AffixBreakdown("لـ", "سابقة", "لام الجر أو التعليل", "حرف مبني على الكسر أو الفتح"))
            base = base.substring(1)
        } else if (base.startsWith("ك") && base.length > 2 && base.startsWith("كال")) {
            prefixes.add(AffixBreakdown("كـ", "سابقة", "كاف التشبيه والجر", "حرف جر وتشبيه مبني على الفتح"))
            base = base.substring(1)
        }

        if (base.startsWith("ال") && base.length > 2) {
            prefixes.add(AffixBreakdown("الـ", "أداة تعريف", "أل التعريفية", "أداة تعريف مبنية على السكون لا محل لها من الإعراب"))
            base = base.substring(2)
        }

        // فحص اللواحق والضمائر المتصلة في نهاية الكلمة
        if (base.endsWith("كما") && base.length > 4) {
            suffixes.add(AffixBreakdown("ـكما", "ضمير متصل", "كاف الخطاب للمثنى", "ضمير متصل مبني في محل جر مضاف إليه أو نصب مفعول به"))
            base = base.dropLast(3)
        } else if (base.endsWith("كم") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـكم", "ضمير متصل", "كاف الخطاب لجمع المذكر", "ضمير متصل مبني في محل جر مضاف إليه أو نصب مفعول به"))
            base = base.dropLast(2)
        } else if (base.endsWith("هم") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـهم", "ضمير متصل", "هاء الغيبة لجمع المذكر", "ضمير متصل مبني في محل جر مضاف إليه أو نصب مفعول به"))
            base = base.dropLast(2)
        } else if (base.endsWith("هن") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـهن", "ضمير متصل", "هاء الغيبة لجمع المؤنث", "ضمير متصل مبني في محل جر مضاف إليه أو نصب مفعول به"))
            base = base.dropLast(2)
        } else if (base.endsWith("ها") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـها", "ضمير متصل", "هاء الغيبة للمؤنث", "ضمير متصل مبني على السكون في محل جر مضاف إليه أو نصب مفعول به"))
            base = base.dropLast(2)
        } else if (base.endsWith("نا") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـنا", "ضمير متصل", "نا المتكلمين", "ضمير متصل مبني على السكون في محل رفع فاعل أو نصب مفعول به أو جر"))
            base = base.dropLast(2)
        } else if (base.endsWith("وا") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـوا", "ضمير متصل", "واو الجماعة", "ضمير متصل مبني على السكون في محل رفع فاعل والألف فارقة"))
            base = base.dropLast(2)
        } else if (base.endsWith("ون") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـون", "علامة إعراب", "واو الجمع والنون", "الواو علامة رفع جمع المذكر السالم والنون عوض عن التنوين"))
        } else if (base.endsWith("ين") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـين", "علامة إعراب", "ياء الجمع أو التثنية والنون", "الياء علامة نصب أو جر والنون عوض عن التنوين"))
        } else if (base.endsWith("ان") && base.length > 3) {
            suffixes.add(AffixBreakdown("ـان", "علامة إعراب", "ألف التثنية والنون", "الألف علامة رفع المثنى والنون عوض عن التنوين"))
        }

        return WordDecomposition(baseWord = base, prefixes = prefixes, suffixes = suffixes)
    }

    private data class WordDecomposition(
        val baseWord: String,
        val prefixes: List<AffixBreakdown>,
        val suffixes: List<AffixBreakdown>
    )

    /**
     * استخراج الجذر الثلاثي التقريبي
     */
    private fun extractRoot(word: String): String {
        var clean = word.replace(Regex("[أإآ]"), "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
        if (clean.length == 3) {
            return "${clean[0]}-${clean[1]}-${clean[2]}"
        }
        if (clean.length < 3) {
            return clean
        }

        // إزالة حروف الزيادة (س أ ل ت م و ن ي هـ ا)
        val candidate = clean.filter { it in "كتبعلمقردخلخرجسمعنظرضربفهمشربصبرحمدنصر" }
        return if (candidate.length >= 3) {
            "${candidate[0]}-${candidate[1]}-${candidate[2]}"
        } else {
            "${clean.first()}-${clean[clean.length / 2]}-${clean.last()}"
        }
    }

    /**
     * استنتاج الميزان الصرفي
     */
    private fun deduceWeight(word: String, root: String, isAmr: Boolean): String {
        val len = word.length
        return when {
            isAmr -> "افْعُلْ"
            word.startsWith("است") -> "اسْتَفْعَلَ"
            word.startsWith("مت") || word.startsWith("يت") -> "يَتَفَاعَلُ"
            word.startsWith("م") && len == 5 -> "مَفْعُول"
            word.startsWith("م") && len == 4 -> "مَفْعَل"
            word.startsWith("ت") && len == 5 -> "تَفْعِيل"
            len == 4 && word[1] == 'ا' -> "فَاعِل"
            len == 4 && word[2] == 'ي' -> "فَعِيل"
            len == 4 && word[2] == 'و' -> "فَعُول"
            len == 3 -> "فَعَلَ"
            else -> "فِعَال / مُفَعَّل"
        }
    }

    private fun isLikelyVerb(clean: String, base: String): Boolean {
        if (clean.length in 3..4 && !clean.contains("ة") && !clean.startsWith("ال")) {
            val commonVerbs = setOf(
                "قال", "كان", "كتب", "ذهب", "قرأ", "علم", "فهم", "شرب", "أكل", "جلس", "قام",
                "صام", "صلى", "سأل", "أمر", "نهى", "دعا", "هدى", "رمى", "وجد", "وعد", "عمل",
                "درس", "حضر", "خرج", "دخل", "نصر", "فتح", "حكم", "سمع", "شهد", "كفر", "آمن",
                "أكرم", "أحسن", "أصلح", "أرسل", "أنزل", "اجتهد", "استغفر", "تعلم", "تكلم", "شارك"
            )
            if (commonVerbs.contains(clean) || commonVerbs.contains(base)) return true
        }
        return false
    }

    private fun hasAlifLamPrefix(text: String): Boolean {
        return text.startsWith("ال")
    }

    private fun vocalizeWord(raw: String, clean: String, pos: PartOfSpeech): String {
        if (raw.any { it in '\u064B'..'\u0652' }) return raw
        return when (clean) {
            "في" -> "فِي"
            "من" -> "مِنْ"
            "إلى" -> "إِلَى"
            "على" -> "عَلَى"
            "عن" -> "عَنْ"
            "حتى" -> "حَتَّى"
            "ثم" -> "ثُمَّ"
            "إن" -> "إِنَّ"
            "أن" -> "أَنَّ"
            "كان" -> "كَانَ"
            "ليس" -> "لَيْسَ"
            "ما" -> "مَا"
            "لا" -> "لَا"
            "لم" -> "لَمْ"
            "لن" -> "لَنْ"
            "كي" -> "كَيْ"
            else -> clean
        }
    }

    private fun vocalizeVerb(clean: String, type: String): String {
        return when (type) {
            "ماض" -> if (clean.length == 3) "${clean[0]}َ${clean[1]}َ${clean[2]}َ" else clean
            "مضارع" -> if (clean.length == 4) "${clean[0]}َ${clean[1]}ْ${clean[2]}ُ${clean[3]}ُ" else clean
            "امر" -> if (clean.startsWith("ا")) "ا${clean[1]}ْ${clean[2]}ُ${clean.last()}ْ" else clean
            else -> clean
        }
    }

    private fun vocalizeNoun(clean: String, caseVal: GrammaticalCase, sign: String, hasAl: Boolean): String {
        val lastVowel = when (caseVal) {
            GrammaticalCase.MARFOO -> if (hasAl) "ُ" else "ٌ"
            GrammaticalCase.MANSOOB -> if (hasAl) "َ" else "اً"
            GrammaticalCase.MAJROOR -> if (hasAl) "ِ" else "ٍ"
            else -> ""
        }
        return clean + lastVowel
    }
}
