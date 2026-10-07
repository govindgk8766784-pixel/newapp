package com.example.data.model

data class PassportPreset(
    val id: String,
    val name: String,
    val country: String,
    val widthMm: Float,
    val heightMm: Float,
    val dpi: Int = 300,
    val recommendedBg: String = "White",
    val headRatioMin: Float = 0.65f, // Min head percentage
    val headRatioMax: Float = 0.75f  // Max head percentage
) {
    val widthPx: Int get() = ((widthMm / 25.4f) * dpi).toInt()
    val heightPx: Int get() = ((heightMm / 25.4f) * dpi).toInt()
    val aspectRatio: Float get() = widthMm / heightMm
    val dimensionDisplay: String get() = "${widthMm.toInt()} x ${heightMm.toInt()} mm (${widthPx} x ${heightPx} px)"

    companion object {
        val ALL = listOf(
            PassportPreset("in_pass", "India Passport / Visa", "India", 35f, 45f, recommendedBg = "White"),
            PassportPreset("us_pass", "US Passport & Visa / Green Card", "USA", 51f, 51f, recommendedBg = "White"),
            PassportPreset("eu_pass", "UK / Schengen / EU Passport", "Europe", 35f, 45f, recommendedBg = "Light Grey"),
            PassportPreset("ca_pass", "Canada Passport", "Canada", 50f, 70f, recommendedBg = "White"),
            PassportPreset("au_pass", "Australia Passport", "Australia", 35f, 45f, recommendedBg = "White"),
            PassportPreset("ae_visa", "UAE / Dubai Visa", "UAE", 40f, 60f, recommendedBg = "White"),
            PassportPreset("in_pan", "India PAN Card Photo", "India", 25f, 35f, recommendedBg = "White"),
            PassportPreset("jp_visa", "Japan Visa", "Japan", 35f, 45f, recommendedBg = "White"),
            PassportPreset("cn_visa", "China Visa", "China", 33f, 48f, recommendedBg = "White"),
            PassportPreset("sg_pass", "Singapore Passport", "Singapore", 35f, 45f, recommendedBg = "White")
        )
    }
}
