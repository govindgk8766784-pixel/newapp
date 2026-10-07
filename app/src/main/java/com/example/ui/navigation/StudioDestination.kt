package com.example.ui.navigation

enum class StudioDestination(
    val title: String,
    val iconTag: String
) {
    DASHBOARD("Studio", "home"),
    PASSPORT("Passport", "face"),
    ID_CARD("ID & Card", "badge"),
    BACKGROUND("BG Changer", "auto_fix_high"),
    DOCUMENT("Doc Scan", "document_scanner"),
    PROJECTS("History", "photo_library"),
    HEALTH("Health", "tune")
}
