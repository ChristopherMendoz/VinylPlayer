package com.chrismdz.vinylplayer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chrismdz.vinylplayer.settings.AccentColor
import com.chrismdz.vinylplayer.settings.AccentColorValue
import com.chrismdz.vinylplayer.settings.AppSettings
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
    val pageQuery = if (category?.title?.contains(query, ignoreCase = true) == true) "" else query

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (category != null) onCategorySelected(null) else onBack() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = if (category == null) "Volver" else "Volver a ajustes",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                category?.title ?: "Configuración",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Buscar ajustes") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda") } }
            } else null
        )

        if (category == null) {
            val matchingCategories = SettingsCategory.entries.filter { item ->
                query.isBlank() || "$item ${item.title} ${item.summary} ${item.searchableText}"
                    .contains(query, ignoreCase = true)
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (query.isNotBlank() && matchingCategories.isEmpty()) {
                    SettingNote("Sin resultados", "Prueba con otro nombre de ajuste.")
                }
                matchingCategories.forEachIndexed { index, item ->
                    Card(
                        onClick = { onCategorySelected(item) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("%02d".format(index + 1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(item.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(
                    "VinylPlayer · ${appVersion(context)}",
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (category) {
                    SettingsCategory.APPEARANCE -> AppearanceSettings(settings, pageQuery, onSettingsChange, onRestoreAppearance)
                    SettingsCategory.PLAYBACK -> PlaybackSettings(settings, pageQuery, onSettingsChange, onStartSleepTimer, onCancelSleepTimer)
                    SettingsCategory.LIBRARY -> LibrarySettings(settings, songCount, isLibraryLoading, lastLibraryScanAt, errorMessage, pageQuery, onSettingsChange, onRefreshLibrary)
                    SettingsCategory.LYRICS -> LyricsSettings(settings, pageQuery, onSettingsChange)
                    SettingsCategory.QUEUE -> QueueSettings(settings, pageQuery, onSettingsChange) { showClearConfirmation = true }
                    SettingsCategory.AUDIO -> AudioSettings(settings, equalizerSupported, pageQuery, onSettingsChange, onOpenEqualizer)
                    SettingsCategory.STORAGE -> StorageSettings(context, pageQuery, permissionRevision, onRequestAudioPermission)
                    SettingsCategory.ABOUT -> AboutSettings(context, pageQuery) { showResetConfirmation = true }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Limpiar pendientes") },
            text = { Text("Se quitarán de la fila las canciones que todavía no se reproducen. La canción actual y los archivos se conservarán.") },
            confirmButton = {
                TextButton(onClick = { onClearPendingQueue(); showClearConfirmation = false }) { Text("Limpiar") }
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
                TextButton(onClick = { onResetAll(); showResetConfirmation = false }) { Text("Restablecer") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AppearanceSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit, onRestore: () -> Unit) {
    SettingsGroup("Personalización") {
        if (matches(query, "Tema", "Oscuro", "Claro", "Según el sistema")) {
            ChoiceSetting("Tema", "Elige el aspecto de VinylPlayer.", ThemeMode.entries, settings.themeMode, { it.title }) {
                onChange(settings.copy(themeMode = it))
            }
        }
        if (matches(query, "Color de acento", "Dorado", "Rojo", "Personalizado", "hexadecimal")) {
            ChoiceSetting("Color de acento", "Elige Dorado, Rojo o cualquier color personalizado.", AccentColor.entries, settings.accentColor, { it.title }) {
                onChange(settings.copy(accentColor = it))
            }
            if (settings.accentColor == AccentColor.CUSTOM) CustomAccentPicker(settings, onChange)
        }
        if (matches(query, "Fondo del reproductor", "Portada desenfocada", "Color sólido", "Colores de portada")) {
            ChoiceSetting("Fondo del reproductor", "Elige desenfoque, color sólido o tonos extraídos de la portada.", PlayerBackground.entries, settings.playerBackground, { it.title }) {
                onChange(settings.copy(playerBackground = it))
            }
        }
        if (matches(query, "Animar brazo fonocaptor", "brazo", "vinilo")) {
            SettingSwitch(
                "Animar brazo fonocaptor",
                if (settings.tonearmAnimationEnabled) "Se retira al pausar y baja al reproducir." else "Permanece sobre el vinilo al pausar.",
                settings.tonearmAnimationEnabled
            ) { onChange(settings.copy(tonearmAnimationEnabled = it)) }
        }
        if (matches(query, "Tamaño de las letras", "tipografía", "letras")) {
            Text("El tamaño y estilo de las letras se ajustan en Letras y traducción.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (matches(query, "Restaurar apariencia", "predeterminada")) {
            ActionSetting("Restaurar apariencia predeterminada", "Vuelve al tema oscuro, acento dorado y brazo animado.", "Restaurar", onRestore)
        }
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), RoundedCornerShape(14.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Column {
                Text("Vista previa", fontWeight = FontWeight.SemiBold)
                Text("El tema y el acento se aplican a toda la interfaz.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
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
    SettingsGroup("Comportamiento") {
        if (matches(query, "Reanudar última sesión", "canción", "posición")) {
            SettingSwitch("Reanudar última sesión", "Recuerda la canción y posición entre aperturas.", settings.resumeLastSession) {
                onChange(settings.copy(resumeLastSession = it))
            }
        }
        if (matches(query, "Reproducción automática", "siguiente canción", "avance automático")) {
            SettingSwitch("Avance automático", "Continúa con la siguiente canción al terminar.", settings.autoAdvance) {
                onChange(settings.copy(autoAdvance = it))
            }
        }
        if (matches(query, "Repetición", "desactivado", "toda la fila", "una canción")) {
            ChoiceSetting("Repetición", "Modo que también controla el botón de repetir.", RepeatMode.entries, settings.repeatMode, { it.title }) {
                onChange(settings.copy(repeatMode = it))
            }
        }
        if (matches(query, "Aleatorio", "shuffle")) {
            SettingSwitch("Reproducción aleatoria", "El orden aleatorio se conserva entre sesiones.", settings.shuffleEnabled) {
                onChange(settings.copy(shuffleEnabled = it))
            }
        }
    }
    if (matches(query, "Temporizador", "apagar", "pausar")) {
        SettingsGroup("Temporizador de apagado") {
            Text("Pausa la reproducción después del tiempo elegido.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            ChoiceSetting("Duración", "", listOf(15, 30, 45, 60), null, { "$it min" }) { onStartTimer(it) }
            ActionSetting("Cancelar temporizador", "", "Cancelar", onCancelTimer)
        }
    }
    if (matches(query, "Auriculares", "desconexión")) {
        SettingNote("Pausa al desconectar auriculares", "Pendiente: el reproductor todavía no detecta la desconexión de auriculares.")
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
    SettingsGroup("Biblioteca") {
        if (matches(query, "Actualizar biblioteca", "escanear", "canciones", "último escaneo")) {
            Text("$songCount canciones indexadas", fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    loading -> "Actualizando biblioteca…"
                    error != null -> error
                    lastScanAt != null -> "Último escaneo: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(lastScanAt))}"
                    else -> "La biblioteca se actualizará al abrir VinylPlayer."
                },
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            ActionSetting("Actualizar biblioteca", "Vuelve a leer los archivos de audio permitidos.", if (loading) "Actualizando" else "Actualizar", onRefresh, enabled = !loading)
        }
        if (matches(query, "Orden predeterminado", "título", "artista", "álbum")) {
            ChoiceSetting("Orden predeterminado", "Se aplica a la lista de canciones.", LibrarySortOrder.entries, settings.librarySortOrder, { it.title }) {
                onChange(settings.copy(librarySortOrder = it))
            }
        }
        if (matches(query, "fecha", "más recientes")) {
            SettingNote("Orden por fecha", "Pendiente: esta preferencia todavía no está integrada.")
        }
        if (matches(query, "Carpetas", "incluir", "excluir")) {
            SettingNote("Carpetas incluidas y excluidas", "Pendiente: requiere seleccionar carpetas con el acceso seguro de Android.")
        }
    }
}

@Composable
private fun LyricsSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit) {
    SettingsGroup("Visualización") {
        if (matches(query, "Mostrar letras", "visibilidad")) {
            SettingSwitch("Mostrar letras", "Muestra la pestaña de letra en el reproductor.", settings.lyricsEnabled) {
                onChange(settings.copy(lyricsEnabled = it))
            }
        }
        if (matches(query, "Traducción", "activar", "idioma")) {
            SettingSwitch("Traducción", "Permite traducir letras al mostrar la pestaña de letra.", settings.lyricsTranslationEnabled) {
                onChange(settings.copy(lyricsTranslationEnabled = it))
            }
            if (settings.lyricsTranslationEnabled) {
                ChoiceSetting("Idioma de traducción", "El idioma se guarda junto a cada traducción en caché.", TranslationLanguage.entries, settings.translationLanguage, { it.title }) {
                    onChange(settings.copy(translationLanguage = it))
                }
            }
        }
        if (matches(query, "Tamaño de letra", "tipografía", "texto", "negrita", "peso")) {
            SliderSetting("Tamaño base", "${settings.lyricsFontSizeSp} sp", settings.lyricsFontSizeSp.toFloat(), 16f..40f, 23) {
                onChange(settings.copy(lyricsFontSizeSp = it.toInt()))
            }
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
    if (matches(query, "Desfase global", "sincronización", "offset")) {
        SettingsGroup("Sincronización global") {
            val seconds = settings.globalLyricsOffsetMs / 1_000f
            SliderSetting("Desfase global", "%+.1f s".format(seconds), seconds, -600f..600f, 239) {
                onChange(settings.copy(globalLyricsOffsetMs = (it * 1_000).toLong()))
            }
            Text("Los desfases guardados por canción prevalecen sobre este valor.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            ActionSetting("Restablecer desfase global", "No modifica los ajustes individuales.", "Restablecer", onClick = {
                onChange(settings.copy(globalLyricsOffsetMs = 0L))
            })
        }
    }
}

@Composable
private fun QueueSettings(settings: AppSettings, query: String, onChange: (AppSettings) -> Unit, onClearPending: () -> Unit) {
    SettingsGroup("Conservación y acciones") {
        if (matches(query, "Conservar fila", "conservar cola", "sesiones")) {
            SettingSwitch("Conservar la fila entre sesiones", "Al desactivarlo, la fila actual se mantiene hasta cerrar la app.", settings.keepQueueBetweenSessions) {
                onChange(settings.copy(keepQueueBetweenSessions = it))
            }
        }
        if (matches(query, "Acción predeterminada", "agregar al final", "reproducir a continuación")) {
            ChoiceSetting("Acción predeterminada al agregar", "Afecta a la acción Agregar a la fila.", QueueAddBehavior.entries, settings.queueAddBehavior, { it.title }) {
                onChange(settings.copy(queueAddBehavior = it))
            }
        }
        if (matches(query, "Duplicados", "permitir duplicados")) {
            SettingSwitch("Permitir duplicados", "Permite agregar una canción más de una vez a la fila.", settings.allowQueueDuplicates) {
                onChange(settings.copy(allowQueueDuplicates = it))
            }
        }
        if (matches(query, "Limpiar canciones pendientes", "limpiar", "pendientes")) {
            ActionSetting("Limpiar canciones pendientes", "Conserva la canción que se está reproduciendo.", "Limpiar", onClearPending)
        }
        if (matches(query, "Nuevo álbum o lista", "comportamiento")) {
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
    SettingsGroup("Ecualizador") {
        if (matches(query, "Ecualizador", "activar", "desactivar")) {
            if (supported) {
                SettingSwitch("Ecualizador", "Activa las bandas de audio integradas.", settings.equalizerEnabled) {
                    onChange(settings.copy(equalizerEnabled = it))
                }
            } else {
                SettingNote("Ecualizador", "El dispositivo no confirmó compatibilidad con el efecto de audio.")
            }
            if (supported) {
                ActionSetting("Bandas y preajustes", "Ajusta las cinco bandas disponibles.", "Abrir", onOpenEqualizer)
            }
        }
        if (matches(query, "Normalización", "graves", "efectos")) {
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
    SettingsGroup("Permisos de VinylPlayer") {
        if (matches(query, "Audio", "biblioteca", "lectura")) {
            SettingNote("Acceso a música", if (audioGranted) "Concedido" else "No concedido")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && matches(query, "Notificaciones", "reproducción")) {
            SettingNote("Notificaciones", if (notificationsGranted) "Concedido" else "No concedido")
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
        if (matches(query, "Carpetas", "diagnóstico", "tamaño de biblioteca")) {
            SettingNote("Accesos a carpetas", "Pendiente: todavía no se administran permisos SAF por carpeta.")
        }
    }
}

@Composable
private fun AboutSettings(context: Context, query: String, onReset: () -> Unit) {
    SettingsGroup("VinylPlayer") {
        if (matches(query, "Versión", "compilación", "nombre")) {
            SettingNote("Versión", "${appVersion(context)} · ${context.packageName}")
        }
        if (matches(query, "Repositorio", "GitHub")) {
            ActionSetting("Repositorio GitHub", "Código fuente de VinylPlayer.", "Abrir", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ChristopherMendoz/VinylPlayer")))
            })
        }
        if (matches(query, "Créditos", "licencias", "Compose", "Media3", "Room", "Jaudiotagger")) {
            SettingNote("Créditos", "Android, Jetpack Compose, Media3, Room y Jaudiotagger (LGPL-2.1-or-later).")
            ActionSetting("Licencia de Jaudiotagger", "Atribución y licencia del escritor de etiquetas.", "Ver", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kaned1as/jaudiotagger/blob/master/license.txt")))
            })
        }
        if (matches(query, "Privacidad", "contacto", "historial")) {
            SettingNote("Privacidad, contacto e historial", "Pendiente: el proyecto no define enlaces oficiales para estas opciones.")
        }
        if (matches(query, "Restablecer ajustes", "predeterminados")) {
            ActionSetting("Restablecer todos los ajustes", "No elimina música, favoritos, fila, biblioteca ni letras.", "Restablecer", onReset)
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            content()
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).background(Color(selected), CircleShape))
            Text("Color personalizado", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
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
            modifier = Modifier.fillMaxWidth().onFocusChanged { hexFocused = it.isFocused },
            singleLine = true,
            label = { Text("Código hexadecimal") },
            supportingText = { Text("Usa #RRGGBB o #AARRGGBB") }
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (description.isNotBlank()) Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> ChoiceSetting(
    title: String,
    description: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (title.isNotBlank()) Text(title, fontWeight = FontWeight.SemiBold)
        if (description.isNotBlank()) Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(label(option), maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun SliderSetting(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun ActionSetting(title: String, description: String, action: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (description.isNotBlank()) Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

@Composable
private fun SettingNote(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
