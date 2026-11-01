package io.github.cq61tech.konumaldatici

/**
 * Ülke profili: Aşama 3'te hook'lar bu verileri (SIM, operatör, saat dilimi, konum) birlikte döndürecek.
 * Tutarlılık için her ülke tek pakettir.
 */
data class Country(
    val code: String,
    val nameTr: String,
    val nameEn: String,
    val mcc: String,
    val mnc: String,
    val operatorName: String,
    val timezone: String,
    val locale: String,
    val lat: Double,
    val lon: Double,
) {
    val flag: String
        get() = String(Character.toChars(0x1F1E6 + (code[0] - 'A'))) +
            String(Character.toChars(0x1F1E6 + (code[1] - 'A')))

    fun displayName(lang: String): String = if (lang == "tr") nameTr else nameEn
}

object Countries {
    val all: List<Country> = listOf(
        Country("US", "ABD", "United States", "310", "260", "T-Mobile", "America/New_York", "en_US", 40.7128, -74.0060),
        Country("GB", "Birleşik Krallık", "United Kingdom", "234", "15", "Vodafone", "Europe/London", "en_GB", 51.5074, -0.1278),
        Country("DE", "Almanya", "Germany", "262", "01", "Telekom.de", "Europe/Berlin", "de_DE", 52.5200, 13.4050),
        Country("FR", "Fransa", "France", "208", "01", "Orange F", "Europe/Paris", "fr_FR", 48.8566, 2.3522),
        Country("NL", "Hollanda", "Netherlands", "204", "08", "KPN", "Europe/Amsterdam", "nl_NL", 52.3676, 4.9041),
        Country("CA", "Kanada", "Canada", "302", "720", "Rogers", "America/Toronto", "en_CA", 43.6532, -79.3832),
        Country("JP", "Japonya", "Japan", "440", "10", "NTT DOCOMO", "Asia/Tokyo", "ja_JP", 35.6762, 139.6503),
        Country("KR", "Güney Kore", "South Korea", "450", "05", "SK Telecom", "Asia/Seoul", "ko_KR", 37.5665, 126.9780),
        Country("SG", "Singapur", "Singapore", "525", "01", "Singtel", "Asia/Singapore", "en_SG", 1.3521, 103.8198),
        Country("SE", "İsveç", "Sweden", "240", "01", "Telia", "Europe/Stockholm", "sv_SE", 59.3293, 18.0686),
        Country("CH", "İsviçre", "Switzerland", "228", "01", "Swisscom", "Europe/Zurich", "de_CH", 47.3769, 8.5417),
        Country("ES", "İspanya", "Spain", "214", "07", "Movistar", "Europe/Madrid", "es_ES", 40.4168, -3.7038),
        Country("IT", "İtalya", "Italy", "222", "01", "TIM", "Europe/Rome", "it_IT", 41.9028, 12.4964),
        Country("PL", "Polonya", "Poland", "260", "02", "T-Mobile.pl", "Europe/Warsaw", "pl_PL", 52.2297, 21.0122),
        Country("AE", "BAE", "United Arab Emirates", "424", "02", "Etisalat", "Asia/Dubai", "ar_AE", 25.2048, 55.2708),
        Country("AU", "Avustralya", "Australia", "505", "01", "Telstra", "Australia/Sydney", "en_AU", -33.8688, 151.2093),
        Country("BR", "Brezilya", "Brazil", "724", "05", "Claro", "America/Sao_Paulo", "pt_BR", -23.5505, -46.6333),
        Country("IN", "Hindistan", "India", "404", "10", "Airtel", "Asia/Kolkata", "en_IN", 28.6139, 77.2090),
        Country("MX", "Meksika", "Mexico", "334", "020", "Telcel", "America/Mexico_City", "es_MX", 19.4326, -99.1332),
    )

    fun byCode(code: String?): Country = all.firstOrNull { it.code == code } ?: all[0]
}
