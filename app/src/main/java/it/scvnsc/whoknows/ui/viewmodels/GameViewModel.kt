package it.scvnsc.whoknows.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import it.scvnsc.whoknows.R
import it.scvnsc.whoknows.data.db.DatabaseWK
import it.scvnsc.whoknows.data.model.Game
import it.scvnsc.whoknows.data.model.Question
import it.scvnsc.whoknows.data.network.NetworkResult
import it.scvnsc.whoknows.repository.GameQuestionRepository
import it.scvnsc.whoknows.repository.GameRepository
import it.scvnsc.whoknows.repository.QuestionRepository
import it.scvnsc.whoknows.services.NetworkMonitorService
import it.scvnsc.whoknows.utils.CategoryManager
import it.scvnsc.whoknows.utils.DifficultyType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val DEFAULT_CATEGORY = "Mixed"
    private val DEFAULT_DIFFICULTY = "Mixed"
    private val SCORE_EASY_DIFFICULTY = 1
    private val SCORE_MEDIUM_DIFFICULTY = 2
    private val SCORE_HARD_DIFFICULTY = 3
    private val WAIT_TIME = 500L
    private val STARTING_LIVES = 3
    private val RETRY_DELAY = 1000L

    fun getCategories(): List<String> {
        val availableCategories = mutableListOf("Mixed")
        availableCategories.addAll(CategoryManager.categories.keys)
        availableCategories.toList()
        return availableCategories
    }

    //Domande poste all'utente
    private var askedQuestions = mutableListOf<Question>()

    //Memorizza la risposta clickata dall'utente (serve per aggiornare l'interfaccia GameView con i colori verde se corretta e rosso se errata)
    private val _userAnswer = MutableLiveData<String>()
    val userAnswer: LiveData<String> get() = _userAnswer

    //Timer per nuova richiesta API
    private var apiTimerJob: Job? = null

    //SoundPool per riproduzione suono in base alla risposta selezionata
    private val soundPool: SoundPool
    private val correctSoundId: Int
    private val wrongSoundId: Int

    //Creazione MediaPlayer per riproduzione soundtrack
    private var soundtrackPlayer: MediaPlayer = MediaPlayer.create(getApplication(), R.raw.soundtrack_whoknows_long)

    //Shared Preferences per il suono (abilitato/disabilitato)
    private val sharedPreferences: SharedPreferences = getApplication<Application>().getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    //Timer di gioco
    private var gameTimer = 0

    private val _elapsedTime = MutableLiveData<String>()
    val elapsedTime: LiveData<String> = _elapsedTime

    private val _isGameTimerInterrupted = MutableLiveData(false)
    val isGameTimerInterrupted: LiveData<Boolean> = _isGameTimerInterrupted

    private val _lastGame = MutableLiveData<Game>()
    val lastGame: LiveData<Game> get() = _lastGame

    //Controlla se si sta giocando
    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    //Controlla se la partita e' finita (diverso da isPlaying perche' se non sta giocando la partita potrebbe non essere mai iniziata)
    private val _isGameOver = MutableLiveData(false)
    val isGameOver: LiveData<Boolean> get() = _isGameOver

    //Difficolta' selezionata dall'utente
    private val _selectedDifficulty = MutableLiveData(DEFAULT_DIFFICULTY)
    val selectedDifficulty: LiveData<String> get() = _selectedDifficulty

    //Categoria selezionata dall'utente
    private val _selectedCategory = MutableLiveData(DEFAULT_CATEGORY)
    val selectedCategory: LiveData<String> get() = _selectedCategory

    //Fa in modo che solo una risposta sia selezionabile
    private val _isAnswerSelected = MutableLiveData(false)
    val isAnswerSelected: LiveData<Boolean> get() = _isAnswerSelected

    //Punteggio della partita
    private val _score = MutableLiveData<Int>()
    val score: LiveData<Int> = _score

    //Controlla se il nuovo punteggio e' un record
    private val _isRecord = MutableLiveData(false)
    val isRecord: LiveData<Boolean> get() = _isRecord

    //Vite dell'utente
    private val _lives = MutableLiveData<Int>()
    val lives: LiveData<Int> = _lives

    private val _isApiSetupComplete = MutableLiveData(false)
    val isApiSetupComplete: LiveData<Boolean> get() = _isApiSetupComplete

    //Domanda posta all'utente e possibili risposte (in ordine casuale, altrimenti la risposta corretta sarebbe sempre la prima)
    private val _questionForUser = MutableLiveData<Question?>()
    val questionForUser: LiveData<Question?> get() = _questionForUser

    private val _shuffledAnswers = MutableLiveData<List<String>>()
    val shuffledAnswers: LiveData<List<String>> get() = _shuffledAnswers

    //Repositories
    private val questionRepository: QuestionRepository
    private val gameRepository: GameRepository
    private val gameQuestionRepository: GameQuestionRepository

    init {
        val questionDAO = DatabaseWK.getInstance(application).questionDAO()
        val gameDAO = DatabaseWK.getInstance(application).gameDAO()
        val gameQuestionDAO = DatabaseWK.getInstance(application).gameQuestionDAO()
        questionRepository = QuestionRepository(questionDAO)
        gameRepository = GameRepository(gameDAO)
        gameQuestionRepository = GameQuestionRepository(gameQuestionDAO)

        // Inizializza il SoundPool e i suoni per le risposte
        soundPool = SoundPool.Builder()
            .setMaxStreams(3) // Numero massimo di suoni simultanei
            .build()
        correctSoundId = soundPool.load(getApplication(), R.raw.correct_answer, 1)
        wrongSoundId = soundPool.load(getApplication(), R.raw.wrong_answer, 1)
    }

    private fun checkSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean("isSoundEnabled", true)
    }

    fun setDifficulty(difficulty: String) {
        _selectedDifficulty.postValue(difficulty)
    }

    fun setCategory(categoryName: String) {
        _selectedCategory.postValue(categoryName)
    }

    fun setIsPlaying(isPlaying: Boolean) {
        //_isPlaying.postValue(isPlaying)
        _isPlaying.value = isPlaying
    }

    fun setGameOver(isGameOver: Boolean) {
        _isGameOver.value = isGameOver
    }

    fun onStartClicked() {
        viewModelScope.launch {
            setGameOver(false)
            pauseTimer()
            startGame()
            setIsPlaying(true)
            resumeTimer()
        }
    }

    fun onAnswerClicked(givenAnswer: String) {
        _isAnswerSelected.value = true
        viewModelScope.launch {

            _userAnswer.postValue(givenAnswer)

            //imposto la risposta data dall'utente nella domanda corrente e aggiorno il database
            _questionForUser.value?.givenAnswer = givenAnswer

            questionRepository.updateLastQuestion(
                _questionForUser.value!!.id,
                givenAnswer
            )

            evaluateAnswer(givenAnswer)
        }
    }

    private suspend fun evaluateAnswer(givenAnswer: String) {
        //Risposta corretta -> Fetch della prossima domanda, aggiornamento dello score
        if (givenAnswer == questionForUser.value?.correct_answer) {
            //Aggiorno il punteggio e riproduco il suono di risposta corretta
            updateScore()
            playSound(correctSoundId)
            delay(WAIT_TIME)
            fetchNewQuestion()
        } else {
            //Risposta sbagliata -> -1 vita, prossima domanda
            //Risposta sbagliata &&  Fine partita, salvataggio del game nel db, stop del timer, mostrare new record notification se nuovo record
            _lives.value = _lives.value!! - 1
            Log.d("GameViewModel", "Current lives: ${_lives.value}")
            playSound(wrongSoundId)
            delay(WAIT_TIME)

            if (_lives.value == 0){
                //Salvataggio game nel DB
                Log.d("GameViewModel", "Game ended with score: ${_score.value}")
                val playedGame = Game(
                    _score.value!!,
                    _selectedDifficulty.value!!,
                    _selectedCategory.value!!,
                    _elapsedTime.value!!,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
                )
                //Controllo se il nuovo punteggio e' un record e aggiorno isRecord di conseguenza
                checkGameRecord(playedGame)

                //In ogni caso salvo la partita
                saveGameAndQuestions(playedGame, askedQuestions)

                setGameOver(true)
                stopSoundtrack()

                //salvo nella variabile lastGame l'ultima partita salvata
                _lastGame.value = gameRepository.getLastGame()
            }else{
                fetchNewQuestion()
            }
        }
        _isAnswerSelected.value = false
    }



    private suspend fun fetchNewQuestion() {

        //Fetcho la prossima domanda, se la richiesta API fallisce aspetto che torni la connessione e riproviamo
        _questionForUser.value = nextQuestionWithRetry()
        //Resetto la risposta data dall'utente e riattivo il timer per continuare a giocare
        _userAnswer.value = ""

        resumeTimer()
    }

    private fun playSound(answerSound: Int) {
        //Se il suono è abilitato riproduco il suono
        if (checkSoundEnabled()){
            soundPool.play(answerSound,1f,1f,0,0,1f)
        }
    }

    private suspend fun saveGameAndQuestions(
        playedGame: Game,
        askedQuestions: MutableList<Question>
    ) {
        val newGameID = gameRepository.saveGame(playedGame)
        playedGame.id = newGameID

        gameQuestionRepository.saveGameAndQuestions(playedGame, askedQuestions)
    }

    private suspend fun checkGameRecord(playedGame: Game) {
        val maxScore = gameRepository.getMaxScore() ?: 0
        val isNewRecord = playedGame.score!! > maxScore

        if (maxScore == 0) {
            Log.d("GameViewModel", "New record: ${playedGame.score}")
        } else {
            when (isNewRecord) {
                true -> Log.d("GameViewModel", "New record: ${playedGame.score}")
                false -> Log.d("GameViewModel", "No new record")
            }
        }

        _isRecord.value = isNewRecord
    }

    private suspend fun startGame() {
        //Se il setup dell'API era fallito (es. rete assente all'apertura) lo riprovo ora
        ensureApiSetup()

        //Resetto il timer di gioco e il token per le domande
        _elapsedTime.postValue("")
        questionRepository.resetSessionToken()

        //Resetto la lista delle domande poste all'utente
        askedQuestions.clear()

        //Imposto punteggio a 0
        _score.postValue(0)

        //Imposto le vite al valore di partenza
        _lives.postValue(STARTING_LIVES)

        //Fetcho la nuova domanda (se la richiesta API fallisce aspetto che torni la connessione e riproviamo)
        _questionForUser.value = nextQuestionWithRetry()

        //La partita e' stata abbandonata mentre si attendeva la connessione: non avvio timer e musica
        if (_questionForUser.value == null) return

        Log.d("GameViewModel", "Game started")
        Log.d("GameViewModel", "Current question: ${_questionForUser.value}")
        //Start timer della partita
        startTimer()
        startSoundtrack()
    }

    fun startSoundtrack() {
        if (checkSoundEnabled()){
            Log.d("GameViewModel", "Soundtrack started")
            soundtrackPlayer.isLooping = true
            soundtrackPlayer.start()
        }
    }

    fun stopSoundtrack() {
        if (soundtrackPlayer.isPlaying){
            soundtrackPlayer.stop()
            soundtrackPlayer.prepare()
        }
        Log.d("GameViewModel", "Soundtrack stopped")
    }

    //Funzione che converte la stringa "mixed" in "" in modo da far funzionare la richiesta all'API
    private fun convertMixed(text: String): String {
        if (text != "Mixed") {
            return text
        }
        return ""
    }

    //Riprova a ottenere una domanda finche' la partita e' in corso.
    //L'attesa usa delay() e quindi sospende la coroutine senza bloccare il main thread
    //(un while(true) attivo sul main thread impedirebbe anche l'aggiornamento di isOffline).
    private suspend fun nextQuestionWithRetry(): Question? {
        var question = nextQuestion()
        while (question == null && _isGameOver.value != true) {
            //Se siamo offline aspetto che torni la connessione
            while (NetworkMonitorService.isOffline.value == true && _isGameOver.value != true) {
                delay(RETRY_DELAY)
            }
            if (_isGameOver.value == true) break
            question = nextQuestion()
        }
        return question
    }

    //Funzione che ottiene la nuova domanda da presentare all'utente (l'API fornisce le domande in ordine casuale)
    private suspend fun nextQuestion(): Question? {

        //Se il timer per la prossima richiesta API non e' null, aspetto che finisca
        if (apiTimerJob != null) {
            Log.d("TimerJob", "API timer non è null")
            pauseTimer()
            apiTimerJob?.join()
            Log.d("TimerJob", "API timer joinato")
        }

        val result = questionRepository.retrieveNewQuestion(
            convertMixed(_selectedCategory.value.toString()),
            convertMixed(_selectedDifficulty.value.toString()).lowercase()
        )

        //avvio il timer per la prossima richiesta API dopo ogni tentativo, anche fallito,
        //per rispettare il rate limit di OpenTDB (1 richiesta ogni 5 secondi)
        apiCountdownTimer()

        val newQuestion: Question = when (result) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> {
                Log.e("GameViewModel", "Error: ${result.exception.message}")
                return null
            }
        }

        askedQuestions.add(newQuestion)
        _shuffledAnswers.value = shuffleAnswers(newQuestion)

        //riavvio il timer di gioco
        resumeTimer()

        return newQuestion
    }


    //Funzione che viene chiamata quando viene fatta una nuova richiesta API, setta il booleano a false e dopo 5 secondi lo setta di nuovo a true
    private fun apiCountdownTimer() {
        apiTimerJob = viewModelScope.launch {
            delay(5200L)
        }
    }

    //Funzione che mescola le possibili risposte alla domanda (altrimenti la risposta corretta sarebbe sempre la prima)
    private fun shuffleAnswers(question: Question): MutableList<String> {
        val corrAnswer = question.correct_answer
        val incAnswers = question.incorrect_answers.toMutableList()
        incAnswers.add(corrAnswer)
        incAnswers.shuffle()
        return incAnswers
    }

    //Inizializza il timer di gioco
    private fun startTimer() {

        _isGameOver.value = false //per forza così, se utilizzo postValue non si aggiorna il timer

        viewModelScope.launch {
            Log.d("GameViewModel", "Timer started")

            gameTimer = 0
            while (_isGameOver.value == false) {
                if (_isGameTimerInterrupted.value == false) {
                    val formattedTime =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            gameTimer / 60,
                            gameTimer % 60
                        )
                    _elapsedTime.postValue(formattedTime)
                    delay(1000L)
                    gameTimer++
                } else {
                    delay(1000)
                }
            }
        }
    }

    //Aggiorna il punteggio
    private fun updateScore() {

        val increment = when (_questionForUser.value?.difficulty) {
            DifficultyType.Easy.toString().lowercase() -> SCORE_EASY_DIFFICULTY
            DifficultyType.Medium.toString().lowercase() -> SCORE_MEDIUM_DIFFICULTY
            DifficultyType.Hard.toString().lowercase() -> SCORE_HARD_DIFFICULTY
            else -> 1
        }
        _score.postValue(_score.value!! + increment)
    }

    fun clearUserAnswer() {
        _userAnswer.value = ""
    }

    fun onQuitGameClicked() {
        viewModelScope.launch {
            quitGame()
        }
    }

    private suspend fun quitGame() {
        //imposto gameOver a true in modo da far comparire la schermata di gameOver
        setGameOver(true)
        stopSoundtrack()

        //imposto la risposta come non data
        _questionForUser.value?.givenAnswer = ""

        //Salvataggio game nel DB
        Log.d("GameViewModel", "Game ended with score: ${_score.value}")

        val playedGame = Game(
            _score.value!!,
            _selectedDifficulty.value!!,
            _selectedCategory.value!!,
            _elapsedTime.value!!,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
        )

        //Controllo se il nuovo punteggio e' un record e aggiorno isRecord di conseguenza
        checkGameRecord(playedGame)

        //In ogni caso salvo la partita
        saveGameAndQuestions(playedGame, askedQuestions)

        askedQuestions.clear()

        //salvo nella variabile lastGame l'ultima partita salvata
        _lastGame.value = gameRepository.getLastGame()

        //resetto il game timer
        resumeTimer()
    }

    fun pauseTimer() {
        _isGameTimerInterrupted.value = true
        Log.d("GameViewModel", "Timer paused")
        // Logica per fermare il timer
    }

    fun resumeTimer() {
        //_isGameTimerInterrupted.postValue(false)
        _isGameTimerInterrupted.value = false
        Log.d("GameViewModel", "Timer resumed")
        // Logica per riprendere il timer
    }

    fun setupAPI() {
        viewModelScope.launch {
            //provo a fare il setup dell'api all'avvio del programma, se fallisce loggo un errore
            //(senza rilanciare l'eccezione, che farebbe crashare l'app) e riprovo all'avvio della partita
            ensureApiSetup()
        }
    }

    private suspend fun ensureApiSetup(): Boolean {
        if (_isApiSetupComplete.value == true) return true
        Log.d("GameViewModel", "API setup started")
        return when (val result = questionRepository.setupInteractionWithAPI()) {
            is NetworkResult.Success -> {
                _isApiSetupComplete.value = true
                true
            }
            is NetworkResult.Error -> {
                Log.e("GameViewModel", "API setup error: ${result.exception.message}")
                false
            }
        }
    }

}