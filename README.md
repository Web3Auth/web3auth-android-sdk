# Web3Auth Android SDK

[![](https://jitpack.io/v/org.torusresearch/web3auth-android-sdk.svg)](https://jitpack.io/#org.torusresearch/web3auth-android-sdk)

Web3Auth is where passwordless auth meets non-custodial key infrastructure for Web3 apps and wallets. By aggregating OAuth (Google, Twitter, Discord) logins, different wallets and innovative Multi Party Computation (MPC) - Web3Auth provides a seamless login experience to every user on your application.

## 📖 Documentation

Checkout the official [Web3Auth Documentation](https://web3auth.io/docs) and [SDK Reference](https://web3auth.io/docs/sdk/android/) to get started!

## 💡 Features
- Plug and Play, OAuth based Web3 Authentication Service
- Fully decentralized, non-custodial key infrastructure
- End to end Whitelabelable solution
- Threshold Cryptography based Key Reconstruction
- Multi Factor Authentication Setup & Recovery (Includes password, backup phrase, device factor editing/deletion etc)
- Support for WebAuthn & Passwordless Login
- Support for connecting to multiple wallets
- DApp Active Session Management

...and a lot more

## ⏪ Requirements

- Android API version 21 or newer is required.

## ⚡ Installation

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
## 🌟 Configuration

### Configure your Web3Auth project

Hop on to the [Web3Auth Dashboard](https://dashboard.web3auth.io/) and create a new project. Use the Client ID of the project to start your integration.

![Web3Auth Dashboard](https://web3auth.io/docs/assets/images/project_plug_n_play-89c39ec42ad993107bb2485b1ce64b89.png)

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

## 💥 Initialization & Usage

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

## 🩹 Examples

Checkout the examples for your preferred blockchain and platform in our [examples](https://web3auth.io/docs/examples)

## 🌐 Demo

Checkout the [Web3Auth Demo](https://demo-app.web3auth.io/) to see how Web3Auth can be used in an application.

Further checkout the [app folder](https://https://github.com/Web3Auth/web3auth-android-sdk/tree/master/app) within this repository, which contains a sample app.

## 💬 Troubleshooting and Support

- Have a look at our [Community Portal](https://community.web3auth.io/) to see if anyone has any questions or issues you might be having. Feel free to reate new topics and we'll help you out as soon as possible.
- Checkout our [Troubleshooting Documentation Page](https://web3auth.io/docs/troubleshooting) to know the common issues and solutions.
- For Priority Support, please have a look at our [Pricing Page](https://web3auth.io/pricing.html) for the plan that suits your needs.
