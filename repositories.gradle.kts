rootProject.extra.apply {
    set("androidPlugin", "com.android.tools.build:gradle:7.0.2")
    set("kotlinVersion", "1.5.30")
}

repositories {
    google()
    mavenCentral()
    jcenter()
    maven(url = "https://jitpack.io")
    maven(url = "https://maven.google.com")
}
