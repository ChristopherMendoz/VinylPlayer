package com.chrismdz.vinylplayer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.chrismdz.vinylplayer.settings.AccentColor
import com.chrismdz.vinylplayer.settings.AccentColorValue
import com.chrismdz.vinylplayer.settings.AppSettings
import com.chrismdz.vinylplayer.settings.CollectionViewMode
import com.chrismdz.vinylplayer.settings.LibrarySortOrder
import com.chrismdz.vinylplayer.settings.PlayerBackground
import com.chrismdz.vinylplayer.settings.QueueAddBehavior
import com.chrismdz.vinylplayer.settings.RepeatMode
import com.chrismdz.vinylplayer.settings.SettingsPreferences
import com.chrismdz.vinylplayer.settings.ThemeMode
import com.chrismdz.vinylplayer.settings.TranslationLanguage
import java.text.DateFormat
import java.util.Date

enum class SettingsCategory(
    val title: String,
    val summary: String,
    val searchableText: String
) {
    APPEARANCE(
        "Apariencia y personalización",
        "Tema, color, fondo y letras",
        "oscuro claro sistema dorado rojo personalizado hexadecimal cualquier color colores de portada desenfocada sólido brazo fonocaptor tipografía letras tamaño"
    ),
    PLAYBACK(
        "Reproducción",
        "Continuidad, repetición y temporizador",
        "reanudar sesión posición avance automático repetir aleatorio temporizador auriculares"
    ),
    LIBRARY(
        "Biblioteca musical",
        "Escaneo, canciones y metadatos",
        "carpetas actualizar escanear canciones álbum artista orden archivos"
    ),
    LYRICS(
        "Letras y traducción",
        "Idioma, sincronización y visualización",
        "mostrar letras traducción idioma sincronización desfase tamaño negrita líneas anteriores siguientes"
    ),
    QUEUE(
        "Fila de reproducción",
        "Conservación y comportamiento de la cola",
        "fila cola conservar duplicados aleatorio siguiente agregar limpiar pendientes"
    ),
    AUDIO(
        "Audio y ecualizador",
        "Bandas y preajustes disponibles",
        "sonido ecualizador bandas graves bass vocal plano normalización"
    ),
    STORAGE(
        "Almacenamiento y permisos",
        "Accesos y diagnóstico de biblioteca",
        "permisos almacenamiento lectura audio notificaciones carpetas diagnóstico"
    ),
    ABOUT(
        "Acerca de VinylPlayer",
        "Versión, créditos y restablecimiento",
        "versión compilación github repositorio privacidad licencias créditos restablecer predeterminados"
    )
}

/** Visual identity of each category: a muted tint and an icon, so pages are recognizable at a glance. */
private val SettingsCategory.icon: ImageVector
    get() = when (this) {
        SettingsCategory.APPEARANCE -> Icons.Default.Palette
        SettingsCategory.PLAYBACK -> Icons.Default.PlayCircle
        SettingsCategory.LIBRARY -> Icons.Default.LibraryMusic
        SettingsCategory.LYRICS -> Icons.Default.Subtitles
        SettingsCategory.QUEUE -> Icons.Default.QueueMusic
        SettingsCategory.AUDIO -> Icons.Default.GraphicEq
        SettingsCategory.STORAGE -> Icons.Default.Security
        SettingsCategory.ABOUT -> Icons.Default.Info
    }

private val SettingsCategory.tint: Color
    get() = when (this) {
        SettingsCategory.APPEARANCE -> VinylPalette.Gold
        SettingsCategory.PLAYBACK -> Color(0xFF7DD3A7)
        SettingsCategory.LIBRARY -> Color(0xFF8AB4F8)
        SettingsCategory.LYRICS -> Color(0xFFF28B82)
        SettingsCategory.QUEUE -> Color(0xFFC58AF9)
        SettingsCategory.AUDIO -> Color(0xFF78D9EC)
        SettingsCategory.STORAGE -> Color(0xFFA8B4C4)
        SettingsCategory.ABOUT -> Color(0xFFBDBDBD)
    }

/** Root page sections. */
private val settingsSections = listOf(
    "Experiencia" to listOf(SettingsCategory.APPEARANCE, SettingsCategory.PLAYBACK, SettingsCategory.QUEUE, SettingsCategory.LYRICS),
    "Música y sonido" to listOf(SettingsCategory.LIBRARY, SettingsCategory.AUDIO),
    "Sistema" to listOf(SettingsCategory.STORAGE, SettingsCategory.ABOUT)
)

/** Live summary shown under each category on the root page. */
private fun SettingsCategory.liveSummary(settings: AppSettings, songCount: Int): String = when (this) {
    SettingsCategory.APPEARANCE -> "${settings.themeMode.title} · ${settings.accentColor.title}"
    SettingsCategory.PLAYBACK -> "Repetición: ${settings.repeatMode.title.lowercase()}" +
        if (settings.shuffleEnabled) " · Aleatorio" else ""
    SettingsCategory.QUEUE -> settings.queueAddBehavior.title
    SettingsCategory.LYRICS -> if (!settings.lyricsEnabled) "Letras ocultas"
        else if (settings.lyricsTranslationEnabled) "Traducción al ${settings.translationLanguage.title.lowercase()}" else "Sin traducción"
    SettingsCategory.LIBRARY -> "$songCount canciones · Orden por ${settings.librarySortOrder.title.lowercase()}"
    SettingsCategory.AUDIO -> if (settings.equalizerEnabled) "Ecualizador activado" else "Ecualizador desactivado"
    SettingsCategory.STORAGE -> summary
    SettingsCategory.ABOUT -> summary
}

@Composable
fun SettingsScreen(
    category: SettingsCategory?,
    onCategorySelected: (SettingsCategory?) -> Unit,
    onBack: () -> Unit,
    settings: AppSettings,
    songCount: Int,
    isLibraryLoading: Boolean,
    lastLibraryScanAt: Long?,
    errorMessage: String?,
    onSettingsChange: (AppSettings) -> Unit,
    onRefreshLibrary: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onClearPendingQueue: () -> Unit,
    onRestoreAppearance: () -> Unit,
    onResetAll: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var permissionRevision by remember { mutableStateOf(0) }
    var equalizerSupported by remember {
        mutableStateOf(
            context.getSharedPreferences(SettingsPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean("eq_supported", false)
        )
    }
    DisposableEffect(context) {
        val preferences = context.getSharedPreferences(SettingsPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "eq_supported") equalizerSupported = preferences.getBoolean("eq_supported", false)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionRevision++ }
    // Each page starts unfiltered; the search box filters the page you are on.
    LaunchedEffect(category) { query = "" }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (category != null) onCategorySelected(null) else onBack() }) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = if (category == null) "Volver" else "Volver a configuración",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            AnimatedVisibility(visible = category != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "Configuración",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        AnimatedContent(
            targetState = category,
            transitionSpec = {
                val forward = targetState != null
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { if (forward) it / 6 else -it / 6 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { if (forward) -it / 8 else it / 8 })
            },
            label = "settingsPage",
            modifier = Modifier.weight(1f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VinylSpacing.lg)
            ) {
                if (page == null) {
                    Text(
                        "Configuración",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(start = VinylSpacing.xxs)
                    )
                } else {
                    CategoryHeader(page)
                }
                VinylSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = if (page == null) "Buscar en configuración" else "Buscar en ${page.title.lowercase()}",
                    modifier = Modifier.fillMaxWidth()
                )
                if (page == null) {
                    RootCategories(query, settings, songCount, onCategorySelected)
                    Text(
                        "VinylPlayer · ${appVersion(context)}",
                        modifier = Modifier.fillMaxWidth().padding(bottom = VinylSpacing.lg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val pageQuery = if (page.title.contains(query, ignoreCase = true)) "" else query
                    when (page) {
                        SettingsCategory.APPEARANCE -> AppearanceSettings(settings, pageQuery, onSettingsChange, onRestoreAppearance)
                        SettingsCategory.PLAYBACK -> PlaybackSettings(settings, pageQuery, onSettingsChange, onStartSleepTimer, onCancelSleepTimer)
                        SettingsCategory.LIBRARY -> LibrarySettings(settings, songCount, isLibraryLoading, lastLibraryScanAt, errorMessage, pageQuery, onSettingsChange, onRefreshLibrary)
                        SettingsCategory.LYRICS -> LyricsSettings(settings, pageQuery, onSettingsChange)
                        SettingsCategory.QUEUE -> QueueSettings(settings, pageQuery, onSettingsChange) { showClearConfirmation = true }
                        SettingsCategory.AUDIO -> AudioSettings(settings, equalizerSupported, pageQuery, onSettingsChange, onOpenEqualizer)
                        SettingsCategory.STORAGE -> StorageSettings(context, pageQuery, permissionRevision, onRequestAudioPermission)
                        SettingsCategory.ABOUT -> AboutSettings(context, pageQuery) { showResetConfirmation = true }
                    }
                    Spacer(Modifier.height(VinylSpacing.xl))
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Limpiar pendientes") },
            text = { Text("Se quitarán de la fila las canciones que todavía no se reproducen. La canción actual y los archivos se conservarán.") },
            confirmButton = {
                TextButton(onClick = { onClearPendingQueue(); showClearConfirmation = false }) { Text("Limpiar", color = VinylPalette.Danger) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Cancelar") } }
        )
    }
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Restablecer ajustes") },
            text = { Text("Se restablecerán las preferencias de VinylPlayer. La música, la fila, los favoritos, la biblioteca y las letras guardadas se conservarán.") },
            confirmButton = {
                TextButton(onClick = { onResetAll(); showResetConfirmation = false }) { Text("Restablecer", color = VinylPalette.Danger) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun RootCategories(
    query: String,
    settings: AppSettings,
    songCount: Int,
    onCategorySelected: (SettingsCategory?) -> Unit
) {
    if (query.isNotBlank()) {
        val matching = SettingsCategory.entries.filter { item ->
            "$item ${item.title} ${item.summary} ${item.searchableText}".contains(query, ignoreCase = true)
        }
        if (matching.isEmpty()) {
            EmptySearch()
        } else {
            Column {
                SectionLabel("Resultados")
                GroupedSurface {
                    matching.forEachIndexed { index, item ->
                        if (index > 0) GroupDivider(startIndent = 68.dp)
                        CategoryRow(item, item.summary) { onCategorySelected(item) }
                    }
                }
            }
        }
        return
    }
    settingsSections.forEach { (title, categories) ->
        Column {
            SectionLabel(title)
            GroupedSurface {
                categories.forEachIndexed { index, item ->
                    if (index > 0) GroupDivider(startIndent = 68.dp)
                    CategoryRow(item, item.liveSummary(settings, songCount)) { onCategorySelected(item) }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(item: SettingsCategory, summary: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(onClick = onClick)
            .padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        IconBadge(item.icon, item.tint)
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryHeader(category: SettingsCategory) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VinylSpacing.md)) {
        IconBadge(category.icon, category.tint, size = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(category.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(category.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptySearch() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = VinylSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(VinylSpacing.sm))
        Text("Sin resultados", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Prueba con otro nombre de ajuste.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Category pages
// ---------------------------------------------------------------------------------------------

@Composable
private fun AppearanceSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit, onRestore: () -> Unit) {
    if (matches(query, "Tema", "Oscuro", "Claro", "Según el sistema", "Color de acento", "Dorado", "Rojo", "Personalizado", "hexadecimal")) {
        SettingsGroup("Tema y color") {
            if (matches(query, "Tema", "Oscuro", "Claro", "Según el sistema")) {
                SegmentedSetting(
                    "Tema", "Elige el aspecto de VinylPlayer.",
                    ThemeMode.entries, settings.themeMode, { it.title },
                    icon = {
                        when (it) {
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                    }
                ) { onChange(settings.copy(themeMode = it)) }
            }
            if (matches(query, "Color de acento", "Dorado", "Rojo", "Personalizado", "hexadecimal")) {
                AccentSwatches(settings, onChange)
                AnimatedVisibility(settings.accentColor == AccentColor.CUSTOM) { CustomAccentPicker(settings, onChange) }
            }
        }
    }
    if (matches(query, "Fondo del reproductor", "Portada desenfocada", "Color sólido", "Colores de portada", "Animar brazo fonocaptor", "brazo", "vinilo")) {
        SettingsGroup("Reproductor") {
            if (matches(query, "Fondo del reproductor", "Portada desenfocada", "Color sólido", "Colores de portada")) {
                SegmentedSetting(
                    "Fondo del reproductor", "Desenfoque, color sólido o tonos extraídos de la portada.",
                    PlayerBackground.entries, settings.playerBackground, { it.title }
                ) { onChange(settings.copy(playerBackground = it)) }
            }
            if (matches(query, "Animar brazo fonocaptor", "brazo", "vinilo")) {
                SettingSwitch(
                    "Animar brazo fonocaptor",
                    if (settings.tonearmAnimationEnabled) "Se retira al pausar y baja al reproducir." else "Permanece sobre el vinilo al pausar.",
                    settings.tonearmAnimationEnabled
                ) { onChange(settings.copy(tonearmAnimationEnabled = it)) }
            }
        }
    }
    if (matches(query, "Tamaño de las letras", "tipografía", "letras")) {
        SettingsGroup("Letras") {
            SettingNote("Tamaño y estilo de las letras", "Se ajustan en Letras y traducción.", icon = Icons.Default.Subtitles)
        }
    }
    if (matches(query, "Restaurar apariencia", "predeterminada")) {
        SettingsGroup("Restablecer") {
            ActionSetting("Restaurar apariencia predeterminada", "Tema oscuro, acento dorado y brazo animado.", "Restaurar", onRestore)
        }
    }
}

@Composable
private fun PlaybackSettings(
    settings: AppSettings,
    query: String,
    onChange: (AppSettings) -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    if (matches(query, "Reanudar última sesión", "canción", "posición", "Reproducción automática", "siguiente canción", "avance automático")) {
        SettingsGroup("Continuidad") {
            if (matches(query, "Reanudar última sesión", "canción", "posición")) {
                SettingSwitch("Reanudar última sesión", "Recuerda la canción y la posición entre aperturas.", settings.resumeLastSession) {
                    onChange(settings.copy(resumeLastSession = it))
                }
            }
            if (matches(query, "Reproducción automática", "siguiente canción", "avance automático")) {
                SettingSwitch("Avance automático", "Continúa con la siguiente canción al terminar.", settings.autoAdvance) {
                    onChange(settings.copy(autoAdvance = it))
                }
            }
        }
    }
    if (matches(query, "Repetición", "desactivado", "toda la fila", "una canción", "Aleatorio", "shuffle")) {
        SettingsGroup("Orden de reproducción") {
            if (matches(query, "Repetición", "desactivado", "toda la fila", "una canción")) {
                SegmentedSetting("Repetición", "También lo controla el botón de repetir.", RepeatMode.entries, settings.repeatMode, { it.title }) {
                    onChange(settings.copy(repeatMode = it))
                }
            }
            if (matches(query, "Aleatorio", "shuffle")) {
                SettingSwitch("Reproducción aleatoria", "El orden aleatorio se conserva entre sesiones.", settings.shuffleEnabled) {
                    onChange(settings.copy(shuffleEnabled = it))
                }
            }
        }
    }
    if (matches(query, "Temporizador", "apagar", "pausar")) {
        SettingsGroup("Temporizador de apagado") {
            OptionPills(
                title = "Pausar después de",
                description = "Elige una duración para iniciar el temporizador.",
                options = listOf(15, 30, 45, 60),
                selected = null,
                label = { "$it min" },
                icon = Icons.Default.Timer,
                onSelect = onStartTimer
            )
            ActionSetting("Cancelar temporizador", "Detiene el temporizador activo.", "Cancelar", onCancelTimer)
        }
    }
    if (matches(query, "Auriculares", "desconexión")) {
        SettingsGroup("Próximamente") {
            SettingNote("Pausa al desconectar auriculares", "Pendiente: el reproductor todavía no detecta la desconexión de auriculares.")
        }
    }
}

@Composable
private fun LibrarySettings(
    settings: AppSettings,
    songCount: Int,
    loading: Boolean,
    lastScanAt: Long?,
    error: String?,
    query: String,
    onChange: (AppSettings) -> Unit,
    onRefresh: () -> Unit
) {
    if (matches(query, "Actualizar biblioteca", "escanear", "canciones", "último escaneo")) {
        SettingsGroup("Estado") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("$songCount", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("canciones indexadas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (loading) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                when {
                    loading -> "Actualizando biblioteca…"
                    error != null -> error
                    lastScanAt != null -> "Último escaneo: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastScanAt))}"
                    else -> "Toca Actualizar para leer tu música."
                },
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = VinylSpacing.md)
            )
            ActionSetting("Actualizar biblioteca", "Vuelve a leer los archivos de audio permitidos.", if (loading) "Actualizando" else "Actualizar", onRefresh, enabled = !loading)
        }
    }
    if (matches(query, "Orden predeterminado", "título", "artista", "álbum", "Vista", "cuadrícula", "lista", "artistas", "álbumes")) {
        SettingsGroup("Organización") {
            if (matches(query, "Orden predeterminado", "título", "artista", "álbum")) {
                SegmentedSetting("Orden predeterminado", "Se aplica a la lista de canciones.", LibrarySortOrder.entries, settings.librarySortOrder, { it.title }) {
                    onChange(settings.copy(librarySortOrder = it))
                }
            }
            if (matches(query, "Vista", "cuadrícula", "lista", "álbumes", "álbum")) {
                SegmentedSetting("Vista de álbumes", "También se cambia desde la pestaña Álbumes.", CollectionViewMode.entries, settings.albumViewMode, { it.title }) {
                    onChange(settings.copy(albumViewMode = it))
                }
            }
            if (matches(query, "Vista", "cuadrícula", "lista", "artistas", "artista")) {
                SegmentedSetting("Vista de artistas", "También se cambia desde la pestaña Artistas.", CollectionViewMode.entries, settings.artistViewMode, { it.title }) {
                    onChange(settings.copy(artistViewMode = it))
                }
            }
        }
    }
    if (matches(query, "fecha", "más recientes", "Carpetas", "incluir", "excluir")) {
        SettingsGroup("Próximamente") {
            if (matches(query, "fecha", "más recientes")) {
                SettingNote("Orden por fecha", "Pendiente: esta preferencia todavía no está integrada.")
            }
            if (matches(query, "Carpetas", "incluir", "excluir")) {
                SettingNote("Carpetas incluidas y excluidas", "Pendiente: requiere seleccionar carpetas con el acceso seguro de Android.")
            }
        }
    }
}

@Composable
private fun LyricsSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit) {
    if (matches(query, "Mostrar letras", "visibilidad", "Traducción", "activar", "idioma")) {
        SettingsGroup("Letras y traducción") {
            if (matches(query, "Mostrar letras", "visibilidad")) {
                SettingSwitch("Mostrar letras", "Muestra la pestaña de letra en el reproductor.", settings.lyricsEnabled) {
                    onChange(settings.copy(lyricsEnabled = it))
                }
            }
            if (matches(query, "Traducción", "activar", "idioma")) {
                SettingSwitch("Traducción", "Permite traducir letras desde la pestaña de letra.", settings.lyricsTranslationEnabled) {
                    onChange(settings.copy(lyricsTranslationEnabled = it))
                }
                AnimatedVisibility(settings.lyricsTranslationEnabled) {
                    OptionPills(
                        "Idioma de traducción", "Cada traducción guardada recuerda su idioma.",
                        TranslationLanguage.entries, settings.translationLanguage, { it.title }
                    ) { onChange(settings.copy(translationLanguage = it)) }
                }
            }
        }
    }
    if (matches(query, "Tamaño de letra", "tipografía", "texto", "negrita", "peso", "Líneas anteriores", "líneas siguientes", "contexto")) {
        SettingsGroup("Visualización") {
            if (matches(query, "Tamaño de letra", "tipografía", "texto", "negrita", "peso")) {
                SliderSetting("Tamaño base", "${settings.lyricsFontSizeSp} sp", settings.lyricsFontSizeSp.toFloat(), 16f..40f, 23) {
                    onChange(settings.copy(lyricsFontSizeSp = it.toInt()))
                }
                LyricsPreview(settings)
                SettingSwitch("Usar negrita", "Resalta la línea activa y da más peso a las líneas cercanas.", settings.lyricsBoldText) {
                    onChange(settings.copy(lyricsBoldText = it))
                }
            }
            if (matches(query, "Líneas anteriores", "líneas siguientes", "contexto")) {
                SliderSetting("Líneas anteriores", settings.lyricsPreviousLines.toString(), settings.lyricsPreviousLines.toFloat(), 0f..8f, 7) {
                    onChange(settings.copy(lyricsPreviousLines = it.toInt()))
                }
                SliderSetting("Líneas siguientes", settings.lyricsNextLines.toString(), settings.lyricsNextLines.toFloat(), 0f..12f, 11) {
                    onChange(settings.copy(lyricsNextLines = it.toInt()))
                }
            }
        }
    }
    if (matches(query, "Desfase global", "sincronización", "offset")) {
        SettingsGroup("Sincronización global") {
            val seconds = settings.globalLyricsOffsetMs / 1_000f
            SliderSetting("Desfase global", "%+.1f s".format(seconds), seconds, -600f..600f, 239) {
                onChange(settings.copy(globalLyricsOffsetMs = (it * 1_000).toLong()))
            }
            SettingNote("Prioridad", "Los desfases guardados por canción prevalecen sobre este valor.", icon = Icons.Default.Info)
            ActionSetting("Restablecer desfase global", "No modifica los ajustes individuales.", "Restablecer", onClick = {
                onChange(settings.copy(globalLyricsOffsetMs = 0L))
            })
        }
    }
}

/** Non-interactive sample that reflects the size and weight chosen above. */
@Composable
private fun LyricsPreview(settings: AppSettings) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md).clip(VinylShapes.control)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(VinylSpacing.md),
        verticalArrangement = Arrangement.spacedBy(VinylSpacing.xxs)
    ) {
        Text("Línea anterior", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (settings.lyricsFontSizeSp * 0.8f).sp)
        Text(
            "Línea que suena ahora",
            color = MaterialTheme.colorScheme.primary,
            fontSize = settings.lyricsFontSizeSp.sp,
            fontWeight = if (settings.lyricsBoldText) FontWeight.Bold else FontWeight.Normal,
            lineHeight = (settings.lyricsFontSizeSp * 1.2f).sp
        )
    }
}

@Composable
private fun QueueSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit, onClearPending: () -> Unit) {
    if (matches(query, "Conservar fila", "conservar cola", "sesiones", "Duplicados", "permitir duplicados")) {
        SettingsGroup("Comportamiento") {
            if (matches(query, "Conservar fila", "conservar cola", "sesiones")) {
                SettingSwitch("Conservar la fila entre sesiones", "Al desactivarlo, la fila se mantiene solo hasta cerrar la app.", settings.keepQueueBetweenSessions) {
                    onChange(settings.copy(keepQueueBetweenSessions = it))
                }
            }
            if (matches(query, "Duplicados", "permitir duplicados")) {
                SettingSwitch("Permitir duplicados", "Permite agregar una canción más de una vez a la fila.", settings.allowQueueDuplicates) {
                    onChange(settings.copy(allowQueueDuplicates = it))
                }
            }
        }
    }
    if (matches(query, "Acción predeterminada", "agregar al final", "reproducir a continuación")) {
        SettingsGroup("Al agregar canciones") {
            SegmentedSetting("Acción predeterminada", "Afecta a la opción Agregar a la fila.", QueueAddBehavior.entries, settings.queueAddBehavior, { it.title }) {
                onChange(settings.copy(queueAddBehavior = it))
            }
        }
    }
    if (matches(query, "Limpiar canciones pendientes", "limpiar", "pendientes")) {
        SettingsGroup("Acciones") {
            ActionSetting("Limpiar canciones pendientes", "Conserva la canción que se está reproduciendo.", "Limpiar", onClearPending, destructive = true)
        }
    }
    if (matches(query, "Nuevo álbum o lista", "comportamiento")) {
        SettingsGroup("Próximamente") {
            SettingNote("Al abrir otra lista o álbum", "Pendiente: el comportamiento actual reemplaza la fila desde la biblioteca.")
        }
    }
}

@Composable
private fun AudioSettings(
    settings: AppSettings,
    supported: Boolean,
    query: String,
    onChange: (AppSettings) -> Unit,
    onOpenEqualizer: () -> Unit
) {
    if (matches(query, "Ecualizador", "activar", "desactivar", "bandas", "preajustes")) {
        SettingsGroup("Ecualizador") {
            if (supported) {
                SettingSwitch("Ecualizador", "Activa las bandas de audio integradas.", settings.equalizerEnabled) {
                    onChange(settings.copy(equalizerEnabled = it))
                }
                ActionSetting("Bandas y preajustes", "Ajusta las cinco bandas disponibles.", "Abrir", onOpenEqualizer)
            } else {
                SettingNote("Ecualizador no disponible", "El dispositivo no confirmó compatibilidad con el efecto de audio.", icon = Icons.Default.ErrorOutline)
            }
        }
    }
    if (matches(query, "Normalización", "graves", "efectos")) {
        SettingsGroup("Próximamente") {
            SettingNote("Normalización y otros efectos", "Pendiente: no están disponibles en el motor de audio actual.")
        }
    }
}

@Composable
private fun StorageSettings(context: Context, query: String, permissionRevision: Int, onRequestPermission: () -> Unit) {
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val audioGranted = remember(permissionRevision, audioPermission) {
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
    }
    val notificationsGranted = remember(permissionRevision) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    SettingsGroup("Permisos") {
        if (matches(query, "Audio", "biblioteca", "lectura", "música")) {
            PermissionRow("Acceso a música", "Necesario para leer tu biblioteca.", audioGranted)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && matches(query, "Notificaciones", "reproducción")) {
            PermissionRow("Notificaciones", "Controles de reproducción fuera de la app.", notificationsGranted)
        }
        if ((!audioGranted || !notificationsGranted) && matches(query, "Solicitar permisos", "conceder")) {
            ActionSetting("Solicitar permisos", "Android mostrará los accesos que necesita VinylPlayer.", "Solicitar", onRequestPermission)
        }
        if ((!audioGranted || !notificationsGranted) && matches(query, "Ajustes de la aplicación", "sistema")) {
            val openSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            ActionSetting("Ajustes de la aplicación", "Abre los permisos de VinylPlayer en Android.", "Abrir", openSettings)
        }
    }
    if (matches(query, "Carpetas", "diagnóstico", "tamaño de biblioteca")) {
        SettingsGroup("Próximamente") {
            SettingNote("Accesos a carpetas", "Pendiente: todavía no se administran permisos SAF por carpeta.")
        }
    }
}

@Composable
private fun AboutSettings(context: Context, query: String, onReset: () -> Unit) {
    if (matches(query, "Versión", "compilación", "nombre", "Repositorio", "GitHub")) {
        SettingsGroup("VinylPlayer") {
            if (matches(query, "Versión", "compilación", "nombre")) {
                SettingNote("Versión", "${appVersion(context)} · ${context.packageName}", icon = Icons.Default.Info)
            }
            if (matches(query, "Repositorio", "GitHub")) {
                ActionSetting("Repositorio GitHub", "Código fuente de VinylPlayer.", "Abrir", onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChristopherMendoz/VinylPlayer")))
                })
            }
        }
    }
    if (matches(query, "Créditos", "licencias", "Compose", "Media3", "Room", "Jaudiotagger")) {
        SettingsGroup("Créditos y licencias") {
            SettingNote("Tecnologías", "Android, Jetpack Compose, Media3, Room y Jaudiotagger (LGPL-2.1-or-later).", icon = Icons.Default.Code)
            ActionSetting("Licencia de Jaudiotagger", "Atribución y licencia del escritor de etiquetas.", "Ver", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kaned1as/jaudiotagger/blob/master/license.txt")))
            })
        }
    }
    if (matches(query, "Privacidad", "contacto", "historial")) {
        SettingsGroup("Próximamente") {
            SettingNote("Privacidad, contacto e historial", "Pendiente: el proyecto no define enlaces oficiales para estas opciones.")
        }
    }
    if (matches(query, "Restablecer ajustes", "predeterminados")) {
        SettingsGroup("Restablecer") {
            ActionSetting("Restablecer todos los ajustes", "No elimina música, favoritos, fila, biblioteca ni letras.", "Restablecer", onReset, destructive = true)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Building blocks
// ---------------------------------------------------------------------------------------------

/** Section label + grouped surface. Rows inside use the same horizontal padding. */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title)
        GroupedSurface(Modifier.padding(vertical = 0.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = VinylSpacing.xs), verticalArrangement = Arrangement.spacedBy(VinylSpacing.xxs)) {
                content()
            }
        }
    }
}

@Composable
private fun RowTexts(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (description.isNotBlank()) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts(title, description, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, colors = vinylSwitchColors())
    }
}

/** Equal-width segmented control for short option sets. */
@Composable
private fun <T> SegmentedSetting(
    title: String,
    description: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    onSelect: (T) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts(title, description)
        Row(
            Modifier.fillMaxWidth().clip(VinylShapes.control).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val background by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, tween(200), label = "segment"
                )
                val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Column(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp)).background(background)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    icon?.let { Icon(it(option), null, tint = content, modifier = Modifier.size(18.dp)) }
                    Text(
                        label(option),
                        color = content,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/** Wrapping pills for longer option lists (languages) or one-shot choices (timer). */
@Composable
private fun <T> OptionPills(
    title: String,
    description: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    icon: ImageVector? = null,
    onSelect: (T) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts(title, description)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(VinylSpacing.xs)) {
            options.forEach { option ->
                LibraryPill(label(option), selected = option == selected, icon = icon) { onSelect(option) }
            }
        }
    }
}

/** Accent color as real swatches: gold, red or a custom color. */
@Composable
private fun AccentSwatches(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts("Color de acento", "Se usa en botones, selección y la canción que suena.")
        Row(horizontalArrangement = Arrangement.spacedBy(VinylSpacing.lg)) {
            AccentColor.entries.forEach { accent ->
                val swatch = when (accent) {
                    AccentColor.GOLD -> VinylPalette.Gold
                    AccentColor.RED -> VinylPalette.Red
                    AccentColor.CUSTOM -> Color(settings.customAccentArgb ?: 0xFF7DD3A7.toInt())
                }
                val isSelected = settings.accentColor == accent
                Column(
                    Modifier.clip(VinylShapes.control)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onChange(settings.copy(accentColor = accent)) }
                        .padding(VinylSpacing.xxs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(48.dp)
                            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .padding(5.dp).clip(CircleShape)
                            .background(
                                if (accent == AccentColor.CUSTOM && settings.customAccentArgb == null) {
                                    Brush.sweepGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF7DD3A7), Color(0xFF8AB4F8), Color(0xFFC58AF9), Color(0xFFFF6B6B)))
                                } else SolidColor(swatch)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color(AccentColorValue.highContrastForeground(swatch.toArgb(), Color.Black.toArgb())), modifier = Modifier.size(20.dp))
                    }
                    Text(
                        accent.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAccentPicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    val selected = settings.customAccentArgb ?: 0xFFE4B45B.toInt()
    val hsl = AccentColorValue.toHsl(selected)
    var hex by remember { mutableStateOf(AccentColorValue.toHex(selected)) }
    var hexFocused by remember { mutableStateOf(false) }
    LaunchedEffect(selected, hexFocused) {
        if (!hexFocused) hex = AccentColorValue.toHex(selected)
    }

    Column(verticalArrangement = Arrangement.spacedBy(VinylSpacing.xxs)) {
        SliderSetting("Tono", "${hsl.hue.toInt()}°", hsl.hue, 0f..360f, 359) { hue ->
            onChange(settings.copy(accentColor = AccentColor.CUSTOM, customAccentArgb = AccentColorValue.fromHsl(hue, hsl.saturation, hsl.lightness)))
        }
        SliderSetting("Saturación", "${(hsl.saturation * 100).toInt()}%", hsl.saturation, 0f..1f, 19) { saturation ->
            onChange(settings.copy(accentColor = AccentColor.CUSTOM, customAccentArgb = AccentColorValue.fromHsl(hsl.hue, saturation, hsl.lightness)))
        }
        SliderSetting("Luminosidad", "${(hsl.lightness * 100).toInt()}%", hsl.lightness, 0f..1f, 19) { lightness ->
            onChange(settings.copy(accentColor = AccentColor.CUSTOM, customAccentArgb = AccentColorValue.fromHsl(hsl.hue, hsl.saturation, lightness)))
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { value ->
                hex = value
                AccentColorValue.parseHex(value)?.let { color ->
                    onChange(settings.copy(accentColor = AccentColor.CUSTOM, customAccentArgb = color))
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md).onFocusChanged { hexFocused = it.isFocused },
            singleLine = true,
            shape = VinylShapes.control,
            leadingIcon = { Box(Modifier.size(20.dp).clip(CircleShape).background(Color(selected))) },
            label = { Text("Código hexadecimal") },
            supportingText = { Text("Usa #RRGGBB o #AARRGGBB") }
        )
    }
}

@Composable
private fun SliderSetting(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xxs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                valueLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

/** Row whose trailing pill runs the action; the whole row is also tappable. */
@Composable
private fun ActionSetting(
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val accent = if (destructive) VinylPalette.Danger else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts(title, description, Modifier.weight(1f))
        Text(
            action,
            color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clip(CircleShape)
                .background(if (enabled) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 * Informational row. Notes starting with "Pendiente:" are shown as upcoming features with a
 * "Próximamente" badge and no interaction.
 */
@Composable
private fun SettingNote(title: String, description: String, icon: ImageVector? = null) {
    val pending = description.startsWith("Pendiente")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        Icon(
            icon ?: if (pending) Icons.Default.Schedule else Icons.Default.Info,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VinylSpacing.xs)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (pending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (pending) StatusBadge("Próximamente", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                description.removePrefix("Pendiente:").trim().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = VinylSpacing.md, vertical = VinylSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VinylSpacing.sm)
    ) {
        RowTexts(title, description, Modifier.weight(1f))
        StatusBadge(
            if (granted) "Concedido" else "No concedido",
            if (granted) Color(0xFF7DD3A7) else VinylPalette.Danger,
            icon = if (granted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color, icon: ImageVector? = null) {
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon?.let { Icon(it, null, tint = color, modifier = Modifier.size(14.dp)) }
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private fun matches(query: String, vararg values: String): Boolean =
    query.isBlank() || values.any { it.contains(query, ignoreCase = true) }

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = runCatching {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode
    "${packageInfo.versionName} ($build)"
}.getOrDefault("Versión no disponible")
