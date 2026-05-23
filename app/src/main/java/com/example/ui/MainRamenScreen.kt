package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.CustomerReview
import com.example.data.GameState
import com.example.data.Recipe
import com.example.ui.theme.*

@Composable
fun MainRamenScreen(viewModel: RamenViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.gameState.collectAsState()
    val recipesList by viewModel.recipes.collectAsState()
    val reviewsList by viewModel.reviews.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val simHour by viewModel.simHour.collectAsState()
    val simWeather by viewModel.simWeather.collectAsState()
    val dailyEarnings by viewModel.dailyEarnings.collectAsState()
    val customersServed by viewModel.customersServedCount.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    var showRecipeDetailDialog by remember { mutableStateOf<Recipe?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Recipe?>(null) }

    val activeFeaturedRecipe = remember(state, recipesList) {
        recipesList.find { it.id == state?.currentRecipeId }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RamenTopBar(state = state, activeRecipe = activeFeaturedRecipe)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Screen tabs - styled traditionally
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = DarkWoodBar,
                contentColor = RamenRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = BrothGold,
                        height = 4.dp
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { if (!isSimulating) activeTab = 0 },
                    enabled = !isSimulating,
                    modifier = Modifier.testTag("tab_business"),
                    text = {
                        Text(
                            text = stringResource(R.string.tab_business),
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 0) RamenRed else NoodleIvory.copy(alpha = 0.8f)
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { if (!isSimulating) activeTab = 1 },
                    enabled = !isSimulating,
                    modifier = Modifier.testTag("tab_kitchen"),
                    text = {
                        Text(
                            text = stringResource(R.string.tab_kitchen),
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 1) RamenRed else NoodleIvory.copy(alpha = 0.8f)
                        )
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { if (!isSimulating) activeTab = 2 },
                    enabled = !isSimulating,
                    modifier = Modifier.testTag("tab_upgrade"),
                    text = {
                        Text(
                            text = stringResource(R.string.tab_upgrade),
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 2) RamenRed else NoodleIvory.copy(alpha = 0.8f)
                        )
                    }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { if (!isSimulating) activeTab = 3 },
                    enabled = !isSimulating,
                    modifier = Modifier.testTag("tab_reviews"),
                    text = {
                        Text(
                            text = stringResource(R.string.tab_reviews),
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab == 3) RamenRed else NoodleIvory.copy(alpha = 0.8f)
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> SimulationTab(
                        viewModel = viewModel,
                        state = state,
                        activeRecipe = activeFeaturedRecipe,
                        isSimulating = isSimulating,
                        simHour = simHour,
                        simWeather = simWeather,
                        dailyEarnings = dailyEarnings,
                        customersServed = customersServed
                    )
                    1 -> KitchenTab(
                        viewModel = viewModel,
                        state = state,
                        recipes = recipesList,
                        onViewRecipe = { showRecipeDetailDialog = it },
                        onDeleteRecipe = { showDeleteConfirmDialog = it }
                    )
                    2 -> UpgradesTab(
                        viewModel = viewModel,
                        state = state
                    )
                    3 -> ReviewsTab(
                        reviews = reviewsList,
                        totalRevenue = reviewsList.sumOf { it.moneySpent }
                    )
                }
            }
        }
    }

    // Recipe detail view
    showRecipeDetailDialog?.let { recipe ->
        RecipeDetailDialog(recipe = recipe, onDismiss = { showRecipeDetailDialog = null })
    }

    // Confirm Delete Recipe
    showDeleteConfirmDialog?.let { recipe ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("レシピの破棄", fontWeight = FontWeight.Bold, color = RamenRed) },
            text = { Text("レシピ「${recipe.name}」を完全に破棄しますか？この操作は取り消せません。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecipe(recipe.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RamenRed)
                ) {
                    Text("破棄する", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("キャンセル", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = DarkWoodBar,
            textContentColor = NoodleIvory,
            titleContentColor = RamenRed
        )
    }
}

@Composable
fun RamenTopBar(state: GameState?, activeRecipe: Recipe?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkWoodBar)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (state != null) stringResource(R.string.label_day, state.day) else "第 - 日目",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrothGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Popularity",
                        tint = EggYolk,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state != null) "人気: ${state.popularity} ★" else "人気: -",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NoodleIvory.copy(alpha = 0.9f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (state != null) stringResource(R.string.label_money, state.money) else "資金: - 円",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrothGold
                )
                // Small indicator of current capacity
                Text(
                    text = "カウンター: ${state?.seatingCapacity ?: 4}席",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoodleIvory.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = RamenRed.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "看板メニュー:",
                style = MaterialTheme.typography.labelMedium,
                color = NoodleIvory.copy(alpha = 0.6f)
            )
            Text(
                text = activeRecipe?.name ?: "未設定",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (activeRecipe != null) NoodleIvory else RamenRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f)
            )
            if (activeRecipe != null) {
                Text(
                    text = "${activeRecipe.sellingPrice}円 (評★${activeRecipe.tasteScore})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EggYolk
                )
            }
        }
    }
}

@Composable
fun SimulationTab(
    viewModel: RamenViewModel,
    state: GameState?,
    activeRecipe: Recipe?,
    isSimulating: Boolean,
    simHour: Int,
    simWeather: Weather,
    dailyEarnings: Int,
    customersServed: Int
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isSimulating) {
            // Closed / Intro Screen state
            Text(
                text = "ラーメン店 準備中",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = RamenRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Vector Ramen Illustration
            RamenBowlVector(modifier = Modifier.size(160.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Widget of the day
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkWoodBar),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "本日の天気予報",
                        fontWeight = FontWeight.Bold,
                        color = BrothGold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "暖簾を掲げると天気が確定し、来客傾向に影響を及ぼします。お昼時(12時-13時)とディナー時(18時-20時)が最も混み合います。",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoodleIvory.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeRecipe == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RamenRed.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RamenRed)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = RamenRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "看板メニュー（提供するラーメン）が設定されていません。厨房タブで開発し、設定してください。",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = RamenRed
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.startDayBusiness() },
                    colors = ButtonDefaults.buttonColors(containerColor = RamenRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(60.dp)
                        .testTag("open_shop_btn"),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Open", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "暖簾を掛け、開店する！",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Running Daily Simulation Screen state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live stats
                Column {
                    Text(
                        text = "店舗 営業中！ 🔴",
                        fontWeight = FontWeight.Bold,
                        color = RamenRed,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "現在の時刻: ${String.format("%02d:00", simHour)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrothGold
                    )
                    Text(
                        text = "天気: ${simWeather.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoodleIvory.copy(alpha = 0.8f)
                    )
                }

                // Financial state
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "本日の売上",
                        style = MaterialTheme.typography.labelSmall,
                        color = NoodleIvory.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "+${dailyEarnings} 円",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = BrothGold
                    )
                    Text(
                        text = "配膳人数: ${customersServed} 人",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoodleIvory.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seating Capacity Visualizer
            Text(
                text = "店舗カウンター客席 (${viewModel.activeCustomers.size}/${state?.seatingCapacity ?: 4} 満席率)",
                fontWeight = FontWeight.Bold,
                color = NoodleIvory.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Grid of seating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cap = state?.seatingCapacity ?: 4
                val seatsHalf1 = if (cap > 6) cap / 2 else cap
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until seatsHalf1) {
                        val occupier = viewModel.activeCustomers.find { it.seatIndex == i }
                        SeatCard(seatNum = i + 1, customer = occupier)
                    }
                }
                
                if (cap > 6) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in seatsHalf1 until cap) {
                            val occupier = viewModel.activeCustomers.find { it.seatIndex == i }
                            SeatCard(seatNum = i + 1, customer = occupier)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live event logs
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCharcoal),
                border = BorderStroke(1.dp, WoodBrown),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    if (viewModel.simLogs.isEmpty()) {
                        Text(
                            text = "来客をお待ちしております...",
                            color = NoodleIvory.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(
                            reverseLayout = false,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(viewModel.simLogs, key = { it.id }) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "[${log.timeStr}]",
                                        color = BrothGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = log.text,
                                        color = if (log.iconType == "customer") NoodleIvory else if (log.iconType == "money") EggYolk else RicePaper,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatCard(seatNum: Int, customer: ActiveCustomer?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (customer != null) CharcoalCard else DarkWoodBar.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (customer != null) {
                    when (customer.status) {
                        "注文中" -> RamenRed
                        "食事中" -> BrothGold
                        else -> EggYolk
                    }
                } else WoodBrown.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        if (customer == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${seatNum}番席 (空席)",
                    color = NoodleIvory.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.5f))
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${seatNum}番席: ${customer.type}",
                        fontWeight = FontWeight.Bold,
                        color = NoodleIvory,
                        fontSize = 12.sp
                    )

                    // Status pill
                    val statusColor = when (customer.status) {
                        "注文中" -> RamenRed
                        "食事中" -> BrothGold
                        else -> EggYolk
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = customer.status,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = customer.comment,
                    fontSize = 11.sp,
                    color = NoodleIvory.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (customer.status == "食事中") {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = customer.progress,
                        color = BrothGold,
                        trackColor = WoodBrown.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitchenTab(
    viewModel: RamenViewModel,
    state: GameState?,
    recipes: List<Recipe>,
    onViewRecipe: (Recipe) -> Unit,
    onDeleteRecipe: (Recipe) -> Unit
) {
    val scrollState = rememberScrollState()

    // Read unlocks
    val unlockedStr = state?.unlockedIngredientsStr ?: ""
    val unlockedList = remember(unlockedStr) {
        unlockedStr.split(",").filter { it.isNotEmpty() }
    }

    // Temporary states within formulation
    var tempName by remember { mutableStateOf(viewModel.sandboxRecipeName) }
    var tempBroth by remember { mutableStateOf(viewModel.sandboxBroth) }
    var tempNoodle by remember { mutableStateOf(viewModel.sandboxNoodles) }
    var tempPrice by remember { mutableStateOf(viewModel.sandboxSellingPrice) }

    // Live calculations based on options
    val currentMetrics = remember(tempBroth, tempNoodle, viewModel.sandboxToppings.size, state?.boilingSkillLvl) {
        viewModel.sandboxRecipeName = tempName
        viewModel.sandboxBroth = tempBroth
        viewModel.sandboxNoodles = tempNoodle
        viewModel.sandboxSellingPrice = tempPrice
        viewModel.calculateSandboxMetrics()
    }
    val estimatedTaste = currentMetrics.first
    val estimatedCost = currentMetrics.second
    val margin = (tempPrice - estimatedCost).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "🍜 秘伝ラーメン開発ラボ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrothGold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Core Formulation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            border = BorderStroke(1.dp, WoodBrown.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Recipe Name input
                Text("ラーメンの名称", fontWeight = FontWeight.Bold, color = NoodleIvory, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = { Text("例: 秘伝濃厚極み麺") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recipe_name_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkWoodBar,
                        unfocusedContainerColor = DarkWoodBar,
                        focusedTextColor = NoodleIvory,
                        unfocusedTextColor = NoodleIvory
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Soup Selection
                Text("特製スープの出汁", fontWeight = FontWeight.Bold, color = NoodleIvory, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val soups = listOf("醤油", "塩", "豚骨", "味噌")
                    soups.forEach { s ->
                        val isUnlocked = unlockedList.contains(s)
                        val isSelected = tempBroth == s

                        Button(
                            onClick = { if (isUnlocked) tempBroth = s },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RamenRed else if (isUnlocked) DarkWoodBar else Color.Gray.copy(alpha = 0.2f),
                                contentColor = if (isSelected) Color.White else if (isUnlocked) NoodleIvory else Color.Gray
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("soup_btn_$s"),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            enabled = isUnlocked
                        ) {
                            Text(s + if (!isUnlocked) "🔒" else "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Noodle Selection
                Text("こだわりの麺", fontWeight = FontWeight.Bold, color = NoodleIvory, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val noodles = listOf("極細麺", "中細麺", "太麺")
                    noodles.forEach { n ->
                        val isUnlocked = unlockedList.contains(n)
                        val isSelected = tempNoodle == n

                        Button(
                            onClick = { if (isUnlocked) tempNoodle = n },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RamenRed else if (isUnlocked) DarkWoodBar else Color.Gray.copy(alpha = 0.2f),
                                contentColor = if (isSelected) Color.White else if (isUnlocked) NoodleIvory else Color.Gray
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            enabled = isUnlocked
                        ) {
                            Text(n + if (!isUnlocked) "🔒" else "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toppings Multi Selection
                Text("トッピングトッピング (複数可)", fontWeight = FontWeight.Bold, color = NoodleIvory, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                
                val possibleToppings = listOf("ネギ", "メンマ", "チャーシュー", "味玉", "海苔", "ナルト", "背脂", "黒マー油", "プレミアム極厚叉焼")
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    possibleToppings.forEach { t ->
                        val isUnlocked = unlockedList.contains(t)
                        val isSelected = viewModel.sandboxToppings.contains(t)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) RamenRed
                                    else if (isUnlocked) DarkWoodBar
                                    else Color.Gray.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) RamenRed
                                    else if (isUnlocked) WoodBrown.copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = isUnlocked) {
                                    if (isSelected) viewModel.sandboxToppings.remove(t)
                                    else viewModel.sandboxToppings.add(t)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t + if (!isUnlocked) " 🔒" else "",
                                color = if (isSelected) Color.White else if (isUnlocked) NoodleIvory else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price adjustments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "販売価格の設定: ${tempPrice}円",
                        fontWeight = FontWeight.Bold,
                        color = NoodleIvory,
                        fontSize = 13.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (tempPrice > 400) tempPrice -= 50 },
                            modifier = Modifier.background(DarkWoodBar, CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Down", tint = BrothGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { if (tempPrice < 1800) tempPrice += 50 },
                            modifier = Modifier.background(DarkWoodBar, CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Up", tint = BrothGold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Performance Metric live estimation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkWoodBar),
            border = BorderStroke(1.dp, BrothGold.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🍳 試作品テイスティング結果",
                    fontWeight = FontWeight.Bold,
                    color = BrothGold,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("美味しさ指数", color = NoodleIvory.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${estimatedTaste} / 100", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EggYolk)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("一杯の食材原価", color = NoodleIvory.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${estimatedCost} 円", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = NoodleIvory)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("一杯あたりの粗利", color = NoodleIvory.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("+${margin} 円", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = BrothGold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Synergy advice line
                val advice = remember(tempBroth, tempNoodle, viewModel.sandboxToppings.size) {
                    when {
                        tempBroth == "豚骨" && tempNoodle == "極細麺" -> "【相性抜群】本場博多仕立て！極細麺が豚骨の芳醇な旨味を美しくからめ取ります。"
                        tempBroth == "味噌" && tempNoodle == "太麺" -> "【相性抜群】王道の札幌仕立て！極太麺が濃厚な味噌スープに負けずに主張します。"
                        tempBroth == "塩" && tempNoodle == "極細麺" -> "【極細麺マッチ】淡麗な塩スープを、スルリと喉越しよく頂けます。"
                        tempBroth == "味噌" && tempNoodle == "極細麺" -> "【ミスマッチ】濃厚味噌に極細麺はスープの強さに負けてしまい、バランスが崩れます。"
                        tempBroth == "塩" && viewModel.sandboxToppings.contains("背脂") -> "【ミスマッチ】澄んだ塩スープに背脂コッテリは脂っこくなり、上品さを損ないます。"
                        else -> "【安定品質】個々の素材の旨味がしっかりと調和した素晴らしい構成です。"
                    }
                }

                Text(
                    text = advice,
                    fontSize = 11.sp,
                    color = BrothGold,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmCharcoal.copy(alpha = 0.5f))
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action button to cook and register
        Button(
            onClick = {
                viewModel.saveCraftedRecipe(tempName)
                tempName = "自慢の特製ラーメン" // reset name placeholder
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_recipe_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = RamenRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("このレシピを開発・看板登録する", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Historic recipe list
        Text(
            text = "開発済みのメニュー一覧",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NoodleIvory,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (recipes.isEmpty()) {
            Text(
                text = "登録されているメニューはありません。上記のラボからオリジナルラーメンを創作してください！",
                color = NoodleIvory.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recipes.forEach { r ->
                    val isChosen = state?.currentRecipeId == r.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isChosen) BrothGold else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChosen) DarkWoodBar else CharcoalCard
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = r.name,
                                        fontWeight = FontWeight.Bold,
                                        color = NoodleIvory,
                                        fontSize = 14.sp
                                    )
                                    if (isChosen) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(RamenRed)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("看板", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${r.broth}スープ / ${r.noodles} / 具: ${r.getToppingsList().joinToString(",")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NoodleIvory.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "原価: ${r.costPrice}円 / 価格: ${r.sellingPrice}円 / 美味★${r.tasteScore}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrothGold
                                )
                            }

                            Row {
                                if (!isChosen) {
                                    IconButton(onClick = { viewModel.selectRecipe(r.id) }) {
                                        Icon(Icons.Default.Check, contentDescription = "Select", tint = Color.Green)
                                    }
                                }
                                IconButton(onClick = { onViewRecipe(r) }) {
                                    Icon(Icons.Default.Info, contentDescription = "Detail", tint = NoodleIvory.copy(alpha = 0.7f))
                                }
                                IconButton(onClick = { onDeleteRecipe(r) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RamenRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradesTab(viewModel: RamenViewModel, state: GameState?) {
    val scrollState = rememberScrollState()
    if (state == null) return

    val unlockedStr = state.unlockedIngredientsStr
    val unlockedList = remember(unlockedStr) {
        unlockedStr.split(",").filter { it.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Section 1: Physical Upgrades
        Text(
            text = "🪵 店舗リフォーム・修業",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrothGold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Seating Capacity
        val seatingCost = when (state.seatingCapacity) {
            4 -> 12000
            6 -> 25000
            8 -> 45000
            10 -> 75000
            else -> -1
        }
        UpgradeCard(
            title = stringResource(R.string.upgrade_seats_title),
            desc = stringResource(R.string.upgrade_seats_desc),
            levelText = "現在: ${state.seatingCapacity} 席",
            icon = Icons.Default.Home,
            cost = seatingCost,
            canAfford = state.money >= seatingCost,
            onPurchase = { viewModel.buyUpgrade("SEATS", seatingCost) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Boiling quality
        val boilingCost = when (state.boilingSkillLvl) {
            1 -> 8000
            2 -> 18000
            3 -> 35000
            4 -> 60000
            else -> -1
        }
        UpgradeCard(
            title = stringResource(R.string.upgrade_boiling_title),
            desc = stringResource(R.string.upgrade_boiling_desc),
            levelText = "現在: 熟練度 Lvl ${state.boilingSkillLvl}",
            icon = Icons.Default.Build,
            cost = boilingCost,
            canAfford = state.money >= boilingCost,
            onPurchase = { viewModel.buyUpgrade("BOILING", boilingCost) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Decor quality
        val decorCost = when (state.decorLvl) {
            1 -> 5000
            2 -> 12000
            3 -> 25000
            4 -> 50050
            else -> -1
        }
        UpgradeCard(
            title = stringResource(R.string.upgrade_decor_title),
            desc = stringResource(R.string.upgrade_decor_desc),
            levelText = "現在: 魅力度 Lvl ${state.decorLvl}",
            icon = Icons.Default.Star,
            cost = decorCost,
            canAfford = state.money >= decorCost,
            onPurchase = { viewModel.buyUpgrade("DECOR", decorCost) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Marketing ads
        val marketingCost = when (state.marketingLvl) {
            1 -> 4000
            2 -> 10000
            3 -> 22000
            4 -> 45000
            else -> -1
        }
        UpgradeCard(
            title = stringResource(R.string.upgrade_marketing_title),
            desc = stringResource(R.string.upgrade_marketing_desc),
            levelText = "現在: 知名度 Lvl ${state.marketingLvl}",
            icon = Icons.Default.Notifications,
            cost = marketingCost,
            canAfford = state.money >= marketingCost,
            onPurchase = { viewModel.buyUpgrade("MARKETING", marketingCost) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Ingredients unlock
        Text(
            text = "🥩 特別な食材の仕入れ・開放",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrothGold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val unlockableIngredients = listOf(
            Triple("豚骨", 15000, "最高ランクのゲンコツを長時間炊いた、濃厚な豚骨スープが作れます。"),
            Triple("味噌", 10000, "信州味噌をはじめとする数種類のブレンド味噌スープを選択可能にします。"),
            Triple("太麺", 5000, "もっちり噛みごたえのある太い麺。コッテリした味噌や豚骨に好相性。"),
            Triple("チャーシュー", 8000, "みんな大好き！煮卵に並ぶラーメンの王様トッピング。サラリーマンに抜群。"),
            Triple("味玉", 4000, "トロトロ半熟の絶品味付け玉子。すべての客から愛される無敵具材。"),
            Triple("海苔", 2000, "横浜家系や東京醤油に不可欠な板のり。磯の香りが上品に溶け出します。"),
            Triple("ナルト", 3000, "昔懐かしい渦巻柄のナルト。ビジュアルが和風になり、シニアが好みます。"),
            Triple("背脂", 6000, "極上豚の背脂ミンチ。スープに強烈な「コク・甘み・脂感」を加えます。"),
            Triple("黒マー油", 12000, "ニンニクを焦がして香ばしさを極限に高めた黒オイル。豚骨スープの破壊力を大幅強化。"),
            Triple("プレミアム極厚叉焼", 28000, "一口では食べきれない超高級肉。圧倒的な美味指数パワーを叩き出します。")
        )

        val chunkedPair = unlockableIngredients.chunked(2)
        chunkedPair.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val name = item.first
                    val cost = item.second
                    val desc = item.third
                    val isPurchased = unlockedList.contains(name)

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPurchased) DarkWoodBar else CharcoalCard
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isPurchased) Color.Green.copy(alpha = 0.3f) else WoodBrown.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        color = NoodleIvory,
                                        fontSize = 13.sp
                                    )
                                    if (isPurchased) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Green.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("解放済", color = Color.Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = NoodleIvory.copy(alpha = 0.6f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!isPurchased) {
                                Button(
                                    onClick = { viewModel.unlockIngredient(name, cost) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RamenRed),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "${cost}円 で解放",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Fill empty column if odd elements
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun UpgradeCard(
    title: String,
    desc: String,
    levelText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cost: Int,
    canAfford: Boolean,
    onPurchase: () -> Unit
) {
    val isMaxLevel = cost < 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = BorderStroke(1.dp, WoodBrown.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkWoodBar)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Upgrade Icon",
                        tint = BrothGold,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, color = NoodleIvory, fontSize = 14.sp)
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = NoodleIvory.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = levelText, fontSize = 11.sp, color = BrothGold, fontWeight = FontWeight.Bold)
                }
            }

            if (isMaxLevel) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BrothGold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "極限 (MAX)", color = BrothGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RamenRed,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "${cost}円",
                        color = if (canAfford) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewsTab(reviews: List<CustomerReview>, totalRevenue: Int) {
    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, contentDescription = "Reviews empty", tint = WoodBrown, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "まだ常連客の評判はありません。\n「営業」をして口コミを広げましょう！",
                    textAlign = TextAlign.Center,
                    color = NoodleIvory.copy(alpha = 0.5f),
                    lineHeight = 18.sp,
                    fontSize = 13.sp
                )
            }
        }
    } else {
        val averageRating = remember(reviews) {
            reviews.map { it.rrating }.average()
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Summary Header Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkWoodBar)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("店舗の総合評価 (食べシュラン)", fontWeight = FontWeight.Bold, color = BrothGold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%.2f", averageRating),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                color = EggYolk
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star",
                                            tint = if (i <= averageRating.toInt()) EggYolk else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "口コミ数: ${reviews.size}件",
                                    fontSize = 10.sp,
                                    color = NoodleIvory.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("累計販売価格総額", color = NoodleIvory.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${totalRevenue}円", fontWeight = FontWeight.Bold, color = BrothGold, fontSize = 18.sp)
                    }
                }
            }

            // List of reviews
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reviews) { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Custom visual badge based on customer type
                                    val badgeColor = when (review.customerType) {
                                        "サラリーマン" -> Color(0xFF2196F3)
                                        "学生" -> Color(0xFF4CAF50)
                                        "女子高生" -> Color(0xFFE91E63)
                                        "ラーメンマニア" -> Color(0xFF9C27B0)
                                        else -> Color(0xFF795548) // シニア
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    ) {
                                        val initialText = if (review.customerType.length > 1) review.customerType.substring(0, 1) else "客"
                                        Text(
                                            text = initialText,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "匿名(${review.customerType})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NoodleIvory
                                    )
                                }

                                Text(
                                    text = "第${review.day}日目",
                                    fontSize = 11.sp,
                                    color = NoodleIvory.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row {
                                    for (i in 1..5) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Review Stars",
                                            tint = if (i <= review.rrating.toInt()) EggYolk else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "実食: ${review.recipeName}",
                                    fontSize = 10.sp,
                                    color = NoodleIvory.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = review.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoodleIvory.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailDialog(recipe: Recipe, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkWoodBar),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrothGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = recipe.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrothGold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = RamenRed.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(label = "スープ出汁:", value = recipe.broth)
                DetailRow(label = "使用した麺:", value = recipe.noodles)
                DetailRow(label = "具材:", value = recipe.getToppingsList().joinToString(", ").ifEmpty { "なし" })
                DetailRow(label = "美味しさ指数:", value = "${recipe.tasteScore} / 100", valueColor = EggYolk)
                DetailRow(label = "材料原価:", value = "${recipe.costPrice} 円")
                DetailRow(label = "設定価格:", value = "${recipe.sellingPrice} 円", valueColor = BrothGold)
                DetailRow(label = "仕込み難易度:", value = "★".repeat(recipe.complexity.coerceIn(1, 5)))

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RamenRed),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("閉じる", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = NoodleIvory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = NoodleIvory.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun RamenBowlVector(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Draw Soup Bowl body (lacquer red)
        val bowlPath = Path().apply {
            moveTo(centerX - width * 0.4f, centerY - height * 0.1f)
            quadraticTo(
                centerX - width * 0.35f, centerY + height * 0.3f,
                centerX - width * 0.15f, centerY + height * 0.35f
            )
            lineTo(centerX + width * 0.15f, centerY + height * 0.35f)
            quadraticTo(
                centerX + width * 0.35f, centerY + height * 0.3f,
                centerX + width * 0.4f, centerY - height * 0.1f
            )
            close()
        }
        drawPath(path = bowlPath, color = RamenRed)

        // Bowl rim (black lacquer trim)
        val rimPath = Path().apply {
            moveTo(centerX - width * 0.42f, centerY - height * 0.14f)
            lineTo(centerX + width * 0.42f, centerY - height * 0.14f)
            lineTo(centerX + width * 0.4f, centerY - height * 0.08f)
            lineTo(centerX - width * 0.4f, centerY - height * 0.08f)
            close()
        }
        drawPath(path = rimPath, color = Color.Black)

        // Draw Soup surface (golden broth color)
        val soupPath = Path().apply {
            moveTo(centerX - width * 0.39f, centerY - height * 0.08f)
            quadraticTo(
                centerX, centerY + height * 0.05f,
                centerX + width * 0.39f, centerY - height * 0.08f
            )
            close()
        }
        drawPath(path = soupPath, color = BrothGold)

        // Draw classic curly noodle strings using bezier curve
        for (i in -3..3) {
            val offsetMultiplier = i * 20f
            val noodlePath = Path().apply {
                moveTo(centerX + offsetMultiplier - 30f, centerY - height * 0.04f)
                cubicTo(
                    centerX + offsetMultiplier - 15f, centerY + height * 0.1f,
                    centerX + offsetMultiplier + 15f, centerY - height * 0.05f,
                    centerX + offsetMultiplier + 30f, centerY + height * 0.08f
                )
            }
            drawPath(
                path = noodlePath,
                color = NoodleIvory,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        // Draw green onion sprinkling (green small circles)
        drawCircle(color = Color(0xFF4CAF50), radius = 6f, center = Offset(centerX - 40f, centerY - 10f))
        drawCircle(color = Color(0xFF2E7D32), radius = 5f, center = Offset(centerX - 20f, centerY - 15f))
        drawCircle(color = Color(0xFF4CAF50), radius = 7f, center = Offset(centerX - 30f, centerY + 10f))

        // Draw Naruto scroll topping (white disc with pink swirl)
        drawCircle(color = Color.White, radius = 22f, center = Offset(centerX + 60f, centerY - 20f))
        val swirlPath = Path().apply {
            moveTo(centerX + 48f, centerY - 20f)
            quadraticTo(
                centerX + 60f, centerY + 2f,
                centerX + 70f, centerY - 15f
            )
            quadraticTo(
                centerX + 60f, centerY - 35f,
                centerX + 54f, centerY - 20f
            )
        }
        drawPath(
            path = swirlPath,
            color = Color(0xFFE91E63),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Draw a juicy Chashu pork strip (fleshy pink/brown rounded rectangle)
        val chashuPath = Path().apply {
            moveTo(centerX - 80f, centerY - 40f)
            lineTo(centerX - 30f, centerY - 45f)
            lineTo(centerX - 40f, centerY - 10f)
            lineTo(centerX - 90f, centerY - 5f)
            close()
        }
        drawPath(path = chashuPath, color = Color(0xFFD84315))
        drawPath(
            path = chashuPath,
            color = Color(0xFFFFCCBC),
            style = Stroke(width = 4f)
        )

        // Draw chopsticks resting on the side
        drawLine(
            color = WoodBrown,
            start = Offset(centerX - width * 0.55f, centerY - height * 0.22f),
            end = Offset(centerX + width * 0.55f, centerY - height * 0.15f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }
}
