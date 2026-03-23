pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri(rootDir.resolve("local-m2")) }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri(rootDir.resolve("local-m2")) }
    }
}

rootProject.name = "PingPlace"
include(":app")
