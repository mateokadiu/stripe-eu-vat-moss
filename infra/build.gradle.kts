plugins {
    application
}

application {
    mainClass.set("io.github.mateokadiu.moss.infra.MossStack")
}

dependencies {
    implementation("com.pulumi:pulumi:0.16.1")
    implementation("com.pulumi:oci:2.18.0")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("infra")
    manifest {
        attributes(
            "Main-Class" to "io.github.mateokadiu.moss.infra.MossStack",
        )
    }
}
