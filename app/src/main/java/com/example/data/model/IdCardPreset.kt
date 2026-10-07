package com.example.data.model

data class IdCardPreset(
    val id: String,
    val name: String,
    val category: String,
    val widthMm: Float,
    val heightMm: Float,
    val description: String,
    val isDualSided: Boolean = true
) {
    val aspectRatio: Float get() = widthMm / heightMm
    val dimensionDisplay: String get() = "${widthMm.toInt()} x ${heightMm.toInt()} mm"

    companion object {
        val ALL = listOf(
            IdCardPreset("aadhaar", "Aadhaar Card (Standard)", "Government ID", 85.6f, 53.98f, "Front & Back Indian National ID Card format", true),
            IdCardPreset("pan_card", "PAN Card (Income Tax)", "Financial ID", 85.6f, 53.98f, "Permanent Account Number card standard CR80", false),
            IdCardPreset("voter_id", "Voter ID / EPIC Card", "Election Commission", 85.6f, 53.98f, "Election Photo Identity Card (EPIC)", true),
            IdCardPreset("dl_card", "Driving License", "Transport Dept", 85.6f, 53.98f, "Smart Card format with microchip zone", true),
            IdCardPreset("passbook", "Bank Passbook Header", "Banking Document", 150f, 90f, "Account number, IFSC, and customer photo page", false),
            IdCardPreset("ration", "Ration / Food Security Card", "Civil Supplies", 130f, 95f, "Family head details and ration card sheet", true),
            IdCardPreset("custom_id", "Custom ID / Card Format", "Custom", 100f, 65f, "Configurable card proportions", true)
        )
    }
}
