dependencies {
    implementation(project(":moss-shared"))
    implementation(project(":moss-ledger"))
    implementation("io.micrometer:micrometer-core:1.13.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.3")
    implementation("io.micrometer:micrometer-registry-otlp:1.13.3")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.7")
}
