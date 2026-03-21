pluginManagement {
    repositories {
        maven { url = uri(rootDir.resolve("local-m2")) }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri(rootDir.resolve("local-m2")) }
        google()
        mavenCentral()
    }
}

rootProject.name = "PingPlace"
include(":app")
