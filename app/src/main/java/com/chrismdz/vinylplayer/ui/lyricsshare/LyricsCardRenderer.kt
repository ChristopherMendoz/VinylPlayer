package com.chrismdz.vinylplayer.ui.lyricsshare

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import kotlin.math.roundToInt

internal enum class LyricsCardStyle(val label: String) {
    CLASSIC("Classic"),
    GRADIENT("Gradient"),
    MINIMAL("Minimal"),
    DARK("Dark"),
    EDITORIAL("Editorial")
}

/**
 * Font for the lyrics on the card. [STYLE] keeps each style's own font (bold sans, serif for
 * Editorial…); the others use Android's built-in families, so no font files are bundled.
 */
internal enum class LyricsCardFont(val label: String) {
    STYLE("Del estilo"),
    MODERN("Moderna"),
    SERIF("Clásica"),
    ITALIC("Elegante"),
    CONDENSED("Condensada"),
    LIGHT("Ligera"),
    MONO("Máquina"),
    CASUAL("Casual");

    val typeface: Typeface? by lazy {
        when (this) {
            STYLE -> null
            MODERN -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            ITALIC -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            LIGHT -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            CASUAL -> Typeface.create("casual", Typeface.BOLD)
        }
    }
}

internal data class LyricsCardContent(
    val title: String,
    val artist: String,
    /** Selected lyric lines, in song order. */
    val lines: List<String>,
    val cover: Bitmap?,
    /** User's text size choice (1 = automatic size); the fit still shrinks text that doesn't fit. */
    val textScale: Float = 1f,
    val font: LyricsCardFont = LyricsCardFont.STYLE
)

/**
 * Draws a lyrics card on an android.graphics [Canvas] in a fixed 1080×1920 (9:16) space.
 * The preview scales this same drawing down and the export draws it into a bitmap, so what the
 * user sees is exactly what gets shared.
 */
internal object LyricsCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1920
    const val MIN_TEXT_SCALE = 0.6f
    const val MAX_TEXT_SCALE = 1.3f
    private const val W = WIDTH.toFloat()
    private const val H = HEIGHT.toFloat()
    private const val MARGIN = 96f
    private const val SCALED_COVER_CACHE_BYTES = 6 * 1024 * 1024

    private val sansBold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val sansMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val sansRegular: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    fun draw(canvas: Canvas, content: LyricsCardContent, style: LyricsCardStyle, colors: LyricsCardColors) {
        // Never transparent: an opaque base under every style.
        canvas.drawColor(0xFF000000.toInt())
        when (style) {
            LyricsCardStyle.CLASSIC -> classic(canvas, content, colors)
            LyricsCardStyle.GRADIENT -> gradient(canvas, content, colors)
            LyricsCardStyle.MINIMAL -> minimal(canvas, content, colors)
            LyricsCardStyle.DARK -> dark(canvas, content, colors)
            LyricsCardStyle.EDITORIAL -> editorial(canvas, content, colors)
        }
    }

    // --- Styles ------------------------------------------------------------------------------------

    /** Spotify-like: background color, a raised inner card holding header, lyrics and brand. */
    private fun classic(canvas: Canvas, content: LyricsCardContent, colors: LyricsCardColors) {
        paintBackground(canvas, colors, RectF(0f, 0f, W, H))
        val left = 72f
        val right = W - 72f
        val pad = 64f
        val coverSize = 124f
        val textWidth = (right - left - pad * 2).toInt()
        val lyrics = fitLyrics(
            content.lines, textWidth, maxHeight = H * 0.52f,
            maxSize = 96f * sizeFactor(content), minSize = minSizeFor(content, 40f),
            typeface = content.font.typeface ?: sansBold, color = colors.text, align = Layout.Alignment.ALIGN_NORMAL
        )
        val cardHeight = pad + coverSize + 64f + lyrics.height + 72f + 40f + pad
        val top = (H - cardHeight) / 2f
        val card = RectF(left, top, right, top + cardHeight)
        canvas.drawRoundRect(card, 56f, 56f, fill(colors.surface))
        canvas.drawRoundRect(card, 56f, 56f, stroke(withAlpha(colors.text, 0.08f), 2f))

        val coverRect = RectF(left + pad, top + pad, left + pad + coverSize, top + pad + coverSize)
        drawCover(canvas, content.cover, coverRect, 18f, colors)
        val metaX = coverRect.right + 32f
        val metaWidth = right - pad - metaX
        drawSingleLine(canvas, content.title, textPaint(42f, colors.text, sansBold), metaX, coverRect.top + 18f, metaWidth)
        drawSingleLine(canvas, content.artist, textPaint(34f, colors.secondaryText, sansRegular), metaX, coverRect.top + 72f, metaWidth)

        drawLayout(canvas, lyrics, left + pad, coverRect.bottom + 64f)
        drawBrand(canvas, colors, left + pad, card.bottom - pad - 6f, Paint.Align.LEFT)
    }

    /** Full-bleed album gradient with a soft glow; large lyrics centered in the free space. */
    private fun gradient(canvas: Canvas, content: LyricsCardContent, colors: LyricsCardColors) {
        paintBackground(canvas, colors, RectF(0f, 0f, W, H))
        glow(canvas, W * 0.15f, H * 0.12f, W * 0.95f, colors.accent, 0.32f)
        glow(canvas, W * 0.9f, H * 0.95f, W * 0.8f, colors.background.last(), 0.45f)

        val coverSize = 168f
        val coverRect = RectF(MARGIN, MARGIN + 40f, MARGIN + coverSize, MARGIN + 40f + coverSize)
        drawCover(canvas, content.cover, coverRect, 22f, colors)
        val metaX = coverRect.right + 36f
        val metaWidth = W - MARGIN - metaX
        drawSingleLine(canvas, content.title, textPaint(46f, colors.text, sansBold), metaX, coverRect.top + 34f, metaWidth)
        drawSingleLine(canvas, content.artist, textPaint(36f, colors.secondaryText, sansRegular), metaX, coverRect.top + 94f, metaWidth)

        val areaTop = coverRect.bottom + 90f
        val areaBottom = H - MARGIN - 110f
        val lyrics = fitLyrics(
            content.lines, (W - MARGIN * 2).toInt(), maxHeight = areaBottom - areaTop,
            maxSize = 108f * sizeFactor(content), minSize = minSizeFor(content, 44f),
            typeface = content.font.typeface ?: sansBold, color = colors.text, align = Layout.Alignment.ALIGN_NORMAL
        )
        drawLayout(canvas, lyrics, MARGIN, areaTop + (areaBottom - areaTop - lyrics.height) / 2f)
        drawBrand(canvas, colors, MARGIN, H - MARGIN, Paint.Align.LEFT)
    }

    /** Flat color, centered lyrics and almost nothing else. */
    private fun minimal(canvas: Canvas, content: LyricsCardContent, colors: LyricsCardColors) {
        canvas.drawRect(0f, 0f, W, H, fill(colors.background.first()))
        val sideMargin = 140f
        val lyrics = fitLyrics(
            content.lines, (W - sideMargin * 2).toInt(), maxHeight = H * 0.6f,
            maxSize = 92f * sizeFactor(content), minSize = minSizeFor(content, 40f),
            typeface = content.font.typeface ?: sansMedium, color = colors.text, align = Layout.Alignment.ALIGN_CENTER,
            spacing = 1.28f
        )
        val top = (H - lyrics.height) / 2f - 30f
        // Short accent rule above the lyrics.
        canvas.drawRoundRect(RectF(W / 2f - 36f, top - 86f, W / 2f + 36f, top - 80f), 3f, 3f, fill(colors.accent))
        drawLayout(canvas, lyrics, sideMargin, top)

        val meta = textPaint(30f, colors.secondaryText, sansMedium).apply { textAlign = Paint.Align.CENTER }
        drawSingleLine(canvas, "${content.title} · ${content.artist}", meta, W / 2f, H - MARGIN - 110f, W - sideMargin * 2, Paint.Align.CENTER)
        drawBrand(canvas, colors, W / 2f, H - MARGIN, Paint.Align.CENTER, size = 24f)
    }

    /** Deep tones from the palette, a glowing cover on top and centered light lyrics. */
    private fun dark(canvas: Canvas, content: LyricsCardContent, colors: LyricsCardColors) {
        paintBackground(canvas, colors, RectF(0f, 0f, W, H))
        canvas.drawRect(0f, 0f, W, H, fill(0xB8000000.toInt()))
        glow(canvas, W / 2f, MARGIN + 300f, W * 0.75f, colors.background.first(), 0.75f)

        val light = 0xFFFFFFFF.toInt()
        val lightSecondary = 0xB3FFFFFF.toInt()
        val coverSize = 440f
        val coverRect = RectF((W - coverSize) / 2f, MARGIN + 70f, (W + coverSize) / 2f, MARGIN + 70f + coverSize)
        canvas.drawRoundRect(RectF(coverRect).apply { offset(0f, 18f) }, 30f, 30f, fill(0x66000000))
        drawCover(canvas, content.cover, coverRect, 26f, colors)

        val center = textPaint(44f, light, sansBold).apply { textAlign = Paint.Align.CENTER }
        drawSingleLine(canvas, content.title, center, W / 2f, coverRect.bottom + 56f, W - MARGIN * 2, Paint.Align.CENTER)
        val centerArtist = textPaint(34f, lightSecondary, sansRegular).apply { textAlign = Paint.Align.CENTER }
        drawSingleLine(canvas, content.artist, centerArtist, W / 2f, coverRect.bottom + 116f, W - MARGIN * 2, Paint.Align.CENTER)

        val areaTop = coverRect.bottom + 220f
        val areaBottom = H - MARGIN - 100f
        val lyrics = fitLyrics(
            content.lines, (W - MARGIN * 2).toInt(), maxHeight = areaBottom - areaTop,
            maxSize = 82f * sizeFactor(content), minSize = minSizeFor(content, 36f),
            typeface = content.font.typeface ?: sansBold, color = light, align = Layout.Alignment.ALIGN_CENTER
        )
        drawLayout(canvas, lyrics, MARGIN, areaTop + (areaBottom - areaTop - lyrics.height) / 2f)
        drawBrand(canvas, colors.copy(secondaryText = lightSecondary), W / 2f, H - MARGIN, Paint.Align.CENTER)
    }

    /** Magazine layout: giant quote mark, serif lyrics beside an accent bar, credits at the foot. */
    private fun editorial(canvas: Canvas, content: LyricsCardContent, colors: LyricsCardColors) {
        paintBackground(canvas, colors, RectF(0f, 0f, W, H))
        val brand = textPaint(24f, colors.secondaryText, sansBold).apply {
            letterSpacing = 0.3f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("VINYLPLAYER", W - MARGIN, MARGIN + 20f, brand)
        canvas.drawText("“", MARGIN - 14f, MARGIN + 330f, textPaint(400f, withAlpha(colors.accent, 0.9f), serifBold))

        val textLeft = MARGIN + 44f
        val areaTop = MARGIN + 360f
        val areaBottom = H - MARGIN - 290f
        val lyrics = fitLyrics(
            content.lines, (W - MARGIN - textLeft).toInt(), maxHeight = areaBottom - areaTop,
            maxSize = 102f * sizeFactor(content), minSize = minSizeFor(content, 44f),
            typeface = content.font.typeface ?: serifBold, color = colors.text, align = Layout.Alignment.ALIGN_NORMAL,
            spacing = 1.14f
        )
        canvas.drawRoundRect(RectF(MARGIN, areaTop + 8f, MARGIN + 8f, areaTop + lyrics.height - 8f), 4f, 4f, fill(colors.accent))
        drawLayout(canvas, lyrics, textLeft, areaTop)

        val ruleY = H - MARGIN - 210f
        canvas.drawRect(MARGIN, ruleY, W - MARGIN, ruleY + 2f, fill(withAlpha(colors.text, 0.25f)))
        val coverRect = RectF(MARGIN, ruleY + 50f, MARGIN + 128f, ruleY + 178f)
        drawCover(canvas, content.cover, coverRect, 10f, colors)
        val metaX = coverRect.right + 32f
        val metaWidth = W - MARGIN - metaX
        val title = textPaint(34f, colors.text, sansBold).apply { letterSpacing = 0.08f }
        drawSingleLine(canvas, content.title.uppercase(), title, metaX, coverRect.top + 22f, metaWidth)
        drawSingleLine(canvas, content.artist, textPaint(32f, colors.secondaryText, sansRegular), metaX, coverRect.top + 76f, metaWidth)
    }

    // --- Adaptive lyrics ---------------------------------------------------------------------------

    /**
     * Starting size factor: fewer lines start bigger, then the user's text size choice is applied.
     * The fit below still shrinks anything that doesn't fit.
     */
    private fun sizeFactor(content: LyricsCardContent): Float = lineCountFactor(content.lines) * textScale(content)

    /** Smallest size for the fit; follows the user's choice down, with a legibility floor. */
    private fun minSizeFor(content: LyricsCardContent, base: Float): Float =
        (base * textScale(content).coerceAtMost(1f)).coerceAtLeast(26f)

    private fun textScale(content: LyricsCardContent) = content.textScale.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE)

    private fun lineCountFactor(lines: List<String>): Float = when (lines.size) {
        0, 1 -> 1f
        2 -> 0.84f
        3 -> 0.72f
        4 -> 0.64f
        5 -> 0.58f
        else -> 0.54f
    }

    /**
     * Largest size (from [maxSize] down to [minSize]) whose layout fits [maxHeight] and in which
     * no single word is wider than the column, so words are never split. Lines keep the song's
     * line breaks; long lines wrap at word boundaries with balanced breaks.
     */
    private fun fitLyrics(
        lines: List<String>,
        width: Int,
        maxHeight: Float,
        maxSize: Float,
        minSize: Float,
        typeface: Typeface,
        color: Int,
        align: Layout.Alignment,
        spacing: Float = 1.18f
    ): StaticLayout {
        val text = lines.joinToString("\n").ifBlank { " " }
        val words = text.split(Regex("\\s+")).filter(String::isNotEmpty)
        var size = maxSize.coerceAtLeast(minSize)
        while (true) {
            val paint = textPaint(size, color, typeface)
            val longestWord = words.maxOfOrNull { paint.measureText(it) } ?: 0f
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(align)
                .setLineSpacing(0f, spacing)
                .setIncludePad(false)
                .setBreakStrategy(Layout.BREAK_STRATEGY_BALANCED)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .build()
            if ((layout.height <= maxHeight && longestWord <= width) || size <= minSize) return layout
            size = (size - 2f).coerceAtLeast(minSize)
        }
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    // --- Pieces ------------------------------------------------------------------------------------

    private fun paintBackground(canvas: Canvas, colors: LyricsCardColors, rect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (colors.background.size == 1) {
            paint.color = colors.background.first()
        } else {
            paint.shader = if (colors.gradient == CardGradient.VERTICAL) {
                LinearGradient(rect.left, rect.top, rect.left, rect.bottom, colors.background.toIntArray(), null, Shader.TileMode.CLAMP)
            } else {
                LinearGradient(rect.left, rect.top, rect.right, rect.bottom, colors.background.toIntArray(), null, Shader.TileMode.CLAMP)
            }
        }
        canvas.drawRect(rect, paint)
    }

    private fun glow(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(cx, cy, radius, withAlpha(color, alpha), withAlpha(color, 0f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, paint)
    }

    /**
     * Square center crop of [cover] at exactly [side] px, reduced in halving steps so a large cover
     * keeps its detail (a single bilinear pass from 1500 px to 128 px skips most pixels and looks
     * grainy). Cached per source bitmap and size, so the live preview doesn't rescale every frame and
     * two different covers can never share an entry. Null when the source can't be scaled here.
     */
    private fun scaledCover(cover: Bitmap, side: Int): Bitmap? {
        if (side <= 0 || cover.config == Bitmap.Config.HARDWARE) return null
        val key = "${System.identityHashCode(cover)}:${cover.generationId}:${cover.width}x${cover.height}:$side"
        scaledCovers.get(key)?.let { if (!it.isRecycled) return it }
        val result = runCatching {
            val crop = minOf(cover.width, cover.height)
            var current = Bitmap.createBitmap(cover, (cover.width - crop) / 2, (cover.height - crop) / 2, crop, crop)
            while (current.width / 2 >= side) {
                val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
                if (current !== cover && current !== next) current.recycle()
                current = next
            }
            if (current.width != side) {
                val next = Bitmap.createScaledBitmap(current, side, side, true)
                if (current !== cover && current !== next) current.recycle()
                current = next
            }
            current
        }.getOrNull() ?: return null
        scaledCovers.put(key, result)
        return result
    }

    private val scaledCovers = object : android.util.LruCache<String, Bitmap>(SCALED_COVER_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Center-cropped, rounded cover; a tinted tile with a note when there is no artwork. */
    private fun drawCover(canvas: Canvas, cover: Bitmap?, rect: RectF, radius: Float, colors: LyricsCardColors) {
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) })
        if (cover != null && !cover.isRecycled) {
            val scaled = scaledCover(cover, rect.width().roundToInt())
            if (scaled != null) {
                canvas.drawBitmap(scaled, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                val side = minOf(cover.width, cover.height)
                val src = Rect((cover.width - side) / 2, (cover.height - side) / 2, (cover.width + side) / 2, (cover.height + side) / 2)
                canvas.drawBitmap(cover, src, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
        } else {
            canvas.drawRect(rect, fill(withAlpha(colors.accent, 0.35f)))
            val note = textPaint(rect.height() * 0.45f, colors.text, sansBold).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("♪", rect.centerX(), rect.centerY() - (note.descent() + note.ascent()) / 2f, note)
        }
        canvas.restore()
    }

    /** Gold dot + "VinylPlayer": the app's signature on every card. */
    private fun drawBrand(canvas: Canvas, colors: LyricsCardColors, x: Float, baseline: Float, align: Paint.Align, size: Float = 30f) {
        val paint = textPaint(size, colors.secondaryText, sansMedium)
        val label = "VinylPlayer"
        val dotRadius = size * 0.3f
        val gap = size * 0.45f
        val total = dotRadius * 2 + gap + paint.measureText(label)
        val start = if (align == Paint.Align.CENTER) x - total / 2f else x
        val gold = 0xFFE4B45B.toInt()
        canvas.drawCircle(start + dotRadius, baseline - size * 0.36f, dotRadius, fill(gold))
        canvas.drawText(label, start + dotRadius * 2 + gap, baseline, paint)
    }

    /** One ellipsized line whose top edge sits at [top]. */
    private fun drawSingleLine(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        top: Float,
        maxWidth: Float,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        paint.textAlign = align
        val fitted = TextUtils.ellipsize(text, paint, maxWidth.coerceAtLeast(1f), TextUtils.TruncateAt.END).toString()
        canvas.drawText(fitted, x, top - paint.ascent(), paint)
    }

    private fun textPaint(size: Float, color: Int, typeface: Typeface) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        this.typeface = typeface
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (color and 0x00FFFFFF)
}
