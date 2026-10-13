version = 1
android {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/domain-registry"))
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

val copyDomainRegistry by tasks.registering(Copy::class) {
    from("../MultiSource/src/main/kotlin") { include("**/DomainRegistry.kt") }
    into(layout.buildDirectory.dir("generated/domain-registry"))
}
tasks.named("preBuild") { dependsOn(copyDomainRegistry) }
