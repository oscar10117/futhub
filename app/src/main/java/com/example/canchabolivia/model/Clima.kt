package com.example.canchabolivia.model

enum class Aptitud(val titulo: String) {
    IDEAL("Ideal para jugar"),
    PRECAUCION("Juega con precaución"),
    NO_RECOMENDADO("No recomendado")
}

// Modelo limpio que usa la UI (ya sin nombres de JSON ni de tablas).
data class Clima(
    val temperatura: Double,
    val humedad: Int,
    val viento: Double,
    val hora: String,
    val desdeCache: Boolean = false
) {
    // Consejo para el partido según las condiciones de la cancha.
    // Se evalúa de lo más grave a lo más leve y gana la primera que aplique.
    // Regla del torneo: más de 32 °C o menos de -15 °C el partido no es apto.
    val noApto: Boolean get() = temperatura > 32 || temperatura < -15

    val aptitud: Aptitud
        get() = when {
            noApto || viento >= 40 -> Aptitud.NO_RECOMENDADO
            viento >= 25 || temperatura <= 5 || temperatura >= 30 || humedad >= 85 -> Aptitud.PRECAUCION
            else -> Aptitud.IDEAL
        }

    val consejo: String
        get() = when {
            viento >= 40 -> "El viento es muy fuerte: el balón no irá donde quieres. Considera aplazar."
            temperatura < -15 -> "Frío extremo: el partido no es apto. Mejor reprogramar."
            temperatura > 32 -> "Calor extremo: el partido no es apto. Mejor reprogramar."
            viento >= 25 -> "Hay viento: ojo con los centros y los tiros largos."
            temperatura <= 5 -> "Hace mucho frío: calienta bien y juega abrigado."
            temperatura >= 30 -> "Hace calor: hidrátate y rota los cambios."
            humedad >= 85 -> "Ambiente muy húmedo: la cancha puede estar resbalosa."
            else -> "Buenas condiciones para rodar el balón."
        }

    // "2026-10-21T14:30" pasa a "14:30"
    val horaCorta: String get() = hora.substringAfter('T', hora)
}
