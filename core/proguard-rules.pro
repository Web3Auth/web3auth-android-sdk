-keep class com.web3auth.core.* {*;}
-keep class com.web3auth.core.** {*;}
-keepclassmembers class com.web3auth.core.**

##### okhttp3
# okHttp3
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-keep class okhttp3.Headers { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class com.web3auth.session_manager_android.**

#### GSON
# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

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