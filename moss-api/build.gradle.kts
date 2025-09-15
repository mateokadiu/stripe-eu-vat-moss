dependencies {
    implementation(project(":moss-shared"))
    implementation(project(":moss-ledger"))
    implementation(project(":moss-enrich"))
    implementation(project(":moss-ingest"))
    implementation(project(":moss-file"))
    implementation(project(":moss-observe"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
