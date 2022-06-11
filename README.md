# web3auth-unity-sdk
[![](https://jitpack.io/v/org.torusresearch/web3auth-unity-sdk.svg)](https://jitpack.io/#org.torusresearch/web3auth-unity-sdk)
Torus Web3Auth SDK for Unity3D applications (Android/iOS/Windows).
`web3auth-unity-sdk` is a client-side library you can use with your Unity3D app to authenticate users using [Web3Auth](https://web3auth.io/).
## Requirements
- Unity Editor 2019.4.9f1 or greater
- Android SDK min 18
- .Net Framework 4.x
## Installation
Download [.unitypackage]() for Web3Auth SDk, Import package file in to your existing Unity3D project.

> While importing the unity package you encountered error related to `The type or namespace name 'Newtonsoft' could not be found (are you missing a using directive or an assembly reference?)` then add the following line into dependencies object in  `Packages/manifest.json` file.
```
"com.unity.nuget.newtonsoft-json": "2.0.0"
```
![Json Dot Net Error](./Images/JsonDotNet%20Error.png)

## Sample
You can check the pre-built sample to quickly test the Web3Auth authentication. To open sample scene goto `Assets/Plugins/Web3AuthSDK/Samples/Web3AuthSample.scene`
Before building the project for Android/iOS you have to register the redirect_uri into sdk. To do that do following steps.
- Open deep link generator tool provided by Web3Auth Unity SDK from "Window > Web3Auth > Deep Link Generator"
![Deep Link Generator](./Images/Deep%20Link%20Generator.png)
- Enter the redirect_url *(i-e torusapp://com.torus.Web3AuthUnity/auth)* and click generate.
> To use your own client_id , register your app on [https://web3auth.io/](https://web3auth.io/) and replace the client_id in `Assets/Plugins/Web3AuthSDK/Samples/Web3AuthSample.cs` script.
## Integration
### Configure an Web3Auth project
Go to [Developer Dashboard](https://dashboard.web3auth.io/), create or select an Web3Auth project:
- Add {{SCHEMA}}://{YOUR_APP_PACKAGE_NAME}://auth to Whitelist URLs. 
*i-e torusapp://com.torus.Web3AuthUnity/auth*
- Copy the Project ID for usage later as client_id
### Intialize Web3Auth
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

### Simulate redirect callback in Unity Editor
Web3Auth Unity SDK provides a tool to tsimultate the redirect callback. To open go to *"Window > Web3Auth > Debug Deep Link"* and paste the redirect uri (it must include the response code)

![Deep Link Debug](./Images/Deep%20Link%20Debug.png)


## API Reference
```csharp
class Web3Auth {
  Web3Auth(Web3AuthOptions web3AuthOptions) {}
  // Trigger login flow that shows a modal for user to select one of supported
  // providers to login, e.g. Google, Facebook, Twitter, Passwordless, etc
  void login() {}
  // Trigger login flow using login params. Specific Login Provider can be set
  // through Login Params
  void login(LoginParams loginParams) {}
}
class Web3AuthOptions {
  string clientId;  // Your Web3Auth project ID
  public Web3Auth.Network network;    // Network to run Web3Auth, either MAINNET or TESTNET
  public Uri redirectUrl;  // URL that Web3Auth will redirect API responses
  public WhiteLabelData whiteLabel;  // Optional param to configure look
  public Dictionary<string, LoginConfigItem> loginConfig;  // Optional
}
public class LoginParams {
  public Provider loginProvider;
  public bool ? relogin;
  public string dappShare;
  public ExtraLoginOptions;
  public Uri redirectUrl;
  public string appState;
  public MFALevel mfaLevel;
}
```