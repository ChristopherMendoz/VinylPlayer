# Mejoras de biblioteca y personalización — Diseño

**Estado:** diseño aprobado para especificación escrita por el usuario el 2026-10-22.  
**Alcance:** mejoras sobre flujos existentes de Configuración, biblioteca, información de canción y reproductor.

## Objetivo

Mejorar la legibilidad y personalización visual de VinylPlayer, hacer que la búsqueda abarque todas las pestañas de la biblioteca, permitir editar las etiquetas de audio desde la información de canción y dar fondos con colores de portada a álbumes y canciones en reproducción.

## Contexto actual

- Las tarjetas de Configuración tienen un color de superficie con transparencia, pero no fijan el color de contenido; el texto hereda un color incorrecto en el tema oscuro.
- `AccentColor` solo contiene Dorado y Rojo.
- La búsqueda filtra canciones para construir álbumes y artistas, pero la lista de playlists usa directamente `state.playlists` y el estado vacío depende solo de canciones.
- La información de canción dibuja flechas sin acciones. La ruta usa `Uri.path`, que no es la ruta absoluta del archivo MediaStore.
- El detalle del álbum no cambia el fondo según su portada. El reproductor solo tiene los modos desenfoque y color sólido.

## Diseño

### Apariencia

- Fijar explícitamente el color de contenido del tema en tarjetas y grupos de Configuración para conservar contraste en modo oscuro y claro.
- Mantener los acentos Dorado y Rojo y añadir un color personalizado RGB persistido como ARGB. El selector ofrecerá control de tono, saturación y luminosidad, además de entrada hexadecimal para seleccionar cualquier color.
- Mantener compatibilidad con la preferencia de acento guardada como enum; el color personalizado se guardará en una clave adicional. Restablecer apariencia volverá a Dorado.

### Búsqueda de biblioteca

- Mantener una sola consulta y hacerla sensible a la pestaña activa.
- Pistas buscará título, artista y álbum; Álbumes y Artistas buscarán sus nombres y la metadata de sus pistas; Playlist buscará el nombre de cada playlist y filtrará también sus canciones al entrar.
- Los mensajes de estado vacío y los conteos corresponderán a los resultados de la pestaña actual.

### Información y etiquetas de canción

- Cambiar el rótulo “Canción” por “Título de la canción”.
- Consultar la columna de ruta de MediaStore para mostrar la ruta absoluta; si falta, construirla con la ruta relativa y el nombre del archivo cuando sea posible.
- Hacer editables Título, Artista y Álbum desde diálogos con validación de texto.
- Escribir las etiquetas dentro del archivo con un escritor compatible con Android. Android expone los campos de título, artista y álbum como metadata indexada, así que la escritura se hará en el archivo, no con `ContentResolver.update`.
- Usar el fork Android de Jaudiotagger (`com.github.Adonai:jaudiotagger:2.3.14`) para MP3, FLAC, OGG, MP4/M4A, AIFF, WAV, WMA y DSF. No mostrar el editor como operativo para formatos que el escritor no admita.
- Solicitar permiso de escritura del archivo cuando Android lo requiera. Ejecutar la escritura en I/O, volver a indexar el archivo y sincronizar biblioteca, canción actual y fila. Si el formato o proveedor no permite escribir, mostrar un error y conservar intacto el archivo.
- Conservar las filas existentes de duración, tamaño, portada personalizada y archivo manual de letra sin alterar su comportamiento actual.

### Colores de portada

- Añadir un cálculo liviano de paleta desde una miniatura de portada y reutilizarlo en el detalle del álbum y el reproductor.
- El fondo será un degradado con tonos derivados del color dominante. Si la portada carece de un dominante claro, combinará los tonos más representativos; sin portada usará los colores del tema.
- Añadir “Colores de portada” como modo de fondo del reproductor y conservar Desenfoque y Color sólido. El modo de fondo de álbum usará siempre la paleta de su portada.

## Persistencia y flujo de errores

- Las preferencias de acento y modo de fondo siguen en `SettingsPreferences`.
- Las etiquetas se escriben al archivo original solo tras autorización de Android. Errores de permisos, formato no compatible, ruta inaccesible o fallo de escritura se devuelven a la interfaz con un mensaje; no se guarda un estado que aparente éxito.
- Tras escribir etiquetas, volver a consultar MediaStore y actualizar las referencias en pantalla sin reiniciar la reproducción.

## Verificación

- Pruebas unitarias enfocadas a la persistencia del acento personalizado, búsqueda por tipo de contenido y selección/fallback de paleta.
- Verificación manual en modo oscuro, en las cuatro pestañas, al editar tags de un archivo escribible y en el detalle de álbum/reproductor con portada colorida y sin portada.
- Una compilación debug al terminar; no se afirmará que se ejecutó en el teléfono hasta instalar y comprobar esa versión.

## Límites

- La escritura de etiquetas depende del formato y del permiso de escritura concedido por Android. Solo se habilitará para formatos que el escritor soporte; otros conservarán sus datos y mostrarán el motivo.
- La ruta absoluta puede no estar disponible para URI de proveedores ajenos a MediaStore.

## Referencias técnicas

- [Android `MediaStore.Audio.Media`](https://developer.android.com/reference/android/provider/MediaStore.Audio.Media.html): documenta los campos de audio indexados y `DATA` como ruta absoluta.
- [Fork Android de Jaudiotagger](https://github.com/Kaned1as/jaudiotagger): documenta los formatos de escritura y el artefacto Android propuesto.
