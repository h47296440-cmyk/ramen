package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val id: Int = 1,
    val money: Int = 75000, // Starts with typical initial capital
    val popularity: Int = 15, // Starts at 15 fans
    val day: Int = 1,
    val seatingCapacity: Int = 4, // 4 seats initially
    val boilingSkillLvl: Int = 1, // Soup boiling level
    val decorLvl: Int = 1, // Vibe level
    val marketingLvl: Int = 1, // Leaflets/Ads
    val unlockedIngredientsStr: String = "醤油,塩,中細麺,極細麺,ネギ,メンマ", // Starting ingredients
    val currentRecipeId: Int = -1 // ID of the currently featured ramen recipe
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val broth: String, // 醤油, 塩, 豚骨, 味噌
    val noodles: String, // 極細麺, 中細麺, 太麺
    val toppingsStr: String, // Comma-separated list
    val costPrice: Int,
    val sellingPrice: Int,
    val tasteScore: Int, // Calculated quality index
    val complexity: Int // How hard to make
) {
    fun getToppingsList(): List<String> {
        if (toppingsStr.isEmpty()) return emptyList()
        return toppingsStr.split(",").filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "reviews")
data class CustomerReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: Int,
    val customerType: String, // サラリーマン, 女子高生, 学生, ラーメンマニア, シニア
    val recipeName: String,
    val rrating: Float, // 1 to 5 stars
    val comment: String,
    val moneySpent: Int,
    val timestamp: Long = System.currentTimeMillis()
)
