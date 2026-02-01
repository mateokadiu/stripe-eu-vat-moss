plugins {
    id("org.springframework.boot") version "3.4.0"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

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

jib {
    from {
        image = "gcr.io/distroless/java21-debian12:nonroot"
    }
    to {
        image = providers.gradleProperty("jib.to.image").orElse("stripe-eu-vat-moss:dev").get()
    }
    container {
        ports = listOf("8080")
        jvmFlags =
            listOf(
                "-XX:+UseG1GC",
                "-XX:MaxRAMPercentage=75",
                "-XX:+ExitOnOutOfMemoryError",
            )
        environment =
            mapOf(
                "JAVA_TOOL_OPTIONS" to "-XX:+UseContainerSupport",
            )
    }
}
