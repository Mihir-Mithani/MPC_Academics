pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Include all experiments as modules
rootProject.name = "MPC_Experiments"

include(
    ":Experiment1_HelloWorld",
    ":Experiment6_QuoteApp",
    ":Experiment7_SMSAutoReply",
    ":Experiment8_BluetoothScanner",
    ":Experiment9_BluetoothChat",
    ":Experiment10_WiFiSignal",
    ":Experiment11_GPSMaps",
    ":Experiment12_SensorDemo",
    ":Experiment13_FirebaseChat",
    ":Experiment14_MLKitFaceDetection"
)