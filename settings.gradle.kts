dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "nit"

pluginManagement {
    includeBuild("gradle")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
