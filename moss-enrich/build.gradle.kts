plugins {
    id("info.solidsoft.pitest")
}

pitest {
    targetClasses.set(
        listOf(
            "io.github.mateokadiu.moss.enrich.place.*",
            "io.github.mateokadiu.moss.enrich.currency.*",
            "io.github.mateokadiu.moss.enrich.evidence.*",
        ),
    )
    targetTests.set(
        listOf(
            "io.github.mateokadiu.moss.enrich.place.*",
            "io.github.mateokadiu.moss.enrich.currency.*",
            "io.github.mateokadiu.moss.enrich.evidence.*",
        ),
    )
    pitestVersion.set("1.16.0")
    junit5PluginVersion.set("1.2.1")
    timestampedReports.set(false)
    threads.set(4)
    mutationThreshold.set(70)
    avoidCallsTo.set(setOf("org.slf4j", "java.util.logging"))
}

dependencies {
    implementation(project(":moss-shared"))
    implementation(project(":moss-ledger"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.2")
    implementation("org.jooq:jooq:3.19.11")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testImplementation("net.jqwik:jqwik:1.9.0")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.7")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}
