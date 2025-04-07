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