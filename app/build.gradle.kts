import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.util.Properties
plugins {

    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)

}

android {
    namespace = "cse.ssuroom"
    compileSdk = 36
    buildFeatures {
        buildConfig = true // <- 이 부분 추가
    }


    defaultConfig {
        applicationId = "cse.ssuroom"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String","KAKAO_REST_API_KEY",
            gradleLocalProperties(rootDir,providers).getProperty("KAKAO_REST_API_KEY"))

    }



    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    viewBinding { enable = true }

}

dependencies {
    implementation(libs.play.services.location)
    implementation(libs.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.map.sdk)
    implementation(libs.legacy.support.v4)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.cardview:cardview:1.0.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation("de.hdodenhof:circleimageview:3.1.0")


    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}