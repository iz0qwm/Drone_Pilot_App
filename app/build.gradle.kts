plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.kapt")
}


android {
    namespace = "com.kwos.dronepilotapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kwos.dronepilotapp"
        minSdk = 30
        targetSdk = 35
        versionCode = 28
        versionName = "2.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "VERSION_NAME", "\"${defaultConfig.versionName}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation("com.google.code.gson:gson:2.10.1")

    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-functions-ktx:21.1.1")
    implementation("com.google.firebase:firebase-appcheck:18.0.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("androidx.browser:browser:1.7.0")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.android.volley:volley:1.2.1")





    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.play.services.base)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.appset)
    implementation(libs.play.services.appsearch)
    implementation(libs.play.services.basement)
    implementation(libs.play.services.tasks)
    //implementation(libs.play.services.location.v1800)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation(files("libs/PhotoView-2.3.0.jar"))
    implementation("org.json:json:20210307")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


//apply(plugin = "com.google.gms.google-services")

