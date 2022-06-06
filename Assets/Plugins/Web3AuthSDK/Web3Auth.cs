using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Text;
using System.Linq;
using UnityEngine;
using System.Web;
using System.Net;
using System.Threading;
using System.IO;
using Web3AuthSDK.Windows;
/*
https://docs.unity3d.com/2021.2/Documentation/Manual/deep-linking-ios.html
https://docs.unity3d.com/Manual/deep-linking-android.html
*/

public class Web3Auth
{
    public enum Network
    {
        MAINNET, TESTNET, CYAN
    }

    private Web3AuthOptions web3AuthOptions;
    private Dictionary<string, object> initParams;

    private Web3AuthResponse web3AuthResponse;

#if UNITY_STANDALONE_WIN && !UNITY_EDITOR
    private static event Action<Uri> onUriRecieved;
#endif

    public event Action<Web3AuthResponse> onLogin;
    public event Action onLogout;

    public Web3Auth(Web3AuthOptions web3AuthOptions)
    {

        this.web3AuthOptions = web3AuthOptions;

        this.initParams = new Dictionary<string, object>();

        this.initParams["clientId"] = this.web3AuthOptions.clientId;
        this.initParams["network"] = this.web3AuthOptions.network.ToString().ToLower();

        if (this.web3AuthOptions.redirectUrl != null)
            this.initParams["redirectUrl"] = this.web3AuthOptions.redirectUrl;

        if (this.web3AuthOptions.whiteLabel != null)
            this.initParams["whiteLabel"] = JsonConvert.SerializeObject(this.web3AuthOptions.whiteLabel);

        if (this.web3AuthOptions.loginConfig != null)
            this.initParams["loginConfig"] = JsonConvert.SerializeObject(this.web3AuthOptions.loginConfig);


        Application.deepLinkActivated += onDeepLinkActivated;
        if (!string.IsNullOrEmpty(Application.absoluteURL))
            onDeepLinkActivated(Application.absoluteURL);

#if UNITY_STANDALONE_WIN && !UNITY_EDITOR
        onUriRecieved += (Uri url) =>
        {
            this.setResultUrl(url);
        };

        var buildPath = System.IO.Path.GetDirectoryName(
            System.IO.Path.GetDirectoryName(Application.dataPath + "/file.example")
        );

        Protocol.RegisterURL(web3AuthOptions.redirectUrl.Scheme, buildPath + "\\" + Application.productName + ".exe");
#endif
#if UNITY_EDITOR
        Web3AuthSDK.Editor.Web3AuthDebug.onURLRecieved += (Uri url) =>
        {
            this.setResultUrl(url);
        };
#endif
    }

    private void onDeepLinkActivated(string url)
    {
        this.setResultUrl(new Uri(url));
    }

#if UNITY_STANDALONE_WIN && !UNITY_EDITOR
    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSplashScreen)]
    static void OnBeforeSplashScreen()
    {
        Debug.LogError("Debug Session");
        Uri uri = null;

        try
        {
            if (Environment.GetCommandLineArgs().Length > 1)
                uri = new Uri(Environment.GetCommandLineArgs()[1].Trim());
        }
        catch { }

        string filename = Path.Combine(Path.GetTempPath(), "_.webauth");

        if (uri != null)
        {
            var processes = System.Diagnostics.Process.GetProcesses().Where(process => process.ProcessName == Application.productName).ToList();
            if (processes.Count() > 1)
            {
                Debug.Log("Already Running");

                File.WriteAllText(filename, uri.ToString());
                ProcessWindow.Focus(processes[1].ProcessName);

                Application.Quit();
            }
        }

        var watcher = new FileSystemWatcher();
        watcher.Path = Path.GetTempPath();
        watcher.Filter = "_.webauth";
        watcher.NotifyFilter = NotifyFilters.LastWrite | NotifyFilters.CreationTime;
        watcher.Changed += (object sender, FileSystemEventArgs e) =>
        {
            try
            {
                onUriRecieved(new Uri(File.ReadAllText(e.FullPath)));
            }
            catch(Exception ex)
            {
                Debug.LogError(ex.Message);
            }
        };
        watcher.EnableRaisingEvents = true;
    }
#endif

    private void request(string  path, LoginParams loginParams = null, Dictionary<string, object> extraParams = null)
    {

        Dictionary<string, object> paramMap = new Dictionary<string, object>();
        paramMap["init"] = this.initParams;
        paramMap["params"] = loginParams == null ? (object)new Dictionary<string, object>() : (object)loginParams;

        if (extraParams != null && extraParams.Count > 0)
            foreach(KeyValuePair<string, object> item in extraParams)
            {
                (paramMap["params"] as Dictionary<string, object>) [item.Key] = item.Value;
            }

        string hash = Convert.ToBase64String(Encoding.UTF8.GetBytes(JsonConvert.SerializeObject(paramMap, Newtonsoft.Json.Formatting.None,
                            new JsonSerializerSettings
                            {
                                NullValueHandling = NullValueHandling.Ignore
                            })));

        UriBuilder uriBuilder = new UriBuilder(this.web3AuthOptions.sdkUrl);
        uriBuilder.Path = path;
        uriBuilder.Fragment = hash;

        Utils.LaunchUrl(uriBuilder.ToString());
    }

    public void setResultUrl(Uri uri)
    {
        string hash = uri.Fragment;
        if (hash == null)
            throw new UserCancelledException();

        hash = hash.Remove(0, 1);

        Dictionary<string, string> queryParameters = Utils.ParseQuery(uri.Query);

        if (queryParameters.Keys.Contains("error"))
            throw new UnKnownException(queryParameters["error"]);

        this.web3AuthResponse = JsonConvert.DeserializeObject<Web3AuthResponse>(Encoding.UTF8.GetString(Utils.DecodeBase64(hash)));
        if (!string.IsNullOrEmpty(this.web3AuthResponse.error))
            throw new UnKnownException(web3AuthResponse.error);

        if (string.IsNullOrEmpty(this.web3AuthResponse.privKey) || string.IsNullOrEmpty(this.web3AuthResponse.privKey.Trim('0')))
            this.onLogout?.Invoke();
        else
            this.onLogin?.Invoke(this.web3AuthResponse);

#if UNITY_IOS
        Utils.Dismiss();
#endif
    }

    public void login(LoginParams loginParams)
    {
        request("login", loginParams);
    }

    public void logout(Dictionary<string, object> extraParams)
    {
        request("logout", extraParams: extraParams);
    }

    public void logout(Uri redirectUrl = null, string appState = null)
    {
        Dictionary<string, object> extraParams = new Dictionary<string, object>();
        if (redirectUrl != null)
            extraParams["redirectUrl"] = redirectUrl.ToString();

        if (appState != null)
            extraParams["appState"] = appState;

        logout(extraParams);
    }


}
