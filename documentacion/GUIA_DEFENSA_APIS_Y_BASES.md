# FutHub: mapa para defender APIs y bases de datos (Programación 3)

**Versión base:** últimos cambios de `main` de `oscar10117/futhub`. El instalador prepara una **rama local** que parte de `origin/main` y añade comentarios dentro de los archivos sin tocar las ramas remotas.

**En Android Studio:** `Ctrl + Shift + F` → buscar `GUIA DOCENTE`. Ahí aparecen los lugares concretos que debes abrir si la docente dice «muéstrame dónde conectarías otra API o base de datos».

## 1. Dónde entra cada tecnología

| Tecnología | Archivos | Función |
|---|---|---|
| API Open-Meteo | `app/src/main/java/com/example/canchabolivia/api/OpenMeteoApi.kt` | URL base, `@GET`, parámetros para clima. |
| API del clima: respuesta | `dto/ClimaDto.kt` | DTO que refleja el JSON recibido. |
| API del clima: mapeo | `mapper/ClimaMapper.kt` | DTO → Entity de Room → modelo visual. |
| API del clima: lógica | `repository/ClimaRepository.kt` | Hace la llamada, valida la respuesta, guarda el último clima en Room. |
| API del clima: estado | `viewmodel/ClimaViewModel.kt` | Administra carga, actualización y errores. |
| API del clima: vista | `view/ClimaCard.kt` | Muestra datos, botón Actualizar y advertencias. |
| SQLite Cloud, acceso HTTP | `api/FutHubNubeApi.kt` | Retrofit y `@POST` a las Edge Functions. |
| SQLite Cloud, métodos | `repository/NubeRepository.kt` | `cargarDatos`, `guardarEquipo`, `guardarTorneo`, etc. |
| SQLite Cloud, JSON | `dto/NubeDto.kt` | DTO de equipos, jugadores, fichas, torneo, partidos y tarjetas. |
| SQLite Cloud, conversiones | `mapper/NubeMapper.kt` | DTO ↔ modelos Kotlin/cuerpo JSON. |
| SQLite Cloud, esquema | `sqlitecloud/01_tablas.sql` | SQL de las tablas remotas. |
| SQLite Cloud, funciones | `sqlitecloud/funciones/*.js` | Código que consulta y escribe en la base en el servicio remoto. |
| Base local Room | `database/FutHubDatabase.kt` | Entidades registradas, versión 3, DAOs y migraciones. |
| Room, lectura/escritura | `database/ClimaDao.kt` | Consultas `@Query` e inserciones `@Insert`. |
| Login Firebase | `CanchaViewModel.kt` | `FirebaseAuth`, iniciar sesión, crear cuenta, cerrar sesión. |
| Firebase y dependencias | `app/google-services.json`, `app/build.gradle.kts` | Identificador de proyecto y plugin/dependencias. |
| Configuración SQLite Cloud | `sqlitecloud.properties.ejemplo`, `app/build.gradle.kts` | Variables locales que Gradle introduce en `BuildConfig`. |
| Pantallas Compose | `MainActivity.kt` | Login, navegación por roles y pantallas. |
| Lógica torneo | `Modelo.kt` y `CanchaViewModel.kt` | Liga, eliminación directa y grupos tipo Mundial. |

Las rutas cortas (como `dto/` y `mapper/`) están debajo de `app/src/main/java/com/example/canchabolivia/`.

## 2. ¿Cómo conectar otra API? (paso a paso)

Ejemplo: «Agregar una API de estadísticas de fútbol».

1. **Interfaz:** crear `api/EstadisticasApi.kt` con Retrofit, URL base y endpoint `@GET` o `@POST`.
2. **DTO:** crear `dto/EstadisticasDto.kt` con los nombres reales del JSON (`@SerializedName`).
3. **Mapper:** crear `mapper/EstadisticasMapper.kt` para convertir JSON a una clase de dominio; validar campos que puedan faltar.
4. **Repository:** crear `repository/EstadisticasRepository.kt` que invoque la API y devuelva éxito o error.
5. **ViewModel:** añadir estado de carga, resultado y error, y una función `actualizar()`.
6. **UI:** hacer una pantalla Compose que observe el ViewModel y tenga botón Reintentar.
7. **Opcional:** guardar datos en Room con nueva Entity, DAO y migración si se requiere uso sin Internet.
8. **Gradle:** incluir librerías necesarias en `app/build.gradle.kts` solo si no están ya.

**Respuesta corta al docente:** «Aquí está `OpenMeteoApi.kt`: para otra API crearía una interfaz equivalente, DTO, mapper y Repository. Luego la llamo desde el ViewModel, y la pantalla solo observa su estado».

## 3. ¿Cómo agregar una base de datos?

### Opción A: base local Room

- Crear `database/EstadisticaEntity.kt` con `@Entity(tableName = "estadisticas")`.
- Crear `database/EstadisticaDao.kt` con `@Dao`, `@Query`, `@Insert`.
- Añadir la Entity y un getter `abstract fun estadisticaDao()` en `FutHubDatabase.kt`.
- Subir `version = 3` a `4` y escribir migración 3→4 que preserve datos existentes.
- Crear Repository y conectarlo al ViewModel. No poner SQL directamente en Compose.

### Opción B: nueva tabla de SQLite Cloud

- Crear script `sqlitecloud/05_estadisticas.sql` con la tabla y claves/relaciones.
- Crear Edge Function para consultar/escribir, con validación y autorización.
- Añadir operación en `repository/NubeRepository.kt` y, si cambia el contrato JSON, `dto/NubeDto.kt` y `mapper/NubeMapper.kt`.
- Conectar desde `CanchaViewModel.kt` (o ViewModel específico).
- Probar en SQLite Cloud, luego en Android Studio. **No basta con subir SQL a GitHub**; hay que ejecutarlo y desplegar la función.

### Opción C: nueva base Firebase/Firestore

- **Firebase Authentication no es Firestore**. Añadir Firestore exige su dependencia, reglas de seguridad, repositorio, modelos y lectura/escritura. El login que ya existe no implica tener una base Firestore.

## 4. ¿Cómo se comunican las capas?

```text
Open-Meteo -> OpenMeteoApi -> ClimaRepository -> ClimaMapper -> Room (ClimaDao)
                                                         |
ClimaCard (Compose) <- ClimaViewModel <--------------------+

MainActivity (pantallas) -> CanchaViewModel -> NubeRepository -> FutHubNubeApi
                                                          |
                                     Edge Functions -> SQLite Cloud (tablas)
                                                          |
                               NubeDto / NubeMapper -> modelos de FutHub

MainActivity (login) -> CanchaViewModel -> FirebaseAuth
```

## 5. Mejora funcional incluida: botón «Comprobar conexión SQLite Cloud»

En el login, justo debajo de `estadoNube`, se agrega un botón que llama a `vm.recargarDesdeNube()` para volver a consultar el backend. Muestra `comprobando conexion...`, `conectada` o el error existente; sin archivo de configuración continúa indicando `sin configurar`.

**Demostración:** abre `MainActivity.kt`, busca `GUIA DOCENTE [MEJORA]`, pulsa el botón. Abre `CanchaViewModel.kt` y enseña `recargarDesdeNube`, luego `NubeRepository.cargarDatos` y `FutHubNubeApi.llamar`.

Esto solo sirve como **diagnóstico simple**. No prueba por sí mismo que las escrituras, transacciones o permisos de todos los usuarios funcionan.

## 6. Preguntas rápidas

**¿API y base de datos son lo mismo?** No. API es un contrato de comunicación; base de datos es donde se persiste información. Una API puede leer o escribir en una base.

**¿Qué es Retrofit?** Cliente HTTP que transforma interfaces Kotlin anotadas con `@GET` y `@POST` en peticiones.

**¿Qué es DTO?** Estructura para representar el JSON recibido o enviado, separada de los modelos de negocio.

**¿Qué es Mapper?** Función que traduce DTO ↔ modelo o Entity para no mezclar estructura JSON con la UI.

**¿Qué es Repository?** Capa que centraliza el acceso a datos y manejo de errores.

**¿Qué es Room?** Librería de persistencia local sobre SQLite del dispositivo con Entity, DAO y Database.

**¿Por qué SQLite Cloud si ya hay Room?** Room es local; SQLite Cloud permite persistencia remota compartida por las instancias que acceden al servicio. Actualmente permanecen clases Room históricas y la caché del clima.

**¿Qué pasa si falla el Internet?** El clima puede leer la última lectura Room. La gestión del torneo puede mostrar datos de ejemplo y reportar fallos de nube; no hay una sincronización offline completa demostrada.

**¿Qué falta para una app de producción?** Proteger autorización de roles en backend (no confiar en `displayName`), no incrustar claves privilegiadas en APK, configurar restricciones y probar sincronización multiusuario. Tampoco usar un fallback a datos de ejemplo como si fueran datos reales.

**¿Qué hago si la docente me pide cambiar la URL?** `BASE_URL` en `OpenMeteoApi.kt` para clima, `sqlitecloud.properties` + `app/build.gradle.kts` para SQLite Cloud.

## 7. Recomendaciones para la demostración

- Abre `Ctrl + Shift + F`, busca `GUIA DOCENTE` y muestra los comentarios directamente en los archivos.
- Enfócate en la cadena **API → DTO → Mapper → Repository → ViewModel → Compose**.
- Recuerda que **Firebase autentica**, **SQLite Cloud guarda los datos deportivos**, y **Room conserva caché local de clima**.
- Compila en Android Studio después de aplicar cambios; no se ha ejecutado una compilación en este entorno.
