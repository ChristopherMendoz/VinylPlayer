# Centro de configuración de VinylPlayer — Plan de implementación

> **Para agentes implementadores:** subskill requerido: `superpowers:executing-plans` para ejecución nativa o `superpowers:subagent-driven-development` si el usuario elige explícitamente delegar. Seguir las tareas en orden.

**Objetivo:** reemplazar el diálogo actual de Ajustes por un centro adaptable con buscador, ocho categorías y preferencias persistentes conectadas a las funciones reales de VinylPlayer.

**Arquitectura:** usar una pantalla Compose separada y navegación interna por categoría, sin dependencia nueva. Mantener `SharedPreferences("vinylplayer_settings")` como almacenamiento, conservar `PlayerViewModel` como fuente observable del estado del reproductor y mantener la clave existente `tonearm_animation` al mover su control a Apariencia.

**Tecnologías:** Kotlin, Jetpack Compose, SharedPreferences, Media3, Room.

**Especificación:** [docs/superpowers/specs/2026-10-22-configuracion-general-design.md](../specs/2026-10-22-configuracion-general-design.md)

## Restricciones globales

- Mantener `minSdk 26`, Kotlin, Compose, Media3 y Room actuales.
- No añadir dependencias de navegación, estado o persistencia.
- Mantener `tonearm_animation`; conservar favoritos, pistas, posición, cola, biblioteca y letras al restablecer preferencias.
- No presentar controles ficticios. Las funciones sin soporte se muestran como pendientes o se posponen.
- No añadir ni ejecutar pruebas automatizadas sin petición del usuario. Hacer inspección de código y una compilación de depuración al finalizar.
- `.git` está en modo de solo lectura en este entorno; no ejecutar `git add` ni `git commit` durante la implementación.

## Enfoque de revisión

- El valor actual de `tonearm_animation` sobrevive al cambio de pantalla y sigue controlando `VinylView`.
- Tema, acento y fondos restauran valores al recrear la Activity, sin parpadeos ni estados distintos entre pantalla principal y Ajustes.
- Desactivar reanudación no borra canciones ni modifica el contenido de la cola.
- El desfase global de letras solo aplica a canciones sin desfase individual guardado.
- Restablecer ajustes no elimina archivos, favoritos, la cola actual, la sesión guardada, la biblioteca en caché ni las letras de Room.
- Cambios de idioma de traducción no muestran como actual una traducción guardada en otro idioma.

---

## Mapa de archivos

- Crear `app/src/main/java/com/chrismdz/vinylplayer/settings/AppSettings.kt`: enums y valores predeterminados de las preferencias generales.
- Crear `app/src/main/java/com/chrismdz/vinylplayer/settings/SettingsPreferences.kt`: lectura, escritura y restablecimiento limitado a claves de configuración.
- Crear `app/src/main/java/com/chrismdz/vinylplayer/ui/SettingsScreen.kt`: raíz, búsqueda, categorías, subpantallas y filas de control/información.
- Modificar `MainActivity.kt`: observar la configuración para aplicar el tema global y pasar el estado existente.
- Modificar `MainScreen.kt`: sustituir `SettingsDialog` por el centro y conectar navegación/acciones.
- Modificar `PlayerViewModel.kt`: exponer preferencias en `PlayerUiState` y conectar cambios con reproductor, cola, biblioteca y letras.
- Modificar `Theme.kt` y `VinylView.kt`: aplicar tema/acento/fondo y mantener la animación del brazo.
- Modificar `Lyrics.kt`, `LyricsDatabase.kt`, `LyricsRepository.kt` y `LyricsTranslationProvider.kt`: idioma de traducción y desfase global, con migración Room cuando corresponda.
- Modificar `MusicPlayer.kt` y `PlaybackService.kt` únicamente para las preferencias de reproducción/audio que necesiten integrarse con Media3.
- Actualizar `README.md` con el nuevo centro y los ajustes operativos.

---

### Tarea 1: Modelo y persistencia de preferencias

**Archivos:** crear `settings/AppSettings.kt` y `settings/SettingsPreferences.kt`; modificar `ui/PlayerViewModel.kt`.

**Interfaces:**

- `AppSettings` agrupa apariencia (`themeMode`, `accent`, `playerBackground`, `tonearmAnimationEnabled`), sesión/reproducción, biblioteca, letras, cola y ecualizador.
- `SettingsPreferences.load(): AppSettings` lee `vinylplayer_settings`; `save(settings: AppSettings)` escribe solo claves de configuración; `resetAppearance(): AppSettings` restaura únicamente apariencia; `resetAll(): AppSettings` restaura preferencias sin tocar datos de biblioteca, cola, sesión, favoritos o letras.
- `PlayerUiState` expone `appSettings: AppSettings`; `PlayerViewModel.updateAppSettings(transform: (AppSettings) -> AppSettings)` guarda y publica cada cambio. El estado del brazo pasa del campo superior actual a `appSettings.tonearmAnimationEnabled`.
- El ajuste del brazo continúa guardado con la clave histórica `tonearm_animation`; el tema inicial se mantiene oscuro y dorado para conservar la apariencia actual.

- [ ] Definir enums cerrados y valores predeterminados para tema (oscuro, claro, sistema), acento (dorado, rojo), fondo (portada desenfocada, sólido), idioma de traducción (español por defecto, más inglés/francés/alemán/italiano/portugués), acción por defecto de cola y opciones que sí tendrán implementación.
- [ ] Implementar lectura/escritura de preferencias y una lista explícita de claves restablecibles; excluir las claves de datos como `queue_cache`, `current_file`, `playback_position`, `favorite_uris`, `library_cache` y `lyrics_offsets` del restablecimiento general.
- [ ] Añadir `appSettings` al estado inicial del `PlayerViewModel`, mover a él `tonearmAnimationEnabled` y conectar `updateAppSettings` sin duplicar estado de cola ni estado de reproducción.
- [ ] Inspeccionar que los valores predeterminados reflejan el comportamiento existente: tema oscuro/dorado, brazo animado, sesión y cola conservadas, avance automático activado, letras visibles, traducción española y ecualizador activo.

### Tarea 2: Centro, búsqueda y navegación

**Archivos:** crear `ui/SettingsScreen.kt`; modificar `ui/MainScreen.kt`.

**Interfaces:** `SettingsScreen(settings, playerState, onSettingsChange, onRefreshLibrary, onRequestAudioPermission, onOpenAppSettings, onOpenEqualizer, onClearQueue, onReset)`; categorías `APPEARANCE`, `PLAYBACK`, `LIBRARY`, `LYRICS`, `QUEUE`, `AUDIO`, `STORAGE`, `ABOUT`.

- [ ] Crear una raíz desplazable con buscador y ocho tarjetas, cada una con título, resumen e icono descriptivo.
- [ ] Crear una subpantalla por categoría con navegación de regreso y descripciones breves; usar listas Compose desplazables y objetivos táctiles de al menos 48 dp.
- [ ] Hacer que la búsqueda filtre categorías y controles implementados por nombre/descripción y permita abrir directamente la subpantalla correspondiente.
- [ ] Reemplazar `SettingsDialog` en `MainScreen.kt` por el flujo raíz/subpantalla, conservar el botón superior en Biblioteca y Reproduciendo y hacer que Atrás vuelva primero a la raíz de Ajustes.
- [ ] Añadir filas informativas no interactivas para funciones explícitamente pendientes; no dibujar switches o botones para ellas.

### Tarea 3: Apariencia y personalización

**Archivos:** modificar `ui/Theme.kt`, `ui/VinylView.kt`, `ui/MainScreen.kt` y `ui/PlayerViewModel.kt`; consumir `AppSettings` de la Tarea 1.

- [ ] Cambiar `VinylPlayerTheme` para recibir `themeMode` y `accent`, generar esquemas claros/oscuros para dorado/rojo y respetar modo sistema.
- [ ] Observar `PlayerUiState.appSettings` desde `MainActivity` para aplicar el tema a toda la aplicación, no solo a Configuración.
- [ ] Añadir la opción portada desenfocada/sólido al fondo del reproductor preservando el fondo y contraste actuales por defecto.
- [ ] Mover el switch **Animar brazo fonocaptor** a esta página; conectar `settings.tonearmAnimationEnabled` con el `tonearm_animation` existente y con `VinylView`.
- [ ] Conectar restaurar apariencia a `resetAppearance`, sin borrar otras preferencias ni datos.

### Tarea 4: Reproducción y fila

**Archivos:** modificar `ui/PlayerViewModel.kt`, `ui/SettingsScreen.kt` y, solo si la integración lo requiere, `player/MusicPlayer.kt`.

- [ ] Añadir opciones persistidas `resumeLastSession` y `autoAdvance`; al desactivar reanudación, ignorar/eliminar solo el puntero de sesión guardada, sin borrar canciones ni la cola.
- [ ] Persistir el modo de repetición con valores `OFF`, `ALL`, `ONE`, restaurarlo al iniciar y mantener `cycleRepeatMode()` sincronizado con Media3.
- [ ] Mantener `shuffle_enabled` como preferencia actual y exponerlo desde la categoría Reproducción/Fila sin crear una copia de estado.
- [ ] Aplicar `autoAdvance` al final de pista: modo `ONE` repite la pista, `ALL` recorre/repite la cola, y en `OFF` la opción determina si se avanza o se detiene.
- [ ] Añadir preferencia de conservación de cola, acción predeterminada `ADD_TO_END`/`PLAY_NEXT` y duplicados permitidos. Conservar las acciones explícitas existentes y los controles de ordenar/quitar/reproducir/limpiar en la pantalla de cola.
- [ ] Conectar el temporizador de apagado existente desde Ajustes sin quitar el acceso actual. Solo implementar pausa por desconexión de auriculares si el manejo Media3 actual permite hacerlo; en caso contrario, indicar que está pendiente.

### Tarea 5: Biblioteca y permisos

**Archivos:** modificar `ui/SettingsScreen.kt`, `ui/PlayerViewModel.kt` y `MainActivity.kt`.

- [ ] Mostrar cantidad de canciones desde `PlayerUiState.library.size`, estado del último escaneo y errores actuales de carga.
- [ ] Conectar “Actualizar biblioteca” con `loadLibrary(force = true)` y ofrecer resultado comprensible.
- [ ] Mostrar estado de `READ_MEDIA_AUDIO` o `READ_EXTERNAL_STORAGE` según Android y estado de notificaciones; permitir solicitar los permisos y abrir Ajustes de la aplicación cuando estén denegados permanentemente.
- [ ] Mantener comportamiento de acceso existente y no borrar entradas ni archivos al escanear.
- [ ] Mostrar inclusión/exclusión de carpetas como pendiente: no habilitar gestión SAF hasta que tenga persistencia y filtrado real.

### Tarea 6: Letras y traducción

**Archivos:** modificar `ui/PlayerViewModel.kt`, `ui/MainScreen.kt`, `data/Lyrics.kt`, `data/LyricsDatabase.kt`, `data/LyricsRepository.kt` y `data/LyricsTranslationProvider.kt`.

- [ ] Añadir preferencia de idioma destino; cambiar `LyricsTranslationProvider.translate` para recibir `targetLanguage` y usar su código en el parámetro `tl`.
- [ ] Guardar idioma de traducción en el modelo/Room; subir base a versión 3 y añadir migración `2 -> 3` para impedir que el caché de español aparezca como si fuera otro idioma.
- [ ] Conectar visibilidad predeterminada de letras, tamaño/estilo de texto y cantidad de líneas anteriores/siguientes con los composables de la pantalla del reproductor.
- [ ] Añadir `globalLyricsOffsetMs`; `loadLyricsOffset(file)` usa el desfase particular si existe y, en caso contrario, el global. Los botones actuales de ajuste continúan guardando por canción.
- [ ] Añadir control de desfase global y restauración global que no modifica las entradas existentes de `lyrics_offsets`.

### Tarea 7: Audio, almacenamiento y Acerca de

**Archivos:** modificar `ui/SettingsScreen.kt`, `ui/MainScreen.kt` y `player/PlaybackService.kt`; reutilizar datos de `app/build.gradle.kts` y `git remote` verificados en el diseño.

- [ ] Integrar el ecualizador de cinco bandas y sus preajustes existentes en Audio. Mantener la opción de activar/desactivar solo si `Equalizer` se creó correctamente; conectar restaurar a valores planos sin alterar el resto del sonido.
- [ ] Marcar normalización/efectos no disponibles si el motor actual no los ofrece.
- [ ] Implementar diálogo de confirmación para restablecimiento general; llamar a `SettingsPreferences.resetAll`, restaurar estados observables y mantener archivos, cola, sesión, biblioteca, favoritos y letras locales.
- [ ] Mostrar nombre, versión/build reales y enlace al repositorio GitHub. Mostrar créditos y licencias solo para dependencias verificadas; no añadir enlaces de privacidad/contacto inexistentes.

### Tarea 8: Revisión e integración

**Archivos:** revisar las rutas afectadas; actualizar `README.md`.

- [ ] Recorrer por inspección de código las categorías, navegación de regreso, búsqueda vacía/coincidente, persistencia y valores predeterminados.
- [ ] Revisar los cinco casos de “Enfoque de revisión”, en particular la precedencia de desfase individual, migración Room e integridad de datos al restablecer.
- [ ] Compilar una vez con `env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug`; no correr suites de prueba ni declarar funciones no ejecutadas como probadas.
- [ ] Documentar en README las categorías funcionales y las opciones que se mantienen pendientes; cerrar con un reporte que separe implementación, limitaciones y verificación realmente realizada.

---

## Cobertura de la especificación

La Tarea 2 cubre estructura, buscador, subpantallas, persistencia visual y accesibilidad; la Tarea 3 Apariencia; la Tarea 4 Reproducción/Fila; la Tarea 5 Biblioteca/Permisos; la Tarea 6 Letras/Traducción; la Tarea 7 Audio/Acerca de/Restablecer; la Tarea 8 revisión y reporte. Gestión de carpetas, efectos de audio no disponibles, privacidad/contacto e historial sin fuente verificada quedan identificados como pendientes, no como controles activos.
