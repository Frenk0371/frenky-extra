version = 1
android {
    sourceSets["main"].kotlin.srcDir("../MultiSource/src/main/kotlin")
    sourceSets["main"].kotlin.include("**/guardaserie/*.kt", "**/DomainRegistry.kt")
}
cloudstream {
    description = "GuardaSerie: dominio originale verificato; catalogo e player pubblico, prova Android richiesta"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("TvSeries")
    language = "it"
    requiresResources = false
}
dependencies { testImplementation("junit:junit:4.13.2") }
