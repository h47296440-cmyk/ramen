package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// Represents an active customer in the shop eating or waiting
data class ActiveCustomer(
    val id: String = UUID.randomUUID().toString(),
    val seatIndex: Int,
    val type: String, // サラリーマン, 女子高生, 学生, マニア, シニア
    val patience: Int, // 1 to 10
    val status: String, // "注文中", "食事中", "完了"
    val progress: Float, // 0.0 to 1.0 (eating progress)
    val comment: String = "",
    val spend: Int = 0,
    val rating: Float = 5.0f
)

// Log messages displayed during simulation
data class SimLog(
    val id: String = UUID.randomUUID().toString(),
    val timeStr: String,
    val text: String,
    val iconType: String // "customer", "money", "upgrade", "info"
)

// Weather configuration of the day
enum class Weather(val label: String, val trafficMod: Float, val explanation: String) {
    SUNNY("晴れ ☀️", 1.0f, "通常の人通り。爽やかな一日の始まり！"),
    RAINY("雨 ☔", 0.75f, "通常より客足-25%。温かいラーメンの需要がアップ！"),
    HOT_SUMMER("猛暑 🥵", 0.70f, "客足-30%。しかしトッピング多めの刺激的なスープや、塩味を好む客が増えます。"),
    FREEZING("極寒 ❄️", 1.30f, "客足+30%！誰もがアツアツの味噌や豚骨ラーメンを求めてやってきます。")
}

class RamenViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application)
    private val repository = GameRepository(database.gameDao())

    // UI exposed states
    val gameState: StateFlow<GameState?> = repository.gameStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recipes: StateFlow<List<Recipe>> = repository.allRecipesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<CustomerReview>> = repository.allReviewsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active day simulation state
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _simHour = MutableStateFlow(9)
    val simHour: StateFlow<Int> = _simHour.asStateFlow()

    private val _simWeather = MutableStateFlow(Weather.SUNNY)
    val simWeather: StateFlow<Weather> = _simWeather.asStateFlow()

    private val _dailyEarnings = MutableStateFlow(0)
    val dailyEarnings: StateFlow<Int> = _dailyEarnings.asStateFlow()

    private val _customersServedCount = MutableStateFlow(0)
    val customersServedCount: StateFlow<Int> = _customersServedCount.asStateFlow()

    val activeCustomers = mutableStateListOf<ActiveCustomer>()
    val simLogs = mutableStateListOf<SimLog>()

    private var simJob: Job? = null
    private var isFastForward = false

    // Recipe crafting sandbox temporary states
    var sandboxRecipeName = "自慢の創作ラーメン"
    var sandboxBroth = "醤油"
    var sandboxNoodles = "中細麺"
    val sandboxToppings = mutableStateListOf<String>("ネギ")
    var sandboxSellingPrice = 800

    init {
        viewModelScope.launch {
            repository.initializeStateIfNeeded()
            resetSandboxToDefaults()
        }
    }

    fun resetSandboxToDefaults() {
        sandboxRecipeName = "自慢の特製ラーメン"
        sandboxBroth = "醤油"
        sandboxNoodles = "中細麺"
        sandboxToppings.clear()
        sandboxToppings.add("ネギ")
        sandboxSellingPrice = 800
    }

    // Helper to calculate score of temporary sandbox recipe
    fun calculateSandboxMetrics(): Pair<Int, Int> { // Taste score, Cost Price
        var baseCost = when (sandboxBroth) {
            "醤油" -> 200
            "塩" -> 180
            "豚骨" -> 250
            "味噌" -> 220
            else -> 150
        }

        val noodleCost = when (sandboxNoodles) {
            "極細麺" -> 80
            "中細麺" -> 80
            "太麺" -> 100
            else -> 80
        }

        var toppingCost = 0
        sandboxToppings.forEach { t ->
            toppingCost += when (t) {
                "ネギ" -> 30
                "メンマ" -> 50
                "チャーシュー" -> 150
                "味玉" -> 80
                "海苔" -> 40
                "ナルト" -> 30
                "背脂" -> 60
                "黒マー油" -> 80
                "プレミアム極厚叉焼" -> 350
                else -> 0
            }
        }

        val totalCost = baseCost + noodleCost + toppingCost

        // Calculate Taste Score
        // Base score has some values
        var tasteScore = when (sandboxBroth) {
            "醤油" -> 60
            "塩" -> 55
            "豚骨" -> 65
            "味噌" -> 62
            else -> 50
        }

        // Synergies & Matchings (Boost / Penalty)
        if (sandboxBroth == "豚骨" && sandboxNoodles == "極細麺") tasteScore += 12 // Legendary standard
        if (sandboxBroth == "豚骨" && sandboxNoodles == "太麺") tasteScore += 5
        if (sandboxBroth == "味噌" && sandboxNoodles == "太麺") tasteScore += 15 // Miso thick ramen synergy
        if (sandboxBroth == "塩" && sandboxNoodles == "極細麺") tasteScore += 8
        if (sandboxBroth == "醤油" && sandboxNoodles == "中細麺") tasteScore += 7

        // Penalties for matching mismatches
        if (sandboxBroth == "味噌" && sandboxNoodles == "極細麺") tasteScore -= 10 // Too heavy for fine noodles
        if (sandboxBroth == "塩" && sandboxNoodles == "太麺") tasteScore -= 10 // Broth too light for thick noodles

        // Toppings effects
        sandboxToppings.forEach { t ->
            tasteScore += when (t) {
                "ネギ" -> 4
                "メンマ" -> 6
                "チャーシュー" -> 12
                "味玉" -> 8
                "海苔" -> 5
                "ナルト" -> 4
                "背脂" -> 10
                "黒マー油" -> 14
                "プレミアム極厚叉焼" -> 28
                else -> 0
            }
        }

        // Synergies with specific toppings
        if (sandboxToppings.contains("背脂") && (sandboxBroth == "豚骨" || sandboxBroth == "味噌")) {
            tasteScore += 8 // oily richness boost
        }
        if (sandboxToppings.contains("黒マー油") && sandboxBroth == "豚骨") {
            tasteScore += 12 // Kumamoto style tonkotsu synergy!
        }
        if (sandboxToppings.contains("海苔") && sandboxBroth == "醤油") {
            tasteScore += 6 // Traditional kanto shoyu
        }

        // Adjust based on Boiling Skill Level (read from state)
        val skillMultiplier = 1.0 + ((gameState.value?.boilingSkillLvl ?: 1) - 1) * 0.08
        tasteScore = (tasteScore * skillMultiplier).toInt().coerceIn(10, 100)

        return Pair(tasteScore, totalCost)
    }

    // Save recipe to list
    fun saveCraftedRecipe(name: String, selectOnSave: Boolean = true) {
        viewModelScope.launch {
            val metrics = calculateSandboxMetrics()
            val score = metrics.first
            val cost = metrics.second

            val r = Recipe(
                name = name.ifBlank { "創作極めだれラーメン" },
                broth = sandboxBroth,
                noodles = sandboxNoodles,
                toppingsStr = sandboxToppings.joinToString(","),
                costPrice = cost,
                sellingPrice = sandboxSellingPrice,
                tasteScore = score,
                complexity = sandboxToppings.size + 1
            )

            val id = repository.addRecipe(r)
            if (selectOnSave) {
                selectRecipe(id)
            }
        }
    }

    fun deleteRecipe(id: Int) {
        viewModelScope.launch {
            repository.deleteRecipe(id)
            val state = gameState.value ?: return@launch
            if (state.currentRecipeId == id) {
                // Find another recipe if any
                val activeList = repository.getAllRecipesOnce()
                val nextRecipeId = activeList.firstOrNull { it.id != id }?.id ?: -1
                repository.saveGameState(state.copy(currentRecipeId = nextRecipeId))
            }
        }
    }

    // Select which recipe is being sold at the shop
    fun selectRecipe(id: Int) {
        viewModelScope.launch {
            val s = gameState.value ?: return@launch
            repository.saveGameState(s.copy(currentRecipeId = id))
        }
    }

    // Run Daily Simulation Engine
    fun toggleSpeed() {
        isFastForward = !isFastForward
    }

    fun startDayBusiness() {
        if (_isSimulating.value) return
        val state = gameState.value ?: return
        if (state.currentRecipeId == -1) {
            // Cannot open without a recipe
            return
        }

        _isSimulating.value = true
        _simHour.value = 9
        _dailyEarnings.value = 0
        _customersServedCount.value = 0
        activeCustomers.clear()
        simLogs.clear()
        isFastForward = false

        // Pick Daily weather randomly with equal options
        val weathers = Weather.values()
        _simWeather.value = weathers[Random.nextInt(weathers.size)]

        simJob = viewModelScope.launch {
            addSimLog("09:00", "暖簾（のれん）を掛けました！ラーメン屋、本日の営業スタートです！", "info")
            addSimLog("09:00", "本日の天気は【${_simWeather.value.label}】（${_simWeather.value.explanation}）", "info")

            val activeRecipe = repository.getRecipeById(state.currentRecipeId) ?: return@launch

            // 12 hour cycle: 9:00 to 21:00 (represented by ticking index 9 to 21)
            for (hour in 9..20) {
                _simHour.value = hour
                val timeStr = String.format("%02d:00", hour)

                // Customer entry density based on parameters
                // Base arrival rate calculation
                val baseRate = when {
                    hour in 12..13 -> 4.0f // Peak lunch
                    hour in 18..20 -> 3.5f // Peak dinner
                    else -> 1.5f // Regular hour
                }

                // Adjustments
                val popularityMod = 1.0f + (state.popularity / 100f)
                val marketingMod = 1.0f + (state.marketingLvl - 1) * 0.25f
                val weatherMod = _simWeather.value.trafficMod

                val finalArrivalCount = ((baseRate * popularityMod * marketingMod * weatherMod).toInt()).coerceAtLeast(1)

                // 1. First, complete existing customers if they are done
                progressActiveCustomers(timeStr, activeRecipe)

                // 2. Bring in new customers up to capacity
                spawnNewCustomers(finalArrivalCount, state.seatingCapacity, timeStr)

                // Render intermediate eating ticks to make it animated
                val gameLoopsInHour = if (isFastForward) 1 else 3
                for (step in 1..gameLoopsInHour) {
                    delay(if (isFastForward) 300L else 1200L)
                    simulateCustomerEatingSteps(timeStr, activeRecipe)
                }
            }

            // End of day (21:00)
            _simHour.value = 21
            while (activeCustomers.isNotEmpty()) {
                // Flush remaining customers
                progressActiveCustomers("21:00", activeRecipe)
                simulateCustomerEatingSteps("21:00", activeRecipe)
                delay(if (isFastForward) 200L else 800L)
            }

            // Compile Day Results & Auto Save
            finishDayResults(state, activeRecipe)
        }
    }

    private fun addSimLog(time: String, text: String, type: String) {
        simLogs.add(0, SimLog(timeStr = time, text = text, iconType = type))
        if (simLogs.size > 100) simLogs.removeLast()
    }

    private fun spawnNewCustomers(count: Int, capacity: Int, timeStr: String) {
        val currentOccupiedSeats = activeCustomers.size
        val availableSeats = (capacity - currentOccupiedSeats).coerceAtLeast(0)
        val arrivalCount = count.coerceAtMost(availableSeats)

        val customerTypes = listOf("サラリーマン", "学生", "女子高生", "ラーメンマニア", "シニア")

        // Seat layout: assign index 0 until capacity
        val takenIndices = activeCustomers.map { it.seatIndex }
        val freeIndices = (0 until capacity).filter { it !in takenIndices }.toMutableList()

        for (i in 0 until arrivalCount) {
            if (freeIndices.isEmpty()) break
            val seatIdx = freeIndices.removeAt(0)
            val customerType = customerTypes[Random.nextInt(customerTypes.size)]

            // Define customer expectations & patience
            val patience = Random.nextInt(6, 11) + (gameState.value?.decorLvl ?: 1)
            
            val comments = when (customerType) {
                "サラリーマン" -> "「サッと食べられて旨いラーメンはあるかな」"
                "学生" -> "「安くて腹いっぱいになるラーメンが一番っすよ！」"
                "女子高生" -> "「SNSで映える美味しいラーメンを探してます🌸」"
                "ラーメンマニア" -> "「この店のスープと麺の調和、確かめさせてもらおう」"
                "シニア" -> "「アッサリした体に優しい味が食べたいねぇ」"
                else -> "「いらっしゃい！」"
            }

            val guest = ActiveCustomer(
                seatIndex = seatIdx,
                type = customerType,
                patience = patience,
                status = "注文中",
                progress = 0.0f,
                comment = comments
            )
            activeCustomers.add(guest)
            addSimLog(timeStr, "【$customerType】が ${seatIdx + 1}番席に座りました。", "customer")
        }
    }

    private fun simulateCustomerEatingSteps(timeStr: String, recipe: Recipe) {
        val itemsToDelete = mutableListOf<ActiveCustomer>()
        
        for (i in 0 until activeCustomers.size) {
            val customer = activeCustomers[i]
            if (customer.status == "注文中") {
                // Switch order to eating
                val updated = customer.copy(status = "食事中", progress = 0.25f, comment = "ずずずっ...（食べている）")
                activeCustomers[i] = updated
            } else if (customer.status == "食事中") {
                val nextProgress = customer.progress + 0.35f
                if (nextProgress >= 1.0f) {
                    // Calculation of Customer Feedback and earnings
                    val (earnedPrice, satisfactionRating, commentText) = evaluateCustomerExperience(customer.type, recipe)
                    val finalized = customer.copy(
                        status = "完了",
                        progress = 1.0f,
                        comment = commentText,
                        spend = earnedPrice,
                        rating = satisfactionRating
                    )
                    activeCustomers[i] = finalized
                } else {
                    activeCustomers[i] = customer.copy(progress = nextProgress)
                }
            }
        }
    }

    private fun progressActiveCustomers(timeStr: String, recipe: Recipe) {
        // Collect money from completed customer actions
        val completed = activeCustomers.filter { it.status == "完了" }
        completed.forEach { guest ->
            _dailyEarnings.value += guest.spend
            _customersServedCount.value += 1
            
            // Log Review
            viewModelScope.launch {
                val r = CustomerReview(
                    day = gameState.value?.day ?: 1,
                    customerType = guest.type,
                    recipeName = recipe.name,
                    rrating = guest.rating,
                    comment = guest.comment,
                    moneySpent = guest.spend
                )
                repository.addReview(r)
            }

            val coinText = when {
                guest.rating >= 4.5f -> "【極大満足】${guest.comment} (+${guest.spend}円 ⭐️${guest.rating})"
                guest.rating >= 3.0f -> "【満足】${guest.comment} (+${guest.spend}円 ⭐️${guest.rating})"
                else -> "【不評】${guest.comment} (+${guest.spend}円 ⭐️${guest.rating})"
            }
            addSimLog(timeStr, "${guest.type}がお会計しました。$coinText", "money")
        }

        activeCustomers.removeAll(completed)
    }

    private fun evaluateCustomerExperience(type: String, recipe: Recipe): Triple<Int, Float, String> {
        var baseRating = 3.5f
        
        // 1. Evaluation based on custom flavor score
        val tasteDeviation = recipe.tasteScore / 100.0f // 0.1 to 1.0
        baseRating += (tasteDeviation - 0.5f) * 2.0f // quality adjustment (-1.0 to +1.0 star)

        // 2. Pricing satisfaction (Willingness to pay value)
        val limitPrice = when (type) {
            "サラリーマン" -> 950
            "学生" -> 800
            "女子高生" -> 1100
            "ラーメンマニア" -> 1450
            "シニア" -> 900
            else -> 900
        }

        var isDoubtfulPrice = false
        if (recipe.sellingPrice > limitPrice) {
            val penalty = ((recipe.sellingPrice - limitPrice) / 100) * 0.5f
            baseRating -= penalty
            isDoubtfulPrice = true
        } else if (recipe.sellingPrice < limitPrice - 200) {
            // Bargain satisfaction
            baseRating += 0.5f
        }

        // 3. Customer Category Specific Taste Matches
        var matchComment = ""
        when (type) {
            "サラリーマン" -> {
                if (recipe.broth == "豚骨" || recipe.broth == "醤油") {
                    baseRating += 0.5f
                    matchComment = "コク深いスープが最高！"
                }
                if (recipe.toppingsStr.contains("チャーシュー")) {
                    baseRating += 0.4f
                }
            }
            "学生" -> {
                if (recipe.broth == "豚骨" || recipe.broth == "味噌") {
                    baseRating += 0.6f
                    matchComment = "ガツンとくるパンチがきいてる！"
                } else if (recipe.broth == "塩") {
                    baseRating -= 0.8f
                    matchComment = "ちょっと物足りないっす。"
                }
                if (recipe.toppingsStr.contains("背脂")) {
                    baseRating += 0.5f
                    matchComment += " 背脂タップリで飯が進む！"
                }
            }
            "女子高生" -> {
                if (recipe.broth == "塩" || recipe.broth == "醤油") {
                    baseRating += 0.5f
                    matchComment = "あっさりしててヘルシー！"
                }
                if (recipe.toppingsStr.contains("背脂")) {
                    baseRating -= 0.8f
                    matchComment = "脂が多すぎてちょっとキツイな…"
                }
                if (recipe.toppingsStr.contains("味玉")) {
                    baseRating += 0.3f
                }
            }
            "ラーメンマニア" -> {
                // Otaku evaluates synergy strictly
                if (recipe.broth == "豚骨" && recipe.noodles == "極細麺" && recipe.toppingsStr.contains("黒マー油")) {
                    baseRating += 1.2f
                    matchComment = "熊本風の薫り高い仕上がりに感服した。"
                } else if (recipe.broth == "味噌" && recipe.noodles == "太麺") {
                    baseRating += 0.8f
                    matchComment = "王道の味噌太麺、実に見事な仕上がりだ。"
                } else if (recipe.broth == "塩" && recipe.toppingsStr.contains("背脂")) {
                    baseRating -= 0.9f
                    matchComment = "サッパリした塩に背脂は蛇足ではないか。"
                }
            }
            "シニア" -> {
                if (recipe.broth == "醤油" || recipe.broth == "塩") {
                    baseRating += 0.7f
                    matchComment = "これだよこれ、昔懐かしい優しい味わいだ。"
                } else if (recipe.broth == "豚骨") {
                    baseRating -= 0.8f
                    matchComment = "私には少し脂っこすぎるかねぇ。"
                }
            }
        }

        // Adjust for boiling pot quality bonus
        val decorLvl = gameState.value?.decorLvl ?: 1
        baseRating += (decorLvl - 1) * 0.1f

        val finalRating = baseRating.coerceIn(1.0f, 5.0f)

        // Construct reviews
        val textFeedback = when {
            finalRating >= 4.5f -> {
                if (matchComment.isNotEmpty()) "$matchComment「絶対にまた来ます！」"
                else "「最高に旨い一杯！スープまで完飲しました。」"
            }
            finalRating >= 3.5f -> {
                if (matchComment.isNotEmpty()) "$matchComment「普通に美味しくて満足です。」"
                else "「レベルの高いラーメン。また食べたいですね。」"
            }
            finalRating >= 2.5f -> {
                if (isDoubtfulPrice) "「味は悪くないが、価格設定が高すぎる。」"
                else "「普通のラーメン。際立つ特徴は無い。」"
            }
            else -> {
                if (isDoubtfulPrice) "「高くて不味い、最悪の一杯だ。」"
                else "「スープと具材の相性が最悪...もう来ないかな。」"
            }
        }

        return Triple(recipe.sellingPrice, finalRating, textFeedback)
    }

    private suspend fun finishDayResults(currentState: GameState, activeRecipe: Recipe) {
        val earnedMoney = _dailyEarnings.value
        val customersServed = _customersServedCount.value

        // Cost of materials
        val ingredientCostTotal = activeRecipe.costPrice * customersServed
        val netGain = earnedMoney - ingredientCostTotal

        // Get reviews created today to evaluate average satisfaction
        val todaysReviews = database.gameDao().getReviewsForDay(currentState.day)
        var popularityChange = 0
        if (todaysReviews.isNotEmpty()) {
            val sumRank = todaysReviews.map { it.rrating }.sum()
            val avgRank = sumRank / todaysReviews.size
            popularityChange = when {
                avgRank >= 4.2f -> ((customersServed * 0.4) + 2).toInt()
                avgRank >= 3.5f -> ((customersServed * 0.2) + 1).toInt()
                avgRank >= 2.8f -> 0
                else -> -((customersServed * 0.3) + 2).toInt()
            }
        }

        val nextDay = currentState.day + 1
        val nextMoney = (currentState.money + netGain).coerceAtLeast(0)
        val nextPopularity = (currentState.popularity + popularityChange).coerceIn(1, 1000)

        val updatedState = currentState.copy(
            day = nextDay,
            money = nextMoney,
            popularity = nextPopularity
        )

        // Save back to DB
        repository.saveGameState(updatedState)

        addSimLog("21:00", "============ 本日の営業結果 ============", "info")
        addSimLog("21:00", "総売上: +${earnedMoney}円 / 食材費: -${ingredientCostTotal}円", "money")
        addSimLog("21:00", "本日の純利益: ${if (netGain >= 0) "+" else ""}${netGain}円", "money")
        addSimLog("21:00", "来客数: ${customersServed}人 / 人気変化: ${if (popularityChange >= 0) "+" else ""}${popularityChange}人", "info")
        addSimLog("21:00", "本日の営業をすべて終了しました！明日（第${nextDay}日目）も頑張りましょう！", "upgrade")

        _isSimulating.value = false
    }

    // Purchase Upgrades
    fun buyUpgrade(upgradeType: String, cost: Int) {
        val state = gameState.value ?: return
        if (state.money < cost) return

        viewModelScope.launch {
            val updatedState = when (upgradeType) {
                "SEATS" -> state.copy(
                    money = state.money - cost,
                    seatingCapacity = state.seatingCapacity + 2
                )
                "BOILING" -> state.copy(
                    money = state.money - cost,
                    boilingSkillLvl = state.boilingSkillLvl + 1
                )
                "DECOR" -> state.copy(
                    money = state.money - cost,
                    decorLvl = state.decorLvl + 1
                )
                "MARKETING" -> state.copy(
                    money = state.money - cost,
                    marketingLvl = state.marketingLvl + 1
                )
                else -> state
            }
            repository.saveGameState(updatedState)
        }
    }

    // Purchase / Unlock Ingredients
    fun unlockIngredient(ingredientName: String, cost: Int) {
        val state = gameState.value ?: return
        if (state.money < cost) return

        val unlockedList = state.unlockedIngredientsStr.split(",").toMutableList()
        if (unlockedList.contains(ingredientName)) return

        unlockedList.add(ingredientName)

        viewModelScope.launch {
            val updatedState = state.copy(
                money = state.money - cost,
                unlockedIngredientsStr = unlockedList.joinToString(",")
            )
            repository.saveGameState(updatedState)
        }
    }
}
