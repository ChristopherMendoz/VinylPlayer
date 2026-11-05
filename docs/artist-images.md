# Artist images

Artist portraits are resolved through a background WorkManager job. The app
checks its private local image file first, searches Deezer for one exact artist
name match, then falls back to MusicBrainz and Wikidata only if needed. The
fallback requires a unique exact MusicBrainz name and a Wikidata entity linked
to that MusicBrainz ID; Commons P18 file metadata supplies the image and
available attribution and license details.

Downloaded image files stay in `filesDir/artist-images` and remain available
offline. Room tracks the lookup status, source, creator attribution, license,
source page, and retry time. Existing image files keep their original
artist-ID-based filenames through the database migration; the old source URL
is retained as provenance but is no longer downloaded or used as a provider.
No API key is required.

MusicBrainz requests include an app/version User-Agent and pass through a
shared one-request-per-second gate. WorkManager waits for network connectivity
and retries temporary failures with exponential backoff. Definitive misses are
stored for 30 days before another lookup is allowed.

## Rights and attribution

An API being free to access does not grant rights to redistribute its images.
Deezer's developer terms say the developer receives no rights to the API's
content and requires developers to hold necessary permissions. Confirm that
the intended use of Deezer images is authorized before distributing the app.
The app displays a Deezer source link, but that does not replace permission.

Wikimedia Commons files have file-specific terms. The profile links to the
Commons file page and displays available creator and license metadata. Check
each file's license and any additional restrictions before reuse; metadata
may be missing and Commons does not warrant that the supplied copyright data
is correct. The app does not scrape websites.

- [Deezer Developer Terms](https://developers.deezer.com/termsofuse)
- [MusicBrainz API rate limits and User-Agent guidance](https://musicbrainz.org/doc/MusicBrainz_API/Rate_Limiting)
- [Wikimedia Commons reuse guidance](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia)
- [Android WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work)
