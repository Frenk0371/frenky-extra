version = 2

android {
    sourceSets["main"].java.srcDir("../MultiSource/src/main/kotlin")
    sourceSets["test"].java.srcDir("../MultiSource/src/test/kotlin")
}

cloudstream {
    description = "FilmPerTutti: catalogo pubblico e lettori compatibili; verifica riproduzione in corso"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("Movie", "TvSeries")
    language = "it"
    requiresResources = false
}

dependencies { testImplementation("junit:junit:4.13.2") }
