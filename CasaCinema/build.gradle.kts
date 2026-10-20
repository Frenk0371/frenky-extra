version = 9

android {
    sourceSets["main"].kotlin.srcDir("../MultiSource/src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("../MultiSource/src/test/kotlin")
}

cloudstream {
    description = "CasaCinema: catalogo pubblico e lettori compatibili; TEST TV: lettore web alternativo"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("Movie", "TvSeries")
    language = "it"
    requiresResources = false
}

dependencies { testImplementation("junit:junit:4.13.2") }
