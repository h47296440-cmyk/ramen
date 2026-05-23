package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(private val gameDao: GameDao) {

    val gameStateFlow: Flow<GameState?> = gameDao.getGameStateFlow()
    val allRecipesFlow: Flow<List<Recipe>> = gameDao.getAllRecipesFlow()
    val allReviewsFlow: Flow<List<CustomerReview>> = gameDao.getAllReviewsFlow()

    suspend fun getGameStateOnce(): GameState? = withContext(Dispatchers.IO) {
        gameDao.getGameStateOnce()
    }

    suspend fun initializeStateIfNeeded() = withContext(Dispatchers.IO) {
        if (gameDao.getGameStateOnce() == null) {
            val defaultState = GameState()
            gameDao.insertOrUpdateState(defaultState)
            
            // Create a default classical Ramen recipe for the player!
            val defaultRecipe = Recipe(
                name = "昔ながらの醤油ラーメン",
                broth = "醤油",
                noodles = "中細麺",
                toppingsStr = "ネギ,メンマ",
                costPrice = 330, // 200 (broth) + 80 (noodle) + 30 (onion) + 50 (menma)
                sellingPrice = 750,
                tasteScore = 65,
                complexity = 1
            )
            val recipeId = gameDao.insertRecipe(defaultRecipe)
            // Associate current recipe
            gameDao.insertOrUpdateState(defaultState.copy(currentRecipeId = recipeId.toInt()))
        }
    }

    suspend fun saveGameState(state: GameState) = withContext(Dispatchers.IO) {
        gameDao.insertOrUpdateState(state)
    }

    suspend fun getRecipeById(id: Int): Recipe? = withContext(Dispatchers.IO) {
        gameDao.getRecipeById(id)
    }

    suspend fun addRecipe(recipe: Recipe): Int = withContext(Dispatchers.IO) {
        gameDao.insertRecipe(recipe).toInt()
    }

    suspend fun updateRecipe(recipe: Recipe) = withContext(Dispatchers.IO) {
        gameDao.updateRecipe(recipe)
    }

    suspend fun deleteRecipe(id: Int) = withContext(Dispatchers.IO) {
        gameDao.deleteRecipeById(id)
    }

    suspend fun addReview(review: CustomerReview) = withContext(Dispatchers.IO) {
        gameDao.insertReview(review)
        gameDao.trimReviews() // Auto-trim history size
    }

    suspend fun getAllRecipesOnce(): List<Recipe> = withContext(Dispatchers.IO) {
        gameDao.getAllRecipesOnce()
    }
}
