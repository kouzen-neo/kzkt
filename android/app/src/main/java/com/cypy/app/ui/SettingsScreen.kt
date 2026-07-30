@file:OptIn(ExperimentalMaterial3Api::class)

package com.cypy.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cypy.app.core.Config
import com.cypy.app.data.SettingsRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showApiKeys by remember { mutableStateOf(false) }
    val providers = remember { Config.PROVIDER_REGISTRY.values.toList() }

    // ── Observe only what's needed inline, scoped by key ──
    val selectedProvider by remember { derivedStateOf { viewModel.settings.value.llmProvider } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Provider ──
            Text("Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ProviderSelector(
                providers = providers,
                selectedKey = selectedProvider,
                onSelect = { key ->
                    scope.launch { viewModel.settingsRepo.saveProvider(key) }
                }
            )

            // Show description
            val meta = remember(selectedProvider) { Config.PROVIDER_REGISTRY[selectedProvider] }
            if (meta != null) {
                Text(meta.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Language ──
            Text("Target Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LanguageSection(viewModel)

            // ── API Keys (collapsible) ──
            Text("API Keys", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            var showKeys by remember { mutableStateOf(false) }
            TextButton(onClick = { showKeys = !showKeys }) {
                Text(if (showKeys) "Hide API Keys" else "Show API Keys (saved securely)")
            }
            if (showKeys) {
                ApiKeysSection(viewModel)
            }

            // ── Model ──
            Text("Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val modelValue = when (selectedProvider) {
                "gemini" -> remember { derivedStateOf { viewModel.settings.value.modelGemini } }
                "openai" -> remember { derivedStateOf { viewModel.settings.value.modelOpenai } }
                "openrouter" -> remember { derivedStateOf { viewModel.settings.value.modelOpenrouter } }
                "zen" -> remember { derivedStateOf { viewModel.settings.value.modelZen } }
                "opencodego" -> remember { derivedStateOf { viewModel.settings.value.modelOpencodego } }
                "custom" -> remember { derivedStateOf { viewModel.settings.value.modelCustom } }
                else -> remember { derivedStateOf { "" } }
            }
            val metaLabel = meta?.displayName ?: selectedProvider
            ModelDropdownInput(
                label = metaLabel,
                value = modelValue.value,
                presets = Config.PRESET_MODELS[selectedProvider] ?: emptyList(),
                onValue = { scope.launch { viewModel.settingsRepo.saveModel(selectedProvider, it) } },
            )

            // Custom base URL + model detect
            if (selectedProvider == "custom") {
                CustomUrlSection(viewModel)
            }

            // ── Tweak Parameters ──
            Text("Tweak Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TweakParamsSection(viewModel)

            // SFX filter mode
            Text("SFX Filter Mode", style = MaterialTheme.typography.bodyLarge)
            SfxFilterSection(viewModel)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Scoped sub‑sections: each reads only its own field(s) ──

@Composable
private fun LanguageSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val selected by remember { derivedStateOf { viewModel.settings.value.targetLanguage } }
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
            Config.LANGUAGE_CHOICES.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang) },
                    onClick = { scope.launch { viewModel.settingsRepo.saveLanguage(lang) }; expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ApiKeysSection(viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ApiKeyInput("Gemini", viewModel, "geminiApiKey")
        ApiKeyInput("OpenAI", viewModel, "openaiApiKey")
        ApiKeyInput("OpenRouter", viewModel, "openrouterApiKey")
        ApiKeyInput("Zen", viewModel, "zenApiKey")
        ApiKeyInput("OpenCode Go", viewModel, "opencodegoApiKey")
        ApiKeyInput("Custom", viewModel, "customApiKey")
    }
}

@Composable
private fun ApiKeyInput(label: String, viewModel: MainViewModel, settingKey: String) {
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
    OutlinedTextField(
        value = textState,
        onValueChange = { newText ->
            textState = newText
            scope.launch { viewModel.settingsRepo.saveApiKey(
                label.lowercase().replace(" ", ""), newText) }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show",
                )
            }
        },
    )
}

@Composable
private fun CustomUrlSection(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val customBaseUrl by remember { derivedStateOf { viewModel.settings.value.customBaseUrl } }
    val customModelsLoading = viewModel.customModelsLoading.value
    val customModels = viewModel.customModels.toList()

    Text("Custom Base URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = { viewModel.fetchCustomModels(customBaseUrl, viewModel.settings.value.customApiKey) },
        enabled = customBaseUrl.isNotBlank() && !customModelsLoading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (customModelsLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text("Detect Models from API")
    }
    if (customModels.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Available Models", style = MaterialTheme.typography.bodyLarge)
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("balanced", "relaxed", "strict").forEach { mode ->
            FilterChip(
                selected = current == mode,
                onClick = { scope.launch { viewModel.settingsRepo.saveTweakParam("sfx_mode", mode) } },
                label = { Text(mode.uppercase().replaceFirstChar { it }) },
            )
        }
    }
}

// ── Shared composables ──

@Composable
private fun ProviderSelector(
    providers: List<Config.ProviderMeta>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        providers.forEach { meta ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = meta.key == selectedKey,
                    onClick = { onSelect(meta.key) },
                )
                Spacer(Modifier.width(4.dp))
                Text(meta.displayName, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
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
