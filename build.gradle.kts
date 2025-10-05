plugins {
    java
    war
}

group = "com.arekalov.islab1"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Jakarta EE API (provided by WildFly, but needed for compilation)
    compileOnly("jakarta.platform:jakarta.jakartaee-api:9.1.0")
    
    // EclipseLink JPA (версия для Jakarta EE 9+) - только JPA, без JAX-RS
    implementation("org.eclipse.persistence:org.eclipse.persistence.jpa:4.0.2")
    implementation("org.eclipse.persistence:org.eclipse.persistence.core:4.0.2")
    implementation("org.eclipse.persistence:org.eclipse.persistence.asm:9.4.0")
    // PostgreSQL driver (provided by WildFly, but needed for compilation)
    compileOnly("org.postgresql:postgresql:42.7.7")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")
    
    // Bean Validation
    compileOnly("org.hibernate.validator:hibernate-validator:6.2.5.Final")
    
    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:4.11.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.war {
    archiveFileName.set("is-lab1.war")
}