version = 4
android {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/domain-registry").get().asFile)
}
cloudstream {
    description = "GuardaSerie: voce Frenky Extra distinta per evitare collisioni; player web verificato sul campione utente"
    authors = listOf("Frenky")
    status = 3
    tvTypes = listOf("TvSeries")
    language = "it"
    requiresResources = false
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    compileOnly("org.jspecify:jspecify:1.0.0")
    testImplementation("org.jspecify:jspecify:1.0.0")
}

val copyDomainRegistry by tasks.registering(Copy::class) {
    from("../MultiSource/src/main/kotlin") { include("**/DomainRegistry.kt") }
    into(layout.buildDirectory.dir("generated/domain-registry"))
}
tasks.named("preBuild") { dependsOn(copyDomainRegistry) }
