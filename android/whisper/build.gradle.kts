plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.tetirua.whisper"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        version = "0.1.0"

        ndk {
            // Primeiro alvo validado: Samsung arm64-v8a. Reative outras ABIs
            // somente após cada uma ter sua biblioteca nativa verificada.
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += "-DWHISPER_CPP_DIR=${rootProject.projectDir}/../third_party/whisper.cpp"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    ndkVersion = "25.2.9519653"

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/whisper/CMakeLists.txt")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
