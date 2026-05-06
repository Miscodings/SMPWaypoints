plugins {
  with(libs.plugins) {
    alias(kotlin)
    alias(shadow)
    alias(minotaur)
    alias(runPaper)
    alias(changelog)
  }
}

val release = (project.property("release") as String).toBoolean()

description = "Waypoints plugin"

dependencies {
  implementation(libs.paper)
  implementation(libs.stdlib)
  implementation(libs.coroutines)

  compileOnly(libs.annotations)

  // Dependencies on own projects
  implementation(libs.md5Commons)
  implementation(libs.kinvs)
  implementation(libs.paperBrigadier)
  implementation(libs.sqlHelper)

  // Required dependencies
  implementation(libs.schedulers)
  implementation(libs.skedule)
  implementation(libs.anvilGui)
  implementation(libs.bStats)
  implementation(libs.configurate.yaml)
  implementation(libs.configurate.core)
  implementation(libs.configurate.kotlin)

  // Optional dependencies
  implementation(libs.vaultApi)

  implementation(variantOf(libs.dynmap.coreApi) { classifier("all") })
  implementation(variantOf(libs.dynmap.api) { classifier("unshaded") }) { isTransitive = false }
  implementation(libs.squaremapApi)
  implementation(libs.bluemapApi)
  implementation(libs.pl3xmap)
  implementation(libs.geyser)

  // Testing
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.junitJupiter)
  testImplementation(libs.mockBukkit)
  testRuntimeOnly(libs.sqliteJdbc)
  testRuntimeOnly(libs.junitLauncher)
}

tasks {
  register("createResourceIndex") {
    val resourcesDir = file("src/main/resources")
    val indexFile = layout.buildDirectory.file("resources/main/resourceIndex")
    inputs.dir(resourcesDir)
    outputs.file(indexFile)
    doLast {
      val index = indexFile.get().asFile
      index.parentFile.mkdirs()
      index.writeText(
          resourcesDir.walkTopDown().filter { it.isFile }
              .joinToString("\n") { it.relativeTo(resourcesDir).invariantSeparatorsPath })
    }
  }

  processResources {
    dependsOn("createResourceIndex")

    val properties =
        mapOf(
            "version" to project.version,
            "apiVersion" to libs.versions.paper.get().substringBefore('-'),
            "kotlinVersion" to libs.versions.kotlin.get(),
            "coroutinesVersion" to libs.versions.coroutines.get(),
        )

    inputs.properties(properties)

    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") { expand(properties) }
  }

  shadowJar {
    archiveClassifier = ""

    minimize {
      if (!release) {
        exclude(dependency(libs.stdlib.get()))
        exclude(dependency(libs.coroutines.get()))
      }
    }

    exclude(
        "META-INF/*.kotlin_module",
        "META-INF/proguard/",
        "META-INF/com.android.tools/",
        "META-INF/services/",
    )
    if (release) {
      exclude("DebugProbesKt.bin")
    }

    dependencies {
      include(dependency(libs.stdlib.get()))
      include(dependency(libs.coroutines.get()))

      include(dependency(libs.md5Commons.get()))
      include(dependency(libs.kinvs.get()))
      include(dependency(libs.paperBrigadier.get()))
      include(dependency(libs.sqlHelper.get()))

      include(dependency(libs.configurate.yaml.get()))
      include(dependency(libs.configurate.core.get()))
      include(dependency(libs.configurate.kotlin.get()))

      include(dependency(libs.schedulers.get()))
      include(dependency(libs.skedule.get()))
      include(dependency(libs.anvilGui.get()))
      include(dependency("org.bstats:.*"))
    }

    // External md5lukas libraries shaded into the jar
    arrayOf("commons", "kinvs", "konfig", "schedulers", "paper.brigadier", "jdbc").forEach {
      relocate("de.md5lukas.$it", "me.miscodes.waypoints.libs.${it.substringAfterLast('.')}")
    }
    val normalRelocations =
        mutableListOf(
            "com.okkero.skedule",
            "net.wesjd.anvilgui",
            "org.bstats",
            "org.spongepowered.configurate",
        )

    if (release) {
      normalRelocations.add("kotlinx.coroutines")
      normalRelocations.add("kotlin")
      normalRelocations.add("_COROUTINE") // TODO maybe or maybe not (probably not)
    }

    normalRelocations.forEach {
      relocate(it, "me.miscodes.waypoints.libs.${it.substringAfterLast('.')}")
    }

    manifest { attributes("paperweight-mappings-namespace" to "mojang+yarn") }
  }

  runServer { minecraftVersion(libs.versions.paperTestServer.get().substringBefore('-')) }

  test { useJUnitPlatform() }
}

runPaper.folia.registerTask()

kotlin { jvmToolchain(libs.versions.jvmToolchain.get().toInt()) }

changelog { path = rootProject.relativePath("CHANGELOG.md") }

modrinth {
  val modrinthToken: String? by project

  token = modrinthToken

  projectId = "waypoints"
  versionType = "release"
  uploadFile.set(tasks.shadowJar)

  gameVersions.addAll(libs.versions.paper.get().substringBefore('-'))
  loaders.addAll("paper", "folia")

  syncBodyFrom = provider { rootProject.file("README.md").readText() }

  changelog = provider {
    with(project.changelog) { renderItem(getLatest().withEmptySections(false).withHeader(false)) }
  }

  dependencies {
    with(optional) {
      project("pl3xmap")
      project("bluemap")
      project("squaremap")
      project("dynmap")
    }
  }

  debugMode = false
}
