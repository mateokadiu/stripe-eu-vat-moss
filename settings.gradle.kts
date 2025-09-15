rootProject.name = "stripe-eu-vat-moss"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

include(
    "moss-shared",
    "moss-ledger",
    "moss-enrich",
    "moss-ingest",
    "moss-file",
    "moss-observe",
    "moss-api",
    "moss-cli",
    "moss-it",
)
