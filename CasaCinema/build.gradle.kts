version = 11

android {
    sourceSets["main"].kotlin.srcDir("../MultiSource/src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("../MultiSource/src/test/kotlin")
}

cloudstream {
    description = "CasaCinema: catalogo pubblico e lettori compatibili; TV TEST v11: diagnostica struttura pagina"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("Movie", "TvSeries")
    language = "it"
    requiresResources = false
}

dependencies { testImplementation("junit:junit:4.13.2") }
