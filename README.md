# Web3Auth Unity SDK

[![](https://jitpack.io/v/org.torusresearch/web3auth-unity-sdk.svg)](https://jitpack.io/#org.torusresearch/web3auth-unity-sdk)

Web3Auth is where passwordless auth meets non-custodial key infrastructure for Web3 apps and wallets. By aggregating OAuth (Google, Twitter, Discord) logins, different wallets and innovative Multi Party Computation (MPC) - Web3Auth provides a seamless login experience to every user on your application.

## 📖 Documentation

Checkout the official [Web3Auth Documentation](https://web3auth.io/docs) and [SDK Reference](https://web3auth.io/docs/sdk/unity/) to get started!

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

- Unity Editor 2019.4.9f1 or greater
- .Net Framework 4.x

## ⚡ Installation

Download [.unitypackage](https://github.com/Web3Auth/web3auth-unity-sdk/releases/latest) and import the package file into your existing Unity3D project.

> You may encounter errors when importing this package into your existing project.
> `The type or namespace name 'Newtonsoft' could not be found (are you missing a using directive or an assembly reference?)`
> To fix this problem you need to add the following line into dependencies object which is inside the `Packages/manifest.json` file.

```
"com.unity.nuget.newtonsoft-json": "2.0.0"
```

![Json Dot Net Error](./Images/JsonDotNet%20Error.png)

## 🌟 Configuration

To get started, open a sample scene `Web3AuthSample` inside `Assets/Plugins/Web3AuthSDK/Samples/Web3AuthSample.scene`

Before building the application for Android/IOS you need to register the redirect_uri which can be done easily by the tool provided inside the SDK. To achieve that, you need to follow the steps mentioned below.

- Open deep link generator tool provided by Web3Auth Unity SDK from "Window > Web3Auth > Deep Link Generator"
  ![Deep Link Generator](./Images/Deep%20Link%20Generator.png)
- Enter the redirect_url _(i-e torusapp://com.torus.Web3AuthUnity/auth)_ and click generate.
  > To use your own client_id , register your app on [https://web3auth.io/](https://web3auth.io/) and replace the client_id inside `Assets/Plugins/Web3AuthSDK/Samples/Web3AuthSample.cs` script.

### Configure an Web3Auth project

Go to [Developer Dashboard](https://dashboard.web3auth.io/), create or select an Web3Auth project:

- Add {{SCHEMA}}://{YOUR_APP_PACKAGE_NAME}://auth to Whitelist URLs.
  _i-e torusapp://com.torus.Web3AuthUnity/auth_
- Copy the Project ID for later usage as client_id

## 💥 Initialization & Usage

In your sign-in script', create an Web3Auth instance with your Web3Auth project's configurations and configure it like this:

```csharp
Web3Auth web3Auth = new Web3Auth(new Web3AuthOptions() {
  redirectUrl = new Uri("torusapp://com.torus.Web3AuthUnity/auth"),
    clientId = "BAwFgL-r7wzQKmtcdiz2uHJKNZdK7gzEf2q-m55xfzSZOw8jLOyIi4AVvvzaEQO5nv2dFLEmf9LBkF8kaq3aErg",
    network = Web3Auth.Network.TESTNET,
    whiteLabel = new WhiteLabelData() {
      name = "Web3Auth Sample App",
        logoLight = null,
        logoDark = null,
        defaultLanguage = "en",
        dark = true,
        theme = new Dictionary < string, string > {
          {
            "primary",
            "#123456"
          }
        }
    }
});
web3Auth.onLogin += onLogin;
web3Auth.onLogout += onLogout;
private void onLogin(Web3AuthResponse response) {
    // Handle user signing in
}
private void onLogout() {
  // Handle user signing out
}
```

### Simulate redirect callback inside Unity Editor

Web3Auth Unity SDK provides a tool to simulate the redirect callback. To open go to _"Window > Web3Auth > Debug Deep Link"_ and paste the redirect uri (it must include the response code)

![Deep Link Debug](./Images/Deep%20Link%20Debug.png)


## 🩹 Examples

Checkout the examples for your preferred blockchain and platform in our [examples](https://web3auth.io/docs/examples)

## 🌐 Demo

Checkout the [Web3Auth Demo](https://demo-app.web3auth.io/) to see how Web3Auth can be used in an application.

Further checkout the [samples folder in the Web3Auth SDK](https://github.com/Web3Auth/web3auth-unity-sdk/tree/master/Assets/Plugins/Web3AuthSDK/Samples) within this repository, which contains a sample app.

## 💬 Troubleshooting and Support

- Have a look at our [Community Portal](https://community.web3auth.io/) to see if anyone has any questions or issues you might be having. Feel free to reate new topics and we'll help you out as soon as possible.
- Checkout our [Troubleshooting Documentation Page](https://web3auth.io/docs/troubleshooting) to know the common issues and solutions.
- For Priority Support, please have a look at our [Pricing Page](https://web3auth.io/pricing.html) for the plan that suits your needs.
