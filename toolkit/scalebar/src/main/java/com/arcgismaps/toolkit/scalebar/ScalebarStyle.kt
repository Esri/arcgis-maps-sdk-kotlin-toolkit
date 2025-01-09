package com.arcgismaps.toolkit.scalebar

/**
 * A Scalebar style.
 *
 * @since 200.7.0
 */
public enum class ScalebarStyle {
    /**
     * Displays a single unit with segmented bars of alternating fill color.
     *
     * @since 200.7.0
     */
    AlternatingBar,

    /**
     * Displays a single unit.
     *
     * @since 200.7.0
     */
    Bar,

    /**
     * Displays both metric and imperial units. The primary unit is displayed on top.
     *
     * @since 200.7.0
     */
    DualUnitLine,

    /**
     * Displays a single unit with a single bar.
     *
     * @since 200.7.0
     */
    GraduatedLine,

    /**
     * Displays a single unit with endpoint tick marks.
     *
     * @since 200.7.0
     */
    Line
}
