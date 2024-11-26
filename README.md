# Web3Auth Android SDK

![SDK Version](https://jitpack.io/v/org.torusresearch/web3auth-android-sdk.svg)

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

- Android API version 24 or newer is required.
- `compileSdkVersion` needs to be 34

## ⚡ Installation

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
    implementation 'com.github.web3auth:web3auth-android-sdk:9.0.0'
}
```

## 🌟 Configuration
Checkout [SDK Reference](https://web3auth.io/docs/sdk/pnp/android/install#update-permissions) to configure for Android. 

## 💥 Getting Started

```kotlin
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.Web3AuthOptions

class MainActivity : AppCompatActivity() {
    // ...
    private lateinit var web3Auth: Web3Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        web3Auth = Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = "YOUR_WEB3AUTH_CLIENT_ID", // Pass over your Web3Auth Client ID from Developer Dashboard
                network = Network.MAINNET,
                redirectUrl = Uri.parse("{YOUR_APP_PACKAGE_NAME}://auth"),
            )
        )

        // Handle user signing in when app is not alive
        web3Auth.setResultUrl(intent?.data)
    }

    override fun onResume() {
        super.onResume()
        if (Web3Auth.getCustomTabsClosed()) {
            Toast.makeText(this, "User closed the browser.", Toast.LENGTH_SHORT).show()
            web3Auth.setResultUrl(null)
            Web3Auth.setCustomTabsClosed(false)
        }
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
        
        loginCompletableFuture.whenComplete { _, error ->
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

Checkout the examples for your preferred blockchain and platform in our [examples](https://github.com/Web3Auth/web3auth-pnp-examples/tree/main/android)

## 🌐 Demo

Checkout the [Web3Auth Demo](https://demo-app.web3auth.io/) to see how Web3Auth can be used in an application.

Have a look at our [Web3Auth PnP Android Quick Start](https://github.com/Web3Auth/web3auth-pnp-examples/tree/main/android/android-quick-start) to help you quickly integrate a basic instance of Web3Auth Plug and Play in your Android app.

Further checkout the [app folder](https://https://github.com/Web3Auth/web3auth-android-sdk/tree/master/app) within this repository, which contains a sample app.

## 💬 Troubleshooting and Support

- Have a look at our [Community Portal](https://community.web3auth.io/) to see if anyone has any questions or issues you might be having. Feel free to reate new topics and we'll help you out as soon as possible.
- Checkout our [Troubleshooting Documentation Page](https://web3auth.io/docs/troubleshooting) to know the common issues and solutions.
- For Priority Support, please have a look at our [Pricing Page](https://web3auth.io/pricing.html) for the plan that suits your needs.
