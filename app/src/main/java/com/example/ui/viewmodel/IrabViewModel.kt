package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteEntity
import com.example.data.db.SearchHistoryEntity
import com.example.data.repository.GrammarRepository
import com.example.parser.ArabicGrammarEncyclopedia
import com.example.parser.ArabicMorphologyEngine
import com.example.parser.GrammarQuizEngine
import com.example.parser.GrammarTopic
import com.example.parser.ParsedSentence
import com.example.parser.ParsedWord
import com.example.ui.tts.ArabicTtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IrabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GrammarRepository
    val ttsManager: ArabicTtsManager

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GrammarRepository(db.favoriteDao(), db.searchHistoryDao())
        ttsManager = ArabicTtsManager(application)
    }

    // شريط البحث والإدخال
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // نتيجة الإعراب
    private val _parsedResult = MutableStateFlow<ParsedSentence?>(null)
    val parsedResult: StateFlow<ParsedSentence?> = _parsedResult.asStateFlow()

    // الكلمة المختارة داخل الجملة لعرض تفاصيلها المحددة
    private val _selectedWordIndex = MutableStateFlow(0)
    val selectedWordIndex: StateFlow<Int> = _selectedWordIndex.asStateFlow()

    // المفضلة وسجل البحث
    val favoritesList: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<SearchHistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // هل النتيجة الحالية محفوظة في المفضلة؟
    private val _isCurrentFavorite = MutableStateFlow(false)
    val isCurrentFavorite: StateFlow<Boolean> = _isCurrentFavorite.asStateFlow()

    // موسوعة القواعد النحوية
    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _grammarSearch = MutableStateFlow("")
    val grammarSearch: StateFlow<String> = _grammarSearch.asStateFlow()

    val filteredTopics: StateFlow<List<GrammarTopic>> = combine(
        _selectedCategory,
        _grammarSearch
    ) { category, search ->
        ArabicGrammarEncyclopedia.TOPICS.filter { topic ->
            val matchesCategory = category == "الكل" || topic.category == category
            val matchesSearch = search.isBlank() ||
                    topic.title.contains(search, ignoreCase = true) ||
                    topic.summary.contains(search, ignoreCase = true) ||
                    topic.detailedContent.contains(search, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArabicGrammarEncyclopedia.TOPICS)

    private val _selectedTopic = MutableStateFlow<GrammarTopic?>(null)
    val selectedTopic: StateFlow<GrammarTopic?> = _selectedTopic.asStateFlow()

    // اختبار النحو
    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    init {
        // البدء بمثال إعرابي مشهور لتجربة مباشرة ممتعة
        parseText("العِلْمُ نُورٌ")
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun insertDiacritic(symbol: String) {
        _searchQuery.value = _searchQuery.value + symbol
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _parsedResult.value = null
        _isCurrentFavorite.value = false
    }

    fun parseText(text: String = _searchQuery.value) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _searchQuery.value = trimmed
        val result = ArabicMorphologyEngine.parse(trimmed)
        _parsedResult.value = result
        _selectedWordIndex.value = 0

        viewModelScope.launch {
            repository.addSearchHistory(trimmed)
            checkFavoriteStatus(trimmed)
        }
    }

    fun selectWord(index: Int) {
        _selectedWordIndex.value = index
    }

    private suspend fun checkFavoriteStatus(query: String) {
        _isCurrentFavorite.value = favoritesList.value.any { it.query == query }
    }

    fun toggleFavorite() {
        val current = _parsedResult.value ?: return
        val currentQuery = current.rawText

        viewModelScope.launch {
            if (_isCurrentFavorite.value) {
                repository.removeFavoriteByQuery(currentQuery)
                _isCurrentFavorite.value = false
            } else {
                val primaryWord = current.words.getOrNull(_selectedWordIndex.value) ?: current.words.firstOrNull()
                val entity = FavoriteEntity(
                    query = currentQuery,
                    vocalizedText = current.vocalizedSentence,
                    partOfSpeech = primaryWord?.partOfSpeech?.arabicName ?: "جملة",
                    grammaticalRole = primaryWord?.grammaticalRole ?: current.sentenceType,
                    grammaticalCase = primaryWord?.grammaticalCase?.arabicName ?: "",
                    summaryIrab = current.structureSummary,
                    fullIrab = primaryWord?.fullIrab ?: current.sentenceIrabNote,
                    root = primaryWord?.root ?: "",
                    notes = ""
                )
                repository.addFavorite(entity)
                _isCurrentFavorite.value = true
            }
        }
    }

    fun updateFavoriteNotes(id: Long, notes: String) {
        viewModelScope.launch {
            val fav = favoritesList.value.find { it.id == id }
            if (fav != null) {
                repository.updateFavorite(fav.copy(notes = notes))
            }
        }
    }

    fun deleteFavorite(id: Long) {
        viewModelScope.launch {
            repository.removeFavoriteById(id)
            _parsedResult.value?.let { checkFavoriteStatus(it.rawText) }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // موسوعة القواعد
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun onGrammarSearchChange(search: String) {
        _grammarSearch.value = search
    }

    fun selectTopic(topic: GrammarTopic?) {
        _selectedTopic.value = topic
    }

    // الاختبار
    fun selectQuizOption(index: Int) {
        if (_isAnswerSubmitted.value) return
        _selectedOption.value = index
        _isAnswerSubmitted.value = true
        val currentQ = GrammarQuizEngine.QUESTIONS.getOrNull(_currentQuestionIndex.value)
        if (currentQ != null && index == currentQ.correctIndex) {
            _quizScore.value += 1
        }
    }

    fun nextQuizQuestion() {
        if (_currentQuestionIndex.value < GrammarQuizEngine.QUESTIONS.size - 1) {
            _currentQuestionIndex.value += 1
            _selectedOption.value = null
            _isAnswerSubmitted.value = false
        }
    }

    fun restartQuiz() {
        _currentQuestionIndex.value = 0
        _quizScore.value = 0
        _selectedOption.value = null
        _isAnswerSubmitted.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
