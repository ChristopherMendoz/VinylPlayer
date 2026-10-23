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
- Las letras sincronizadas siguen la canción y mantienen centrada la línea activa, con tamaño y contexto ajustables.
- Permite ajustar la sincronización por canción o usar un desfase global; el ajuste individual tiene prioridad.
- Traduce letras a español, inglés, francés, alemán, italiano o portugués y conserva el idioma asociado a cada traducción guardada.
- Centro de configuración con secciones de Apariencia, Reproducción, Biblioteca, Letras, Fila, Audio, Almacenamiento y Acerca de.
- Configuración persistente para tema, acento, fondo del reproductor, animación del brazo, continuidad, cola, orden de biblioteca, letras y ecualizador.
- La búsqueda de ajustes filtra categorías y opciones; el restablecimiento conserva la música, favoritos, fila, biblioteca y letras guardadas.

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
