
plugins {
    base
    java
}

allprojects {
    group = "com.metaformsystems.fleet"
    version = "1.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")


    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
        implementation("org.jetbrains:annotations:26.0.2-1")
        testImplementation("org.assertj:assertj-core:3.27.4")
    }
    testing {
        suites {
            // Configure the built-in test suite
            val test by getting(JvmTestSuite::class) {
                // Use JUnit Jupiter test framework
                useJUnitJupiter("5.8.1")
            }
        }
    }
}
