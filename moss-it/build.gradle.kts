dependencies {
    testImplementation(project(":moss-shared"))
    testImplementation(project(":moss-ledger"))
    testImplementation(project(":moss-enrich"))
    testImplementation(project(":moss-ingest"))
    testImplementation(project(":moss-file"))
    testImplementation(project(":moss-observe"))
    testImplementation(project(":moss-api"))

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.7")
}
