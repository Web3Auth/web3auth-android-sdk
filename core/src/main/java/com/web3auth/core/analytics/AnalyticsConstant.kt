package com.web3auth.core.analytics

object AnalyticsEvents {
    const val SDK_INITIALIZATION_COMPLETED = "SDK Initialization Completed"
    const val SDK_INITIALIZATION_FAILED = "SDK Initialization Failed"
    const val CONNECTION_STARTED = "Connection Started"
    const val CONNECTION_COMPLETED = "Connection Completed"
    const val CONNECTION_FAILED = "Connection Failed"
    const val MFA_ENABLEMENT_STARTED = "MFA Enablement Started"
    const val MFA_ENABLEMENT_COMPLETED = "MFA Enablement Completed"
    const val MFA_ENABLEMENT_FAILED = "MFA Enablement Failed"
    const val MFA_MANAGEMENT_STARTED = "MFA Management Started"
    const val MFA_MANAGEMENT_FAILED = "MFA Management Failed"
    const val MFA_MANAGEMENT_COMPLETED = "MFA Management Completed"
    const val WALLET_UI_CLICKED = "Wallet UI Clicked"
    const val WALLET_SERVICES_FAILED = "Wallet Services Failed"
    const val LOGOUT_STARTED = "Logout Started"
    const val LOGOUT_COMPLETED = "Logout Completed"
    const val LOGOUT_FAILED = "Logout Failed"
    const val REQUEST_FUNCTION_STARTED = "Request Function Started"
    const val REQUEST_FUNCTION_COMPLETED = "Request Function Completed"
    const val REQUEST_FUNCTION_FAILED = "Request Function Failed"

    const val ANDROID_SDK_VERSION = "10.0.0"
}

object AnalyticsSdkType {
    const val ANDROID = "android"
    const val FLUTTER = "flutter"
}

object SegmentKeys {
    const val SEGMENT_WRITE_KEY = "f6LbNqCeVRf512ggdME4b6CyflhF1tsX" // Production key
    const val SEGMENT_WRITE_KEY_DEV = "rpE5pCcpA6ME2oFu2TbuVydhOXapjHs3" // Development key
}


