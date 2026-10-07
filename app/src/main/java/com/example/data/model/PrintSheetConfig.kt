package com.example.data.model

enum class PaperSize(
    val title: String,
    val widthInches: Float,
    val heightInches: Float,
    val widthMm: Float,
    val heightMm: Float
) {
    FOUR_BY_SIX("4 x 6 inch (10x15 cm)", 4f, 6f, 101.6f, 152.4f),
    FIVE_BY_SEVEN("5 x 7 inch (13x18 cm)", 5f, 7f, 127f, 177.8f),
    SIX_BY_EIGHT("6 x 8 inch (15x20 cm)", 6f, 8f, 152.4f, 203.2f),
    A4("A4 (210 x 297 mm)", 8.27f, 11.69f, 210f, 297f);

    fun getPixelWidth(dpi: Int = 300): Int = (widthInches * dpi).toInt()
    fun getPixelHeight(dpi: Int = 300): Int = (heightInches * dpi).toInt()
}

data class PrintSheetConfig(
    val paperSize: PaperSize = PaperSize.FOUR_BY_SIX,
    val photoCount: Int = 6, // 4, 6, 8, 12, 16, 32
    val showCutBorder: Boolean = true,
    val showCornerMarks: Boolean = true,
    val gapMm: Float = 2.5f,
    val marginMm: Float = 5.0f,
    val dpi: Int = 300
)
