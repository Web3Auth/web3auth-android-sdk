using System;
using UnityEngine;

public static class Utils
{
    public static void LaunchUrl(string url)
    {
#if UNITY_EDITOR
        Application.OpenURL(url);
#elif UNITY_ANDROID
        using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
        using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
        using (var intentBuilder = new AndroidJavaObject("androidx.browser.customtabs.CustomTabsIntent$Builder"))
        using (var intent = intentBuilder.Call<AndroidJavaObject>("build"))
        using (var uriClass = new AndroidJavaClass("android.net.Uri"))
        using (var uri = uriClass.CallStatic<AndroidJavaObject>("parse", url))
        {
            intent.Call("launchUrl", activity, uri);
        }
#else
        Application.OpenURL(url);
#endif
    }

    public static byte[] DecodeBase64(string text)
    {
        var output = text;
        output = output.Replace('-', '+');
        output = output.Replace('_', '/');
        switch (output.Length % 4)
        {
            case 0: break;
            case 2: output += "=="; break;
            case 3: output += "="; break;
            default: throw new FormatException(text);
        }
        var converted = Convert.FromBase64String(output);
        return converted;
    }
}