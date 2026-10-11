version = 4

android {
    sourceSets["main"].kotlin.srcDir("../MultiSource/src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("../MultiSource/src/test/kotlin")
}

cloudstream {
    description = "CasaCinema: catalogo pubblico e lettori compatibili; verifica riproduzione in corso"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("Movie", "TvSeries")
    language = "it"
    requiresResources = false
}

dependencies { testImplementation("junit:junit:4.13.2") }
