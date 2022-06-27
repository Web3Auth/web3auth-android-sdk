# Using Web3Auth Unity SDK
This guide will cover the basics of how to set up your Web3Auth SDK and Auth0 account for the integration and provide you with the links on how to develop a basic web application on the blockchain of your choice. Before starting, you can try the integration in our example here.

## Supported Platforms
- Windows
- IOS
- Android 

## Prerequisites
- A basic knowledge of C# and Unity Editor
- Create a Web3Auth account on the Web3Auth Dashboard
- UnityEditor 2019.x or higher With Android/iOS build support.

## Setup
### Setup your Web3Auth Dashboard
- Create a Project from the Plug and Play Section of the Web3Auth Developer Dashboard
![](https://web3auth.io/docs/assets/images/auth0-w3adashboard-01-74f7f10e5179f77ea0de5c1629c8ee2c.png)
- Enter your desired Project name
- Select the blockchain(s) you'll be building this project on. For interoperability with Torus Wallet, you've an option of allowing the user's private key to be used in other applications using Torus Wallet. We currently have this option across EVM and Solana and Casper blockchains.
- Finally, once you create the project, you've the option to whitelist your URLs for the project. Please whitelist the domains where your project will be hosted.
![](https://web3auth.io/docs/assets/images/auth0-w3adashboard-02-3050b7bbbfacabba44908b32f4ce7fd7.png)
- You will require clientId of the Plug and Play Project.

### Using the Web3Auth SDK
To use the Web3Auth SDK, you need to add the dependency of the respective platform SDK of Web3Auth to your project. To know more about the available SDKs, please have a look at this [documentation page](https://web3auth.io/docs/developing-with-web3auth/understand-sdk).

For this guide here, we will be focused on the Web3Auth Unity SDK and using the OpenLogin Provider alongside it.

#### Installation
Install SDK by downloading the latest .unitypackage file and import into your existing Unity3D project.  

> If you are using Unity Editor version 2019 or less you may encounter an error regarding `The type or namespace name 'Newtonsoft' could not be found (are you missing a using directive or an assembly reference?`. To resolve this issue, edit manifest.json file located inside <project path>/Packages/ and add a dependency `"com.unity.nuget.newtonsoft-json": "2.0.0"` in dependencies object.  

#### Initialization
Once installed, your Web3Auth application needs to be initialized. Initialisation is a multi step process where we add all the config details for Web3Auth and OpenloginAdapter. Please make sure all of this is happening in `Start()` function. This makes sure that Web3Auth is initialised when your application is loaded.

#### Configure settings for Android

#### Setting up the Web3Auth SDK
Attach `Web3Auth.cs` script to your object where you are planning to write your web3auth logic, And reference that Web3Auth in your script by following method. 
> After attaching `Web3Auth.cs` script make sure you configure it by entering client_id, redirect_url and network.
![](https://i.imgur.com/SYUEiZ8.png)

```csharp
Web3Auth web3Auth = GetComponent<Web3Auth>();
web3Auth.setOptions(new Web3AuthOptions() {
  whiteLabel = new WhiteLabelData() {
   name = "Web3Auth Sample App",
   defaultLanguage = "en"
  }
});
```

You can set different options to customize web3auth options using `Web3AuthOptions`;

####  Configure project for Android/iOS 

Unity SDK works on unity deep linking features to redirect the callback from web3auth. You have to enable/configure your custom redirect url by a tool provided with in SDK. Go to `Menu > Window > Web3Auth > Deep Link Generator`,  and enter your redirect url which you have registered in Web3Auth dashboard. 

![](https://i.imgur.com/mRfqh9y.png)

#### Authentication
##### Logging in
Add add event into Web3Auth object to catch login details. Make sure you add this event before calling login function.
```
web3auth.onLogin += onLogin;
```
provide a  callback function for login event. `Web3AuthResponse` contains all the information of user profile who successfully logged in.
```
void onLogin(Web3AuthResponse response) { 
 // your logic after successfull login 
}
```
Once initialized, you can use the `login()` function to authenticate the user when they click the login button.
```
web3auth.login()
```

##### Logout
Add add logout event into Web3Auth object. Make sure you add this event before calling logout function.
```
web3auth.onLogout += onLogout;
```
provide a  callback function for login event. `Web3AuthResponse` contains all the information of user profile who successfully logged in.
```
void onLogout() { 
 // your logic after logout 
} 
```
Once initialized, you can use the `login()` function to authenticate the user when they click the login button.
```
web3auth.logout()
```

## Try Sample scene and Example Code
To quickly explore features and working of SDK open sample scene from <project path>/Assets/Plugins/Web3AuthSDK/Sample , You can try different verifier (e.g google, facebook, github etc) for testing purpose. 

![](https://i.imgur.com/bQKglt4.png)

