# VinylPlayer

Reproductor de música local para Android, desarrollado con Kotlin, Jetpack Compose y Media3.

## Funciones

- Explora y reproduce archivos de audio del dispositivo.
- Muestra título, artista, álbum y carátula cuando esos datos están disponibles.
- Controles de reproducción, progreso, aleatorio y repetición.
- Fila de reproducción dividida entre la canción actual y las siguientes.
- Permite reordenar canciones pendientes arrastrándolas, reproducir una a continuación, agregarla al final o quitarla de la fila.
- La fila y la canción actual se restauran al volver a abrir la app.
- Busca letras y presenta estados de carga, disponibilidad y conexión.
- Las letras sincronizadas siguen la canción: la vista comienza desde el inicio para evitar un espacio vacío y centra la línea activa cuando ya hay suficiente contexto anterior.
- Permite ajustar la sincronización de la letra en ambos sentidos y traducirla cuando hay traducción disponible.

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
