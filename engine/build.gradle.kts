plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("benchmark")
    }
}

tasks.register<Test>("benchmark") {
    group = "verification"
    description = "Runs the ScreeningEngine latency benchmark against the <15ms NFR"
    useJUnitPlatform {
        includeTags("benchmark")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
