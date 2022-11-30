using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Net.Http;
using System;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.Text;

public class Web3AuthApi
{
    static Web3AuthApi instance;
    static Uri baseAddress = new Uri("https://broadcast-server.tor.us");

    public static Web3AuthApi getInstance()
    {
        if (instance == null)
            instance = new Web3AuthApi();
        return instance;
    }

    public StoreApiResponse? authorizeSession(string key)
    {
        using (var client = new HttpClient())
        {
            client.BaseAddress = baseAddress;
            HttpResponseMessage response = client.GetAsync("/store/get?key=" + key).Result;

            if (response.IsSuccessStatusCode)
            {
                string result = response.Content.ReadAsStringAsync().Result;
                return Newtonsoft.Json.JsonConvert.DeserializeObject<StoreApiResponse>(result);
            }
            return null;
        }
    }

    public JObject logout(LogoutApiRequest logoutApiRequest)
    {
        using (var client = new HttpClient())
        {
            client.BaseAddress = baseAddress;
            var content = new StringContent(JsonConvert.SerializeObject(logoutApiRequest), Encoding.UTF8, "application/json");
            HttpResponseMessage response = client.PostAsync("/store/set", content).Result;

            if (response.IsSuccessStatusCode)
            {
                string result = response.Content.ReadAsStringAsync().Result;
                return Newtonsoft.Json.JsonConvert.DeserializeObject<JObject>(result);
            }
            return null;
        }
    }
}
