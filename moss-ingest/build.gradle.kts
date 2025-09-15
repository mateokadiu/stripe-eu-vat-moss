dependencies {
    implementation(project(":moss-shared"))
    implementation(project(":moss-ledger"))
    implementation(project(":moss-enrich"))
    implementation("com.stripe:stripe-java:28.4.0")
    implementation("com.opencsv:opencsv:5.9")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.7")
}
