package com.masasaatim.core.theme

// Import der Kernkomponenten von Jetpack Compose für Material Design 3 und Farbedition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * DarkColorScheme: Definiert die exakte Farbpalette für den Nacht- und Standardmodus der Tischuhr.
 * Basiert auf Material 3 Richtlinien, ist jedoch für AMOLED-Bildschirme optimiert (Pure Black).
 */
private val DarkColorScheme = darkColorScheme(
    // primary: Die Hauptakzentfarbe. Wird für wichtige Details wie das aktuelle Datum oder aktive UI-Elemente verwendet.
    primary = Color(0xFF00E676),      // Knalliges Neon-Grün für beste Lesbarkeit im Raum

    // background: Die Hintergrundfarbe des gesamten Bildschirms.
    // Durch die Verwendung von absolutem Schwarz (0xFF000000) verbrauchen OLED-Displays in diesem Bereich 0% Strom.
    background = Color(0xFF000000),   // Echtes AMOLED-Schwarz (Maximale Pixeleinsparung / Akku-Schonung)

    // surface: Die Hintergrundfarbe für Karten, Dialoge oder abgegrenzte Kontroll-Panels.
    surface = Color(0xFF121212),      // Dunkelgraue Oberflächenstruktur für subtile visuelle Abgrenzung

    // onBackground: Die Schriftfarbe für Texte, die direkt auf dem Haupthintergrund liegen.
    onBackground = Color(0xFFFFFFFF), // Kristallweiß für die primäre, große digitale Zeitanzeige (Uhrzeit)

    // onSurface: Die Schriftfarbe für Texte auf Oberflächen oder sekundäre Listenstrukturen.
    // 0xB3FFFFFF entspricht einem Deckkraftwert (Alpha) von ca. 70% für einen weicheren Kontrast.
    onSurface = Color(0xB3FFFFFF)     // Mattes, leicht transparentes Weiß für inaktive Gebetszeiten-Labels
)

/**
 * DesktopClockTheme: Die zentrale Theme-Funktion (Wrapper-Komponente) Ihrer App.
 * Umschließt die gesamte Benutzeroberfläche und vererbt das definierte Farbschema an alle Kind-Elemente.
 *
 * @param content Ein Lambda-Ausdruck, der die Benutzeroberfläche (z.B. MainScreen) enthält, die im Thema gerendert wird.
 */
@Composable
fun DesktopClockTheme(content: @Composable () -> Unit) {
    // Ruft das MaterialTheme-System auf und injiziert unsere maßgeschneiderte AMOLED-Farbpalette
    MaterialTheme(
        colorScheme = DarkColorScheme, // Überschreibt das Standard-Theme mit unserem optimierten Dunkelmodus
        content = content              // Zeichnet den übergebenen UI-Inhalt innerhalb dieses Farbkontexts
    )
}
