using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public static class Utils
{
    public static void LaunchUrl(string url)
    {
#if UNITY_EDITOR || UNITY_STANDALONE_WIN
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

    public static Dictionary<string, string> ParseQuery(string text)
    {
        if (text.Length > 0 && text[0] == '?')
            text = text.Remove(0, 1);

        var parts = text.Split('&').Where(x => !string.IsNullOrEmpty(x)).ToList();

        Dictionary<string, string> result = new Dictionary<string, string>();

        if (parts.Count > 0)
        {
            result = parts.ToDictionary(
                c => c.Split('=')[0],
                c => Uri.UnescapeDataString(c.Split('=')[1])
            );
        }

        return result;
    }


}