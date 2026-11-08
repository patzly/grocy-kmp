plugins {
    alias(libs.plugins.grocy.android.application)
}

dependencies {
    implementation(projects.apps.shared)
    // Closed source, which is why only this app may use it
    implementation(projects.core.scannerMlkit)
    implementation(projects.core.updatePlay)
    implementation(projects.core.wear)
    implementation(libs.play.services.wearable)
}

// ML Kit creates its registrars by reflection; R8 removing their constructors only crashes at
// runtime (scanning with ML Kit), so the minified builds check its list of removed code instead
listOf("Release", "Nightly").forEach { variant ->
    val usage = layout.buildDirectory.file("outputs/mapping/${variant.lowercase()}/usage.txt")
    tasks.matching { it.name == "minify${variant}WithR8" }.configureEach {
        doLast {
            val removed = usage.get().asFile.readLines()
                .zipWithNext()
                .filter { (owner, member) ->
                    owner.startsWith("com.google.mlkit.") && owner.endsWith("Registrar:") &&
                        member.trim() == "public void <init>()"
                }
                .map { (owner, _) -> owner.removeSuffix(":") }
            check(removed.isEmpty()) { "R8 removed the constructors of $removed" }
        }
    }
}
