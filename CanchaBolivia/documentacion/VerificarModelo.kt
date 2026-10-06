import com.example.canchabolivia.*
fun main() {
    for (n in 2..10) {
        val ids=(1..n).toList()
        val liga=Torneo(1,"Prueba",4,Formato.LIGA,ids)
        val partidos=MotorTorneo.liga(liga,ids,3)
        check(partidos.size==n*(n-1)/2)
        check(partidos.map { setOf(it.localId,it.visitanteId!!) }.distinct().size==partidos.size)
        partidos.groupBy { it.ronda }.values.forEach { ronda ->
            val participantes=ronda.flatMap { listOf(it.localId,it.visitanteId!!) }
            check(participantes.distinct().size==participantes.size)
        }
        val eliminacion=liga.copy(formato=Formato.ELIMINACION)
        var ronda=MotorTorneo.eliminacion(eliminacion,ids,3)
        check(ronda.flatMap { listOfNotNull(it.localId,it.visitanteId) }.sorted()==ids)
        var juegos=0
        do {
            juegos+=ronda.count { it.visitanteId!=null }
            ronda=ronda.map { if(it.visitanteId==null) it else it.copy(golesLocal=1,golesVisitante=0) }
            if(ronda.size==1) break
            ronda=MotorTorneo.siguienteRonda(ronda,100)
        } while(true)
        check(juegos==n-1)
        check(ronda.single().ganador() in ids)
    }
    val t=Torneo(1,"Tabla",4,Formato.LIGA,listOf(1,2,3))
    val tabla=MotorTorneo.tabla(t,listOf(Partido(1,1,1,1,2,3,2,0),Partido(2,1,2,1,3,3,1,1)))
    check(tabla.first().equipoId==1 && tabla.first().puntos==4 && tabla.first().diferencia==2)
    check(Partido(1,1,1,1,2,3,2,2,2).ganador()==2)
    println("OK: ligas y eliminatorias de 2 a 10 equipos, descansos, pases libres, tabla y penales.")
}
