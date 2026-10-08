package dev.cipher.notes.ui

/**
 * Sizing for the app lock, derived from the space actually available.
 *
 * The previous version branched on hard-coded dp thresholds, which only fit the
 * one screen they were tuned on. Screen sizes vary from small phones to tablets
 * to arbitrary split-screen windows, so the layout is computed from the window
 * instead: the arrangement follows the aspect ratio, and the key size is
 * whatever fits, within a usable range.
 */
object LockLayout {
    const val COLUMNS = 3
    const val ROWS = 4

    /** Usable range for a key. Rails, not layout: within range they never bite. */
    const val MIN_KEY_DP = 44f
    const val MAX_KEY_DP = 80f

    /**
     * Preferred key size when the header sits beside the keypad. A design cap,
     * not a fit threshold: the space there could take larger keys, but they are
     * held to this size so they read as balanced against the header rather than
     * oversized. Matches the header's icon so the two line up.
     */
    const val WIDE_KEY_DP = 64f

    const val GAP_DP = 12f

    /**
     * Vertical gap between key rows, as a fraction of the key. The renderer
     * grows the gap with the key, so the height model has to grow it too.
     */
    const val ROW_GAP_RATIO = 0.25f

    /** Floor for the row gap, matching the renderer at small key sizes. */
    const val MIN_ROW_GAP_DP = 12f

    /**
     * True when the header should sit beside the keypad rather than above it.
     * "Landscape" is really "wider than tall", which holds on every device and
     * in split-screen, unlike a fixed height threshold.
     */
    fun isWide(widthDp: Float, heightDp: Float): Boolean = widthDp > heightDp

    /**
     * Largest key that fits an [areaWidthDp] x [areaHeightDp] area, so the grid
     * cannot overflow by construction. Clamped so it stays tappable on a tiny
     * window and does not balloon on a tablet.
     */
    fun keySizeDp(
        areaWidthDp: Float,
        areaHeightDp: Float,
        maxKeyDp: Float = MAX_KEY_DP
    ): Float {
        val byWidth = (areaWidthDp - GAP_DP * (COLUMNS - 1)) / COLUMNS
        // The renderer spaces rows by ROW_GAP_RATIO of the key, so the height a
        // grid of size k needs is k * (ROWS + (ROWS - 1) * ROW_GAP_RATIO), not
        // ROWS * k. Modelling it the latter way underestimates the height and
        // lets a larger key overflow into a scroll.
        val byHeight = areaHeightDp / (ROWS + (ROWS - 1) * ROW_GAP_RATIO)
        return minOf(byWidth, byHeight).coerceIn(MIN_KEY_DP, maxKeyDp.coerceAtLeast(MIN_KEY_DP))
    }

    /** Width the keypad needs for keys of [keySizeDp]. */
    fun keypadWidthDp(keySizeDp: Float): Float =
        keySizeDp * COLUMNS + GAP_DP * (COLUMNS - 1)

    /** Height the keypad needs for keys of [keySizeDp], matching the renderer. */
    fun keypadHeightDp(keySizeDp: Float): Float {
        val rowGap = rowGapDp(keySizeDp)
        return keySizeDp * ROWS + rowGap * (ROWS - 1)
    }

    /** Vertical gap the renderer uses between rows for a key of [keySizeDp]. */
    fun rowGapDp(keySizeDp: Float): Float =
        maxOf(keySizeDp * ROW_GAP_RATIO, MIN_ROW_GAP_DP)

    /** Scale for the header, so it shrinks with the keys in a short window. */
    fun headerScale(keySizeDp: Float): Float =
        (keySizeDp / MAX_KEY_DP).coerceIn(0.6f, 1f)
}
