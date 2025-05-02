pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "pk.eyJ1IjoianVhbmRpZWdvMTExMTYiLCJhIjoiY21hMXAwZTRuMWphbzJqb2wyNmx6NXp1NiJ9.E-xIaG7Jz635dJcQVxzpDg"
            }
        }
    }
}

rootProject.name = "mapas"
include(":app")
