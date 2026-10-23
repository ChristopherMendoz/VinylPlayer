# Biblioteca y personalización — Plan de implementación

> **For agentic workers:** Implementar este plan tarea por tarea en la sesión actual. Cada tarea tiene una comprobación enfocada; cerrar con una compilación debug.

**Goal:** Corregir contraste, ampliar el color de acento y la búsqueda, habilitar edición real de etiquetas y aplicar colores de portada a álbumes y reproductor.

**Architecture:** Conservar `AppSettings`, `MusicRepository`, `PlayerViewModel` y los componentes Compose existentes. Extraer solo lógica pura de búsqueda, paleta y color para que sus casos importantes se puedan comprobar sin duplicar estado. Escribir tags en una copia temporal y devolver el resultado a la URI original tras el consentimiento de Android.

**Tech Stack:** Kotlin, Jetpack Compose, SharedPreferences, MediaStore, Jaudiotagger Android fork, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-22-biblioteca-personalizacion-design.md`

## Global Constraints

- Mantener compatibles las preferencias actuales de `AccentColor` y el valor Dorado predeterminado.
- No cambiar el archivo de audio sin permiso explícito concedido por Android.
- Los controles de edición solo se habilitan para extensiones escritas por la biblioteca de tags.
- Mantener modos de fondo existentes y añadir Colores de portada.
- No añadir servicios ni fuentes de estado alternas para biblioteca, cola o reproducción.
- Ejecutar una suite enfocada y una sola compilación `assembleDebug` al cierre.

## Review Focus

- El custom ARGB nulo o malformado vuelve al acento Dorado y no rompe preferencias antiguas (prueba de códec del color).
- La búsqueda por playlist conserva el filtro de canciones al abrirla y permite buscar por nombre de lista (pruebas de `LibrarySearch`).
- Permiso de escritura cancelado o formato no admitido no altera bytes ni metadata (prueba de whitelist; revisar explícitamente flujo de cancelación).
- Bitmap sin píxeles, transparente o con paleta pareja produce un fondo válido (pruebas de `CoverPalette` incluyendo fallback).
- Ruta no disponible para URI ajena a MediaStore conserva un texto útil de fallback (revisión de ruta).

---

### Task 1: Contraste y acento personalizado

**Files:**
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/settings/AppSettings.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/settings/SettingsPreferences.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/Theme.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/MainActivity.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/SettingsScreen.kt`
- Create: `app/src/main/java/com/chrismdz/vinylplayer/settings/AccentColorValue.kt`
- Test: `app/src/test/java/com/chrismdz/vinylplayer/settings/AccentColorValueTest.kt`

**Interfaces:**
- Produce `AccentColorValue.parseHex(value: String): Int?` y `AccentColorValue.toHex(argb: Int): String`.
- `AppSettings.customAccentArgb: Int?` guarda el color personalizado; `accentColor` continúa guardando las opciones Dorado/Rojo/Personalizado.
- `VinylPlayerTheme` recibe el acento enum y `customAccentArgb` para resolver `colorScheme.primary`.

- [x] Añadir pruebas para parsear `#RRGGBB`/`#AARRGGBB`, rechazar texto inválido y conservar alpha al serializar.
- [x] Implementar el códec ARGB y persistirlo en una clave adicional; resetear Apariencia elimina esa clave.
- [x] Corregir el contenido de tarjetas y grupos fijando `contentColor`/colores de texto desde `MaterialTheme.colorScheme`.
- [x] Agregar selector personalizado con controles HSL y entrada hexadecimal; conservar muestras Dorado y Rojo.

### Task 2: Búsqueda por pestaña

**Files:**
- Create: `app/src/main/java/com/chrismdz/vinylplayer/ui/LibrarySearch.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/MainScreen.kt`
- Test: `app/src/test/java/com/chrismdz/vinylplayer/ui/LibrarySearchTest.kt`

**Interfaces:**
- Produce `filterLibrary(library: List<MusicFile>, query: String): List<MusicFile>`.
- Produce `filterPlaylists(playlists: List<Playlist>, query: String): List<Playlist>` y `playlistSongs(library: List<MusicFile>, playlist: Playlist?, query: String): List<MusicFile>`.

- [x] Probar coincidencias sin distinguir mayúsculas en título, artista, álbum y nombre de playlist; probar playlist filtrada por título/artista/álbum de sus canciones.
- [x] Usar esas funciones en las cuatro pestañas, el detalle seleccionado, conteos y estados vacíos; filtrar Favoritos por canciones.
- [x] Cambiar el placeholder y textos de resultados para reflejar la pestaña activa.

### Task 3: Ruta y edición de tags del archivo

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/data/MusicRepository.kt`
- Create: `app/src/main/java/com/chrismdz/vinylplayer/data/AudioTagWriter.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/PlayerViewModel.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/MainScreen.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/SettingsScreen.kt`
- Create: `app/src/main/assets/third_party_licenses/jaudiotagger.txt`
- Test: `app/src/test/java/com/chrismdz/vinylplayer/data/AudioTagFormatTest.kt`

**Interfaces:**
- Produce `AudioTagWriter.supports(context: Context, uri: Uri): Boolean` y `write(context: Context, uri: Uri, title: String, artist: String, album: String)`; la comprobación local `supportsFileName(name: String?): Boolean` queda pura para probarla.
- Producir `MusicRepository.readFilePath(context: Context, uri: Uri): String?` usando `DATA`, luego `RELATIVE_PATH + DISPLAY_NAME`.
- El ViewModel expone edición que devuelve autorización pendiente de `MediaStore.createWriteRequest` (API 30+), usa el `RecoverableSecurityException` de Android 10 o escribe y refresca metadata en IO; cancelar deja el estado intacto.

- [x] Probar la whitelist soportada y exclusión de extensiones ajenas al escritor.
- [x] Configurar JitPack y Jaudiotagger Android fork; escribir tags en una copia temporal y copiar al URI únicamente después del consentimiento.
- [x] Reindexar MediaStore y reemplazar la metadata en biblioteca, canción actual, cola y playlists sin restablecer posición de reproducción.
- [x] Cambiar “Canción” a “Título de la canción”; hacer editables Título/Artista/Álbum y mostrar errores de cancelación, formato o escritura.
- [x] Mostrar ruta exacta MediaStore; para URI sin ruta mostrar `DISPLAY_NAME` y luego el URI como fallback.
- [x] Atribuir Jaudiotagger y su LGPL en Acerca de y añadir el aviso de licencia de terceros.

### Task 4: Paleta de portada en álbum y reproductor

**Files:**
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/settings/AppSettings.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/settings/SettingsPreferences.kt`
- Create: `app/src/main/java/com/chrismdz/vinylplayer/ui/CoverPalette.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/SettingsScreen.kt`
- Modify: `app/src/main/java/com/chrismdz/vinylplayer/ui/MainScreen.kt`
- Test: `app/src/test/java/com/chrismdz/vinylplayer/ui/CoverPaletteTest.kt`

**Interfaces:**
- Produce `CoverPalette.fromPixels(pixels: IntArray): List<Int>` con hasta tres tonos ARGB, muestreados y agrupados.
- `PlayerBackground.COVER_COLORS` mantiene los valores anteriores con fallback por enum.

- [x] Probar bitmap transparente/vacío, color dominante y paleta repartida.
- [x] Muestrear la miniatura de portada y generar degradado con dominante o combinación representativa; usar colores del tema cuando no haya portada.
- [x] Aplicar el mismo degradado a detalle de álbum y reproductor en modo Colores de portada; mantener Desenfoque y Color sólido.
- [x] Añadir el nuevo modo a Configuración y persistirlo.

### Task 5: Revisión de entrega

**Files:**
- Revisar el diff de los archivos de las tareas anteriores; no ampliar el alcance.

- [x] Ejecutar `./gradlew testDebugUnitTest` una vez y revisar el resultado.
- [x] Ejecutar `./gradlew assembleDebug` una vez y revisar el resultado.
- [x] Informar qué quedó implementado y qué depende del consentimiento/formato Android; reportar la instalación real sin declarar comportamiento no probado.

