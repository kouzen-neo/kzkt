@file:OptIn(ExperimentalMaterial3Api::class)

package com.cypy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ModelTraining
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cypy.app.core.Config
import com.cypy.app.ui.component.ChipsRow
import com.cypy.app.ui.component.Material3SettingsGroup
import com.cypy.app.ui.component.Material3SettingsItem
import com.cypy.app.ui.theme.DefaultThemeColor
import kotlinx.coroutines.launch

// Seed-color presets for the accent picker (Material You keeps the default crimson).
private val ACCENT_PRESETS = listOf(
    DefaultThemeColor,
    Color(0xFF6D5DF6),
    Color(0xFF00897B),
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFFF9A825),
)

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    themeColor: Color,
    onThemeColorChange: (Color) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )

        // ── Appearance ──
        Material3SettingsGroup(
            title = "Appearance",
            items = listOf(
                Material3SettingsItem(
                    leadingContent = { SettingsIcon(Icons.Outlined.DarkMode) },
                    title = { Text("Dark mode") },
                    description = { Text("Use the dark color scheme") },
                    trailingContent = {
                        Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
                    },
                ),
                Material3SettingsItem(
                    leadingContent = { SettingsIcon(Icons.Outlined.BrightnessLow) },
                    title = { Text("Pure black") },
                    description = { Text("True black background in dark mode") },
                    enabled = darkTheme,
                    trailingContent = {
                        Switch(
                            checked = pureBlack,
                            onCheckedChange = onPureBlackChange,
                            enabled = darkTheme,
                        )
                    },
                ),
                Material3SettingsItem(
                    leadingContent = { SettingsIcon(Icons.Outlined.Palette) },
                    title = { Text("Accent color") },
                    description = {
                        Text(if (themeColor == DefaultThemeColor) "System / Material You (default)" else "Custom seed color")
                    },
                    trailingContent = { AccentColorRow(themeColor, onThemeColorChange) },
                ),
            ),
        )

        // ── Provider ──
        Column {
            Text(
                "Provider",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            val selectedProvider = viewModel.settings.value.llmProvider
            ChipsRow(
                chips = Config.PROVIDER_REGISTRY.values.map { it.key to it.displayName },
                currentValue = selectedProvider,
                onValueUpdate = { key -> scope.launch { viewModel.settingsRepo.saveProvider(key) } },
            )
            Config.PROVIDER_REGISTRY[selectedProvider]?.let { meta ->
                Text(
                    meta.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                )
            }
        }

        // ── Target Language ──
        Column {
            Text(
                "Target Language",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            val language = viewModel.settings.value.targetLanguage
            ChipsRow(
                chips = Config.LANGUAGE_CHOICES.map { it to it },
                currentValue = language,
                onValueUpdate = { lang -> scope.launch { viewModel.settingsRepo.saveLanguage(lang) } },
            )
        }

        // ── API Keys ──
        Column {
            Text(
                "API Keys",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            Material3SettingsGroup(
                items = listOf(
                    ApiKeyItem("Gemini", viewModel, "geminiApiKey", Icons.Outlined.GppGood),
                    ApiKeyItem("OpenAI", viewModel, "openaiApiKey", Icons.Outlined.AutoAwesome),
                    ApiKeyItem("OpenRouter", viewModel, "openrouterApiKey", Icons.Outlined.Router),
                    ApiKeyItem("Zen", viewModel, "zenApiKey", Icons.Outlined.Bolt),
                    ApiKeyItem("OpenCode Go", viewModel, "opencodegoApiKey", Icons.Outlined.Code),
                    ApiKeyItem("Custom", viewModel, "customApiKey", Icons.Outlined.SettingsEthernet),
                ),
            )
        }

        // ── Model ──
        Column {
            Text(
                "Model",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            ModelSection(viewModel)

            val selectedProvider = viewModel.settings.value.llmProvider
            if (selectedProvider == "custom") {
                CustomUrlSection(viewModel)
            }
        }

        // ── Tweak Parameters ──
        Column {
            Text(
                "Tweak Parameters",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            TweakParamsSection(viewModel)
        }

        // ── SFX Filter Mode ──
        Column {
            Text(
                "SFX Filter Mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
            )
            SfxFilterSection(viewModel)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun AccentColorRow(selected: Color, onSelect: (Color) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        ACCENT_PRESETS.forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = color, shape = CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (color == selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(color) },
            )
        }
    }
}

@Composable
private fun ApiKeyItem(
    label: String,
    viewModel: MainViewModel,
    settingKey: String,
    icon: ImageVector,
): Material3SettingsItem {
    val scope = rememberCoroutineScope()
    val value by remember { derivedStateOf {
        when (settingKey) {
            "geminiApiKey" -> viewModel.settings.value.geminiApiKey
            "openaiApiKey" -> viewModel.settings.value.openaiApiKey
            "openrouterApiKey" -> viewModel.settings.value.openrouterApiKey
            "zenApiKey" -> viewModel.settings.value.zenApiKey
            "opencodegoApiKey" -> viewModel.settings.value.opencodegoApiKey
            "customApiKey" -> viewModel.settings.value.customApiKey
            else -> ""
        }
    } }
    var textState by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }

    return Material3SettingsItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        },
        title = {
            OutlinedTextField(
                value = textState,
                onValueChange = { newText ->
                    textState = newText
                    scope.launch {
                        viewModel.settingsRepo.saveApiKey(label.lowercase().replace(" ", ""), newText)
                    }
                },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Hide" else "Show",
                        )
                    }
                },
            )
        },
    )
}

@Composable
private fun ModelSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val selectedProvider = viewModel.settings.value.llmProvider
    val meta = Config.PROVIDER_REGISTRY[selectedProvider]

    val modelValue by remember(selectedProvider) { derivedStateOf {
        when (selectedProvider) {
            "gemini" -> viewModel.settings.value.modelGemini
            "openai" -> viewModel.settings.value.modelOpenai
            "openrouter" -> viewModel.settings.value.modelOpenrouter
            "zen" -> viewModel.settings.value.modelZen
            "opencodego" -> viewModel.settings.value.modelOpencodego
            "custom" -> viewModel.settings.value.modelCustom
            else -> ""
        }
    } }
    val metaLabel = meta?.displayName ?: selectedProvider

    Material3SettingsGroup(
        items = listOf(
            Material3SettingsItem(
                leadingContent = { SettingsIcon(Icons.Outlined.ModelTraining) },
                title = {
                    ModelDropdownInput(
                        label = metaLabel,
                        value = modelValue,
                        presets = Config.PRESET_MODELS[selectedProvider] ?: emptyList(),
                        onValue = { scope.launch { viewModel.settingsRepo.saveModel(selectedProvider, it) } },
                    )
                },
            ),
        ),
    )
}

@Composable
private fun CustomUrlSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val customBaseUrl by remember { derivedStateOf { viewModel.settings.value.customBaseUrl } }
    val customModelsLoading = viewModel.customModelsLoading.value
    val customModels = viewModel.customModels.toList()

    Material3SettingsGroup(
        items = listOf(
            Material3SettingsItem(
                leadingContent = { SettingsIcon(Icons.Outlined.Link) },
                title = {
                    var urlText by remember(customBaseUrl) { mutableStateOf(customBaseUrl) }
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { newUrl ->
                            urlText = newUrl
                            scope.launch { viewModel.settingsRepo.saveCustomBaseUrl(newUrl) }
                        },
                        placeholder = { Text("https://api.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                },
            ),
            Material3SettingsItem(
                leadingContent = { SettingsIcon(Icons.Outlined.Science) },
                title = { Text("Detect Models from API") },
                description = { Text("Fetch /v1/models from the custom base URL") },
                enabled = customBaseUrl.isNotBlank() && !customModelsLoading,
                trailingContent = {
                    if (customModelsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                onClick = { viewModel.fetchCustomModels(customBaseUrl, viewModel.settings.value.customApiKey) },
            ),
        ),
    )
    if (customModels.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        CustomModelSelector(
            models = customModels,
            selected = viewModel.settings.value.modelCustom,
            onSelect = { scope.launch { viewModel.settingsRepo.saveModel("custom", it) } }
        )
    }
}

@Composable
private fun TweakParamsSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()

    // Each slider reads only its own field
    @Composable fun slider(keyField: String, label: String, range: ClosedFloatingPointRange<Float>) {
        val value by remember { derivedStateOf {
            when (keyField) {
                "max_bubbles" -> viewModel.settings.value.maxBubblesPerRequest.toFloat()
                "request_delay" -> viewModel.settings.value.minRequestDelay
                "pad_x" -> viewModel.settings.value.padXRatio
                "pad_y" -> viewModel.settings.value.padYRatio
                "min_pad" -> viewModel.settings.value.minPad.toFloat()
                else -> 0f
            }
        } }
        var sliderValue by remember(value) { mutableFloatStateOf(value) }
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    title = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                val fmt = when (keyField) {
                                    "request_delay" -> "%.1fs".format(sliderValue)
                                    "min_pad" -> "${sliderValue.toInt()}"
                                    "max_bubbles" -> "${sliderValue.toInt()}"
                                    else -> "%.2f".format(sliderValue)
                                }
                                Text(fmt, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        viewModel.settingsRepo.saveTweakParam(keyField,
                                            when (keyField) {
                                                "max_bubbles", "min_pad" -> sliderValue.toInt()
                                                else -> sliderValue
                                            }
                                        )
                                    }
                                },
                                valueRange = range,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                ),
            ),
        )
    }

    slider("max_bubbles", "Bubbles per request", 5f..50f)
    slider("request_delay", "Min request delay (s)", 0.5f..10f)
    slider("pad_x", "Pad X ratio", 0.1f..1.0f)
    slider("pad_y", "Pad Y ratio", 0.1f..1.0f)
    slider("min_pad", "Min padding (px)", 5f..100f)
}

@Composable
private fun SfxFilterSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val current by remember { derivedStateOf { viewModel.settings.value.filterSfxMode } }
    val modes = listOf("balanced", "relaxed", "strict")
    Material3SettingsGroup(
        items = modes.map { mode ->
            Material3SettingsItem(
                leadingContent = { SettingsIcon(Icons.Outlined.Tune) },
                title = { Text(mode.uppercase().replaceFirstChar { it }) },
                isHighlighted = current == mode,
                trailingContent = {
                    RadioButton(
                        selected = current == mode,
                        onClick = { scope.launch { viewModel.settingsRepo.saveTweakParam("sfx_mode", mode) } },
                    )
                },
            )
        },
    )
}

@Composable
private fun ModelDropdownInput(
    label: String,
    value: String,
    presets: List<String>,
    onValue: (String) -> Unit,
) {
    var textState by remember(value) { mutableStateOf(value) }
    var expanded by remember { mutableStateOf(false) }

    val options = remember(presets, value) {
        val list = presets.toMutableList()
        if (value.isNotBlank() && value !in list) list.add(0, value)
        list
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = textState,
            onValueChange = { newText ->
                textState = newText
                onValue(newText)
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        textState = model
                        onValue(model)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun CustomModelSelector(
    models: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val displayModels = if (models.size > 200) models.take(200) + "… (${models.size} total)" else models
            displayModels.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model, style = MaterialTheme.typography.bodySmall) },
                    onClick = { onSelect(model); expanded = false },
                )
            }
        }
    }
}
