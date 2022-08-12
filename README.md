# web3auth-android-sdk

[![](https://jitpack.io/v/org.torusresearch/web3auth-android-sdk.svg)](https://jitpack.io/#org.torusresearch/web3auth-android-sdk)

Web3Auth's Android SDK for applications.

`web3auth-android-sdk` is a client-side library you can use with your Android app to authenticate users using [Web3Auth](https://web3auth.io/).

## Requirements

Android API version 21 or newer is required.

## Installation

### Add Web3Auth to Gradle

In your project-level `settings.gradle` file, add JitPack repository:

```groovy
dependencyResolutionManagement {
     repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
     repositories {
         google()
         mavenCentral()
         maven { url "https://jitpack.io" } // <-- Add this line
     }
}
```

Then, in your app-level `build.gradle` dependencies section, add the following:

```groovy
dependencies {
    // ...
    implementation 'org.torusresearch:web3auth-android-sdk:-SNAPSHOT'
}
```

### Permissions

Open your app's `AndroidManifest.xml` file and add the following permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Integrating

### Configure an Web3Auth project

Go to [Developer Dashboard](https://dashboard.web3auth.io/), create or select an Web3Auth project:

- Add `{YOUR_APP_PACKAGE_NAME}://auth` to **Whitelist URLs**.

- Copy the Project ID for usage later.

### Configure Deep Link 

Open your app's `AndroidManifest.xml` file and add the following deep link intent filter to your sign-in activity:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Accept URIs: {YOUR_APP_PACKAGE_NAME}://* -->
    <data android:scheme="{YOUR_APP_PACKAGE_NAME}" />
</intent-filter>
```

Make sure your sign-in activity launchMode is set to **singleTop** in your `AndroidManifest.xml`

```xml
<activity
  android:launchMode="singleTop"
  android:name=".YourActivity">
  // ...
</activity>
```

### Initialize Web3Auth

In your sign-in activity', create an `Web3Auth` instance with your Web3Auth project's configurations and 
configure it like this:

```kotlin
class MainActivity : AppCompatActivity() {
    // ...
    private lateinit var web3Auth: Web3Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web3Auth = Web3Auth(
            Web3AuthOptions(context = this,
                clientId = getString(R.string.web3auth_project_id),
                network = Web3Auth.Network.MAINNET,
                redirectUrl = Uri.parse("{YOUR_APP_PACKAGE_NAME}://auth"),
                whiteLabel = WhiteLabelData(  // Optional param
                    "Web3Auth Sample App", null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                )
            )
        )

        // Handle user signing in when app is not alive
        web3Auth.setResultUrl(intent?.data)
        
        // ...
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Handle user signing in when app is active
        web3Auth.setResultUrl(intent?.data)

        // ...
    }

    private fun onClickLogin() {
        val selectedLoginProvider = Provider.GOOGLE   // Can be Google, Facebook, Twitch etc
        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(LoginParams(selectedLoginProvider))
        
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                // render logged in UI
            } else {
                // render login error UI
            }

        }
    }
    
    //...
}
```

## API Reference

```kotlin
class Web3Auth(
    var web3AuthOptions: Web3AuthOptions
) {    
    // Trigger login flow using login params. Specific Login Provider can be set through Login Params
    fun login(
        loginParams: LoginParams,
    ) {}
} 

data class Web3AuthOptions(
    context: Context, // Android context to launch Web-based authentication, usually is the current activity
    clientId: String, // Your Web3Auth project ID
    network: Network, // Network to run Web3Auth, either MAINNET or TESTNET
    redirectUrl: Uri? = null, // URL that Web3Auth will redirect API responses
    whiteLabel: WhiteLabelData? = null,  // Optional param to configure look and feel of web3uth login page
    loginConfig: HashMap<String, LoginConfigItem>? = null, // Optional
)

data class LoginParams (
    val loginProvider: Provider,
    val dappShare: String? = null,
    val extraLoginOptions: ExtraLoginOptions? = null,
    val redirectUrl: Uri? = null,
    val appState: String? = null,
    val mfaLevel: MFALevel? = null,
    val sessionTime: Int? = null,
    val curve: Curve? = null
)

enum class Provider {
    @SerializedName("google")GOOGLE,
    @SerializedName("facebook")FACEBOOK,
    @SerializedName("reddit")REDDIT,
    @SerializedName("discord")DISCORD,
    @SerializedName("twitch")TWITCH,
    @SerializedName("apple")APPLE,
    @SerializedName("line")LINE,
    @SerializedName("github")GITHUB,
    @SerializedName("kakao")KAKAO,
    @SerializedName("linkedin")LINKEDIN,
    @SerializedName("twitter")TWITTER,
    @SerializedName("weibo")WEIBO,
    @SerializedName("wechat")WECHAT,
    @SerializedName("email_passwordless")EMAIL_PASSWORDLESS,
    @SerializedName("jwt")JWT
}

```

## 💬 Troubleshooting and Discussions

- Have a look at our [GitHub Discussions](https://github.com/Web3Auth/Web3Auth/discussions?discussions_q=sort%3Atop) to see if anyone has any questions or issues you might be having.
- Join our [Discord](https://discord.gg/web3auth) to join our community and get private integration support or help with your integration.
