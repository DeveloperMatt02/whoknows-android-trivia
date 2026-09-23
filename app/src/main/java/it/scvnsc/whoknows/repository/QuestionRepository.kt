package it.scvnsc.whoknows.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.scvnsc.whoknows.data.dao.QuestionDAO
import it.scvnsc.whoknows.data.model.Question
import it.scvnsc.whoknows.data.network.ApiService
import it.scvnsc.whoknows.data.network.NetworkResult
import it.scvnsc.whoknows.data.network.QuestionResponse
import it.scvnsc.whoknows.data.network.TokenResponse
import it.scvnsc.whoknows.utils.CategoryManager
import it.scvnsc.whoknows.utils.QuestionDeserializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QuestionRepository(private val questionDAO: QuestionDAO) {
    //Si occupa dell'interazione tra la tabella del DB questions e ViewModel del game

    private var SESSION_TOKEN = ""

    companion object {
        //Numero arbitrario (costante) di domande da prendere dall'API
        private const val AMOUNT = 1

        //Response code dell'API OpenTDB (https://opentdb.com/api_config.php)
        private const val RESPONSE_SUCCESS = 0
        private const val RESPONSE_TOKEN_EMPTY = 4
    }

    //Si occupa anche di fare la chiamata API per recuperare le domande
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Question::class.java, QuestionDeserializer())
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://opentdb.com/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiService = retrofit.create(ApiService::class.java)


    suspend fun setupInteractionWithAPI(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        when (val categoriesResult = buildCategoryManager()) {
            is NetworkResult.Success -> Unit
            is NetworkResult.Error -> return@withContext categoriesResult
        }
        when (val tokenResponse = getSessionToken()) {
            is NetworkResult.Success -> SESSION_TOKEN = tokenResponse.data.token
            is NetworkResult.Error -> return@withContext tokenResponse
        }
        NetworkResult.Success(Unit)
    }

    // Step 1: Recuperare le categorie per popolare il CategoryManager (HashMap che collega categoryName e categoryID)
    private suspend fun buildCategoryManager(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val categories = apiService.getCategories()
            CategoryManager.buildCategoriesMap(categories.trivia_categories)
            Log.d("QuestionRepository", "Categories built: ${CategoryManager.categories}")
            NetworkResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("QuestionRepository", "Error building categories map: $e")
            NetworkResult.Error(e)
        }
    }

    // Step 2: Ottieni il Session Token per ottenere risposte sempre diverse dall'API
    private suspend fun getSessionToken(): NetworkResult<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            NetworkResult.Success(apiService.getToken())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("QuestionRepository", "Error getting session token: $e")
            NetworkResult.Error(e)
        }
    }

    suspend fun resetSessionToken(): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        if (SESSION_TOKEN.isEmpty()) return@withContext NetworkResult.Success(Unit)
        try {
            apiService.resetToken(SESSION_TOKEN)
            Log.d("QuestionRepository", "Session token reset: $SESSION_TOKEN")
            NetworkResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("QuestionRepository", "Error resetting session token: $e")
            NetworkResult.Error(e)
        }
    }

    //Step 3: Recupera una domanda della categoria e della difficolta' scelte ogni volta che l'utente clicka Play o risponde a una domanda.
    //Gli errori (rete, API, database) vengono restituiti come NetworkResult.Error e non propagati come eccezioni,
    //cosi' il ViewModel puo' gestirli (attendere la connessione e riprovare) senza far crashare l'app.
    suspend fun retrieveNewQuestion(
        categoryName: String,
        difficulty: String,
        allowTokenReset: Boolean = true
    ): NetworkResult<Question> = withContext(Dispatchers.IO) {
        try {
            //Prendo le nuove domande dall'API
            var categoryID: String? = ""
            if (categoryName != "") {
                categoryID = CategoryManager.categories[categoryName].toString()
            }

            val questionResponse: QuestionResponse = apiService.getQuestions(
                AMOUNT,
                categoryID,
                difficulty,
                SESSION_TOKEN
            )

            //Response Code = 4 -> Token Empty, non ci sono altre nuove domande disponibili: resetto il token e rieseguo la query (una sola volta)
            if (questionResponse.response_code == RESPONSE_TOKEN_EMPTY && allowTokenReset) {
                resetSessionToken()
                return@withContext retrieveNewQuestion(categoryName, difficulty, allowTokenReset = false)
            }

            //Qualsiasi altro codice diverso da 0 (es. 1 = nessun risultato, 5 = rate limit) e' un errore
            if (questionResponse.response_code != RESPONSE_SUCCESS || questionResponse.results.isEmpty()) {
                return@withContext NetworkResult.Error(
                    IllegalStateException("OpenTDB returned response_code ${questionResponse.response_code}")
                )
            }

            val newFetchedQuestion = questionResponse.results[0]
            val newQuestionID: Long = questionDAO.insert(newFetchedQuestion)
            newFetchedQuestion.id = newQuestionID

            NetworkResult.Success(newFetchedQuestion)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            Log.e("Database", "Error inserting new question in database: $e")
            NetworkResult.Error(e)
        } catch (e: Exception) {
            Log.e("WhoKnows", "Error retrieving new question: $e")
            NetworkResult.Error(e)
        }
    }

    suspend fun updateLastQuestion(questionID: Long, givenAnswer: String) {
        try {
            questionDAO.updateLastQuestion(questionID.toInt(), givenAnswer)
        } catch (e: SQLiteException) {
            Log.e("Database", "Error updating last question in database: $e")
            throw e
        }
    }

    suspend fun getQuestionsByIDs(questionsIDs: List<Int>): List<Question> {
        try {
            return questionDAO.getQuestionsByIDs(questionsIDs)
        } catch (e: SQLiteException) {
            Log.e("Database", "Error retrieving questions by IDs", e)
            throw e
        }
    }
}

