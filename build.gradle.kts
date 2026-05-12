plugins {
    `java-library`
    `maven-publish`
    jacoco
}

group = "com.sweetbook"
version = "0.2.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = true
    }
}

// examples를 별도 source set으로 두어 SDK jar에 포함되지 않도록 분리
sourceSets {
    create("examples") {
        java.srcDir("examples")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
}

configurations {
    named("examplesImplementation") {
        extendsFrom(configurations["implementation"])
    }
    named("examplesRuntimeOnly") {
        extendsFrom(configurations["runtimeOnly"])
    }
    named("integrationTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("integrationTestRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

// .env 파일을 읽어 환경변수로 주입 (있을 때만)
fun loadDotEnv(): Map<String, String> {
    val envFile = file(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx < 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
        .toMap()
}

tasks.named<ProcessResources>("processIntegrationTestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 각 examples 클래스를 ./gradlew run<Name> --args="..." 로 실행할 수 있도록 등록.
// .env가 있으면 환경변수로 자동 주입 (BOOKPRINT_API_KEY 등).
listOf(
    "SimpleBooks",
    "SimpleOrders",
    "ServerPipeline",
    "WebhookReceiver",
    "HelpersExample"
).forEach { name ->
    tasks.register<JavaExec>("run$name") {
        group = "examples"
        description = "examples/$name 실행. --args=\"...\" 로 인자 전달."
        classpath = sourceSets["examples"].runtimeClasspath
        mainClass = "com.sweetbook.bookprintapi.examples.$name"
        // toolchain Java 11 사용 — system Java(예: 22)의 cacerts 불일치 방지
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(11)
        }
        // Windows 콘솔 한글 출력 깨짐 방지
        jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
        loadDotEnv().forEach { (k, v) -> environment(k, v) }
    }
}

tasks.register<Test>("integrationTest") {
    description = "Sandbox API에 실호출하는 통합 테스트. .env의 BOOKPRINT_API_KEY 또는 환경변수 필요."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter("test")
    val dotEnv = loadDotEnv()
    dotEnv.forEach { (k, v) -> environment(k, v) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).charSet("UTF-8")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "bookprintapi-sdk"
            pom {
                name = "BookPrintAPI Java SDK"
                description = "포토북 생성/주문 API Java SDK (server v1)"
                url = "https://github.com/sweet-book/bookprintapi-java-sdk"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
            }
        }
    }
}
