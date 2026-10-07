package com.example.data.model

enum class ProcessingFilter(
    val title: String,
    val description: String,
    val iconName: String
) {
    ORIGINAL("Original", "Natural untouched photo", "restore"),
    MAGIC_COLOR("Magic Color", "Boosts ink contrast, stamps & text clarity", "auto_fix_high"),
    CLEAN_PAPER("Clean Paper", "Removes shadows and brightens background", "description"),
    BW_PHOTOCOPY("B&W Photocopy", "High-contrast monochrome for xerox", "filter_b_and_w"),
    GRAYSCALE("Grayscale", "Smooth studio grayscale tones", "tonality"),
    STUDIO_ENHANCE("Studio Portrait", "Soft skin smoothing and crisp iris", "face")
}
