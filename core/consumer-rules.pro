# Keep the Web3Auth core classes and methods
-keep class com.web3auth.core.** { *; }
-keep class com.web3auth.core.types.ChainConfig { *; }
-keep class com.web3auth.core.types.** { *; }
-keep class com.web3auth.core.api.** { *; }

-keepattributes *Annotation*
-keep class com.google.gson.annotations.SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all JsonElement types from Gson
-keep class com.google.gson.JsonArray { *; }
-keep class com.google.gson.JsonObject { *; }
-keep class com.google.gson.JsonElement { *; }

# Preserve annotations and parameter names
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# If using Gson or any reflection-based serialization:
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep the Web3Auth class and prevent any obfuscation or stripping
-keep class com.web3auth.core.Web3Auth {
    *;
}

-keepclassmembers class com.web3auth.core.Web3Auth$Companion {
    public *;
}
-keep class com.web3auth.core.Web3Auth {
    public static final com.web3auth.core.Web3Auth$Companion Companion;
}
-keep interface com.web3auth.core.types.WebViewResultCallback {
    *;
}

-keep class com.web3auth.core.types.Web3AuthError {
    *;
}
-keepclassmembers class com.web3auth.core.types.Web3AuthError {
    public static <methods>;
}
-keep enum com.web3auth.core.types.ErrorCode {
    *;
}
