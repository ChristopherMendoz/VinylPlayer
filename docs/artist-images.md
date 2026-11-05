# Artist images

VinylPlayer can use TheAudioDB's artist search endpoint to find a portrait for
an individual artist. The result is accepted only when the returned artist
name matches the library name after Unicode and case normalization. Image URLs
and lookup times are stored in Room; downloaded images are stored in the app's
private files directory. Missing images or connection errors use the built-in
music-note placeholder.

To enable lookups in a local development build, add this Gradle property to
`~/.gradle/gradle.properties`:

```properties
THEAUDIODB_API_KEY=your_api_key
```

Without the property, artist catalog, search, and playback work as usual and
artist images remain on the placeholder. Do not commit API keys. A key supplied
as a build property is still included in the generated APK and can be extracted;
use a server-side proxy for a distributed app. TheAudioDB's terms require
mentioning it as the artwork source and restrict app-store publication on the
free tier, so confirm the applicable plan and permissions before distribution.
The artist profile identifies TheAudioDB as the image source.
