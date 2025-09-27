plugins {
    kotlin("jvm") version "1.9.10"
    war
}

group = "com.arekalov.islab1"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    
    // Jakarta EE API (provided by WildFly, but needed for compilation)
    implementation("jakarta.platform:jakarta.jakartaee-api:9.1.0")
    
    // JAX-RS implementation (Jersey - provided by WildFly)
    compileOnly("org.glassfish.jersey.core:jersey-server:3.0.8")
    compileOnly("org.glassfish.jersey.containers:jersey-container-servlet:3.0.8")
    
    // EclipseLink JPA
    implementation("org.eclipse.persistence:eclipselink:3.0.3")
    
    // PostgreSQL driver (provided by WildFly, but needed for compilation)
    compileOnly("org.postgresql:postgresql:42.7.7")
    
    // JSON processing with Kotlin support
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    
    // Bean Validation
    implementation("org.hibernate.validator:hibernate-validator:6.2.5.Final")
    
    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.war {
    archiveFileName.set("is-lab1.war")
}