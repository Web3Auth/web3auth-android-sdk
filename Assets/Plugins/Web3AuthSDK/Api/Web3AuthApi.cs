using System.Collections;
using System;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using UnityEngine.Networking;

public class Web3AuthApi
{
    static Web3AuthApi instance;
    static string baseAddress = "https://broadcast-server.tor.us";

    public static Web3AuthApi getInstance()
    {
        if (instance == null)
            instance = new Web3AuthApi();
        return instance;
    }

    public IEnumerator authorizeSession(string key, Action<StoreApiResponse> callback)
    {
        var request = UnityWebRequest.Get($"{baseAddress}/store/get?key={key}");
        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
        {
            string result = request.downloadHandler.text;
            callback(Newtonsoft.Json.JsonConvert.DeserializeObject<StoreApiResponse>(result));
        }
        else
            callback(null);
    }

    public IEnumerator logout(LogoutApiRequest logoutApiRequest, Action<JObject> callback)
    {
        var request = new UnityWebRequest($"{baseAddress}/store/set");

        byte[] data = new System.Text.UTF8Encoding().GetBytes(JsonConvert.SerializeObject(logoutApiRequest));
        request.uploadHandler = new UploadHandlerRaw(data);
        request.downloadHandler = new DownloadHandlerBuffer();
        request.SetRequestHeader("Content-Type", "application/json");

        yield return request.SendWebRequest();

        if (request.result == UnityWebRequest.Result.Success)
        {
            string result = request.downloadHandler.text;
            callback(Newtonsoft.Json.JsonConvert.DeserializeObject<JObject>(result));
        }
        else
            callback(null);
    }
}
