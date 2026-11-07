plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "soares.lucas.joao.com.example.gamehunter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "soares.lucas.joao.com.example.gamehunter"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // View Binding: gera uma classe para cada layout XML, permitindo acessar
    // as Views pelo código sem usar findViewById()
    buildFeatures {
        viewBinding = true
    }
}

// Bibliotecas usadas pelo app:
// - appcompat: AppCompatActivity, AlertDialog e SwitchCompat
// - recyclerview: listas e grades (fileiras, cards, chips, lojas e moedas)
// - fragment-ktx: Fragments (o conteúdo da tela Principal é um Fragment)
// - constraintlayout: usado pelo Flow, que organiza os chips em linhas nos modais
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
