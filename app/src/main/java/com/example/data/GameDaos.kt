package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // GameState singleton
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameState?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameStateOnce(): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateState(state: GameState)

    // Recipes
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipesFlow(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    suspend fun getAllRecipesOnce(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: Int): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)

    // Reviews
    @Query("SELECT * FROM reviews ORDER BY id DESC")
    fun getAllReviewsFlow(): Flow<List<CustomerReview>>

    @Query("SELECT * FROM reviews WHERE day = :day ORDER BY id DESC")
    suspend fun getReviewsForDay(day: Int): List<CustomerReview>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CustomerReview)

    @Query("DELETE FROM reviews WHERE id NOT IN (SELECT id FROM reviews ORDER BY id DESC LIMIT 150)")
    suspend fun trimReviews()
}
