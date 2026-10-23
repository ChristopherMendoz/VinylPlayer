# Configuración general de VinylPlayer

## Objetivo

Reemplazar el diálogo de Ajustes, que hoy solo contiene el control **Animar brazo fonocaptor**, por un centro de configuración con buscador, ocho categorías y una subpantalla por categoría. Los ajustes disponibles deben cambiar el comportamiento real y conservarse entre sesiones. Las funciones aún no respaldadas por el proyecto se identificarán como pendientes, sin switches ficticios.

El control de animación del brazo se conserva en **Apariencia** y mantiene su clave `tonearm_animation`, valor predeterminado y efecto actuales; por ello, la preferencia que ya guardó el usuario no se pierde.

## Estado actual y restricciones

- `MainScreen.kt` abre `SettingsDialog`; no hay navegación de subpantallas ni búsqueda de preferencias.
- `PlayerViewModel` persiste preferencias y estado del reproductor en `SharedPreferences("vinylplayer_settings")`. La cola, la pista/posición, el aleatorio, los favoritos, el desfase por canción y el control del brazo ya tienen estado persistido.
- `VinylPlayerTheme` solo aplica el tema oscuro dorado. El brazo del vinilo ya responde a `tonearm_animation`.
- Existe ecualizador de cinco bandas y preajustes, aplicados por `PlaybackService`.
- La cola se conserva y dispone de controles de repetición, aleatorio, orden, limpieza y acciones sobre pistas.
- Las letras se guardan en Room, la traducción actual es de inglés a español y el desfase se guarda por canción.
- La biblioteca lee la colección de audio de Android. La app pide permiso de audio y notificaciones desde `MainActivity`.
- El repositorio Git remoto es `https://github.com/ChristopherMendoz/VinylPlayer`; la versión sale de Gradle. No se encontró una política de privacidad o información de contacto definida en el proyecto.

Restricciones: mantener Kotlin, Compose, Media3 y el mínimo actual de Android; no introducir dependencias de navegación o persistencia nuevas; no borrar música, biblioteca, favoritos, cola o letras al restablecer preferencias; no exponer datos personales en diagnósticos.

## Diseño

### Navegación y presentación

El botón de Ajustes abre una pantalla completa integrada en la navegación Compose existente. La raíz presenta búsqueda y las ocho categorías. Tocar una categoría abre su propia subpantalla con título, botón de regreso, descripciones breves y controles persistidos. El buscador filtra categorías y controles implementados por nombre y descripción. La raíz y las subpantallas usan listas desplazables para pantallas pequeñas y controles con área táctil accesible.

No se incorpora Navigation Compose: un estado de navegación local al flujo de Configuración y el manejo de regreso existente son suficientes para esta pantalla. El botón de Ajustes sigue disponible desde la biblioteca y el reproductor.

### Persistencia y fuentes de verdad

Se conserva `SharedPreferences("vinylplayer_settings")` para las opciones generales, reutilizando las claves que ya existen y agrupando el acceso nuevo tras una capa pequeña de preferencias. `PlayerViewModel` continúa siendo la fuente de estado observable para el comportamiento del reproductor; Configuración envía cambios a sus acciones, no mantiene una copia paralela de la cola o del estado de reproducción. `PlaybackService` sigue aplicando el ecualizador desde las preferencias que ya consume.

Los cambios de apariencia se exponen a `VinylPlayerTheme` y a las vistas que dibujan fondos, letras y vinilo. Los cambios de reproducción, cola y letras usan las operaciones existentes del reproductor/ViewModel y solo agregan estado cuando sea necesario para cumplir un ajuste acordado.

### Categorías y alcance funcional

1. **Apariencia y personalización**: conservar el switch del brazo fonocaptor en su ubicación actual y con la misma preferencia guardada. Añadir modos claro/oscuro/sistema, colores de acento compatibles, fondo sólido o portada desenfocada, y personalización respaldada para la tipografía de las letras. Restaurar solo las preferencias visuales.
2. **Reproducción**: exponer la reanudación de pista y posición, el avance automático y el comportamiento al final de la cola; guardar las preferencias de aleatorio y repetición. Integrar el temporizador de apagado que ya existe. Pausa por desconexión de auriculares se incluirá solo si el flujo Media3 puede respaldarla en la versión actual de Android.
3. **Biblioteca musical**: mostrar cantidad de pistas, estado de permiso y actualización manual; usar el escaneo y metadatos existentes. La inclusión/exclusión de carpetas requiere soporte de acceso a directorios (SAF) que hoy no existe; no se presentará como operativa hasta estar implementada.
4. **Letras y traducción**: controlar visibilidad predeterminada, idioma de traducción respaldado, tamaño/estilo de letra, líneas de contexto y desfase global predeterminado. El desfase individual existente tiene prioridad sobre el global y no se sobrescribe al editar preferencias generales.
5. **Fila de reproducción**: controlar la conservación de la cola, su comportamiento predeterminado al agregar pistas, duplicados y preferencias de aleatorio; conservar las operaciones existentes de mover, reproducir, quitar y limpiar. Las acciones que borran pendientes piden confirmación y nunca borran archivos.
6. **Audio y ecualizador**: reutilizar las cinco bandas y preajustes reales. Añadir activar/desactivar y restaurar valores solo si el efecto `Equalizer` está disponible; no mostrar efectos no soportados por el dispositivo.
7. **Almacenamiento y permisos**: mostrar los permisos requeridos, permitir solicitarlos cuando corresponda y abrir los ajustes de la app cuando Android requiera intervención manual. Incluir el resultado del último escaneo y errores de acceso comprensibles. La gestión de carpetas queda pendiente junto con el soporte SAF.
8. **Acerca de VinylPlayer**: mostrar nombre, versión y compilación reales, enlace al repositorio y créditos/licencias que puedan verificarse desde dependencias del proyecto. Política de privacidad, contacto e historial solo se mostrarán si existen fuentes definidas; no se inventan enlaces.

Los elementos no implementados se presentarán como información no interactiva claramente marcada como pendiente o se pospondrán. Ningún control parecerá habilitado si no altera el estado real.

### Restablecimiento y datos

Restablecer todas las preferencias modifica únicamente las claves de configuración administradas por el centro. Conserva archivos de audio, favoritos, contenido y orden actual de la cola, pista/posición guardadas, biblioteca en caché y letras en Room. Limpiar la cola permanece como acción separada y confirmada.

## Plan por fases

1. **Centro y navegación**: pantalla raíz, ocho categorías, subpantallas, búsqueda y componentes Compose reutilizables; mover el ajuste existente a Apariencia sin cambiar su clave.
2. **Persistencia y apariencia**: acceso a preferencias, tema, acento, fondo, tipografía de letras y restauración visual.
3. **Reproductor y cola**: conectar continuidad, fin de cola, repetición/aleatorio y preferencias de cola sin duplicar controles existentes.
4. **Biblioteca y letras**: escaneo, permisos/estado, idioma de traducción, presentación de letras y desfase global con precedencia individual.
5. **Audio, permisos y acerca de**: conectar el ecualizador disponible, diagnóstico de permisos, metadatos y enlaces verificables.
6. **Revisión**: inspección de navegación, persistencia, valores predeterminados, alcance del restablecimiento y opciones realmente respaldadas; el reporte separará implementación de funciones pendientes y no declarará pruebas no ejecutadas.

## Criterios de aceptación

- Las ocho categorías se muestran y cada una tiene subpantalla y navegación de regreso.
- La búsqueda encuentra categorías y ajustes implementados.
- Cada preferencia visible conserva su valor y modifica el comportamiento indicado.
- El ajuste **Animar brazo fonocaptor** está en Apariencia, funciona y conserva el valor que tenía guardado.
- El desfase global no pisa desfases individuales.
- Los ajustes no compatibles no se presentan como funciones operativas.
- El diseño se puede recorrer en pantallas pequeñas y ofrece descripciones y objetivos táctiles accesibles.
- Restablecer preferencias no elimina música, cola, favoritos, biblioteca ni letras.
