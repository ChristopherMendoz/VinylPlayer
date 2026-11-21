# VinylPlayer

Reproductor de música local para Android, desarrollado con Kotlin, Jetpack Compose y Media3.

## Funciones

- Muestra los formatos de audio que Android reconoce en la biblioteca, excepto los archivos `.opus`; los videos no se incluyen.
- Muestra título, artista, álbum y carátula cuando esos datos están disponibles.
- Los álbumes se agrupan por nombre exacto, incluidas las variantes de mayúsculas, y muestran el conteo real de pistas. La portada predeterminada es la del primer track y se puede elegir otra portada del álbum.
- Los detalles de álbum consolidan los créditos de artistas y usan colores derivados de la portada en el fondo del álbum y del reproductor.
- Controles de reproducción, progreso, aleatorio y repetición.
- Fila de reproducción dividida entre la canción actual y las siguientes.
- Permite reordenar canciones pendientes arrastrándolas, reproducir una a continuación, agregarla al final o quitarla de la fila.
- La fila y la sesión se restauran al volver a abrir la app según las preferencias de continuidad.
- Busca letras y presenta estados de carga, disponibilidad y conexión.
- Las canciones con letra disponible muestran un distintivo antes del artista en la biblioteca, favoritas, playlists, artistas, álbumes y la fila; se actualiza solo al descargar o eliminar una letra.
- Las letras sincronizadas siguen la canción y mantienen centrada la línea activa, con tamaño y contexto ajustables.
- Permite ajustar la sincronización por canción o usar un desfase global; el ajuste individual tiene prioridad.
- Traduce letras a español, inglés, francés, alemán, italiano o portugués y conserva el idioma asociado a cada traducción guardada.
- Comparte fragmentos de letra como tarjetas con la portada en alta resolución: la de la canción por defecto o la del álbum cuando es distinta, con los colores de la tarjeta adaptados a la portada elegida.
- Centro de configuración con secciones de Apariencia, Reproducción, Biblioteca, Letras, Fila, Audio, Almacenamiento y Acerca de.
- Configuración persistente para tema, acento, fondo del reproductor, animación del brazo, continuidad, cola, orden de biblioteca, letras y ecualizador.
- La búsqueda de ajustes filtra categorías y opciones; el restablecimiento conserva la música, favoritos, fila, biblioteca y letras guardadas.

## Stack tecnológico

App Android nativa, 100 % Kotlin, con interfaz en Jetpack Compose. Fuera de AndroidX solo usa jaudiotagger y JUnit.

### Lenguaje y compilación

| Pieza | Versión |
|---|---|
| Kotlin (con el plugin de Compose integrado en el compilador) | 2.0.21 |
| Gradle (scripts `.kts`) | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| JDK de compilación | 17 |
| `minSdk` / `targetSdk` / `compileSdk` | 26 / 34 / 34 |
| Procesamiento de anotaciones | `kapt` (Room) |

### Interfaz

- **Jetpack Compose** (catálogo de versiones 2024.12.01, foundation 1.7.6) con **Material 3** y los íconos extendidos de Material.
- **Sistema de diseño propio** (`DesignSystem.kt`, `Theme.kt`): paleta negra con acento dorado configurable, espaciados (`VinylSpacing`), formas (`VinylShapes`) y componentes compartidos (`LibraryPill`, `GroupedSurface`, `PlayButton`…).
- **Dibujo a medida:**
  - Canvas de Compose para el vinilo y el brazo.
  - `android.graphics` + `StaticLayout` para las tarjetas de letra.
  - Gestos propios para reordenar por arrastre.
- **Activity Compose** 1.9.3 para permisos y selección de archivos.

### Arquitectura y estado

- **Una pantalla con un ViewModel** (`PlayerViewModel`) que expone `StateFlow` (`uiState`, `playbackPositionMs`), con Lifecycle 2.8.7 y `collectAsStateWithLifecycle`.
- **Corrutinas de Kotlin** para el trabajo en segundo plano: análisis de portadas, red y exportación de imágenes.
- **Paquetes:** `data/` (repositorios y proveedores), `player/`, `settings/`, `ui/` y `ui/lyricsshare/`.

### Reproducción

- **Media3** 1.4.1:
  - ExoPlayer como motor de audio.
  - `MediaSessionService` (`PlaybackService`) para reproducir en segundo plano con notificación y controles del sistema.
  - `MediaController` (`MusicPlayer`) para manejarlo desde la app.
- **Ecualizador** con `android.media.audiofx.Equalizer`.

### Datos y almacenamiento

- **Room** 2.6.1: caché de letras y catálogo de artistas con sus créditos e imágenes.
- **SharedPreferences:** ajustes, sesión y fila, favoritos, portadas elegidas por álbum, orden personalizado de álbumes y desfases de letra.
- **MediaStore:** lectura de la biblioteca y guardado de imágenes en la galería.
- **Storage Access Framework** para importar playlists y **FileProvider** para compartir imágenes.
- **jaudiotagger** 2.3.14 (fork de Adonai vía JitPack) para leer y escribir etiquetas de audio.
- **WorkManager** 2.9.1 para descargar imágenes de artistas en segundo plano, con reintentos.

### Servicios externos

Conexiones HTTP directas con `HttpURLConnection` y `org.json`, sin librerías de red.

| Servicio | Uso |
|---|---|
| LRCLIB | Letras simples y sincronizadas |
| Google Translate (endpoint público) | Traducción de letras |
| Deezer API | Imágenes de artistas (fuente principal) |
| MusicBrainz + Wikidata + Wikimedia Commons | Imágenes de artistas (respaldo) |

### Pruebas y publicación

- **JUnit 4** para pruebas unitarias de la lógica pura: búsqueda, créditos de artistas, colores de portada, orden de álbumes y líneas de letra.
- **Versionado** en `app/build.gradle.kts` (`versionName` / `versionCode`); es lo que muestra la sección «Acerca de».
- **Publicación** con tags anotados (`X.Y.Z`) y releases de GitHub que adjuntan `VinylPlayer.apk`.
- **APK publicado:** la compilación debug, firmada con la llave de depuración. Se instala encima de las versiones anteriores sin perder datos.

## Requisitos

- JDK 17
- Android SDK 34
- Android 8.0 (API 26) o posterior

## Abrir y compilar

Abre esta carpeta en Android Studio o VS Code. Para generar e instalar la versión de depuración en un dispositivo conectado por USB:

```bash
./gradlew installDebug
```

Para generar solamente el APK:

```bash
./gradlew assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.
