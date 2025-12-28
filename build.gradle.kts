plugins {
    java
    war
}

group = "com.arekalov.islab1"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Jakarta EE API (provided by WildFly, but needed for compilation)
    compileOnly("jakarta.platform:jakarta.jakartaee-api:9.1.0")
    
    // Apache Commons DBCP2 - Connection Pool (старая версия с javax)
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("org.apache.commons:commons-pool2:2.11.1")
    
    // EclipseLink JPA провайдер (включаем в WAR)
    implementation("org.eclipse.persistence:eclipselink:3.0.3")
    
    // EclipseLink Core для нативного API (БЕЗ JPA)
    implementation("org.eclipse.persistence:org.eclipse.persistence.core:4.0.2")
    implementation("org.eclipse.persistence:org.eclipse.persistence.asm:9.4.0")
    
    // EclipseLink JPA (для работы JPA аннотаций и EntityManager)
    implementation("org.eclipse.persistence:org.eclipse.persistence.jpa:4.0.2")
    
    // Jackson dependencies
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    // PostgreSQL driver (provided by WildFly, but needed for compilation)
    compileOnly("org.postgresql:postgresql:42.7.7")
    
    // Bean Validation
    compileOnly("org.hibernate.validator:hibernate-validator:6.2.5.Final")
    
    
    // Ehcache L2 Cache
    implementation("org.ehcache:ehcache:3.10.8")
    
    // MinIO SDK for S3-compatible storage
    implementation("io.minio:minio:8.5.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
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