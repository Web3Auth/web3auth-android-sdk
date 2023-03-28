using System;
using System.Collections.Generic;

public class Web3AuthOptions {
    //public context: Context,
    public string clientId { get; set; }
    public Web3Auth.Network network { get; set; }
    public Uri redirectUrl { get; set; }
    public string sdkUrl { get; set; } = getSdkUrl(network);
    public WhiteLabelData whiteLabel { get; set; }
    public Dictionary<string, LoginConfigItem> loginConfig { get; set; }
    public bool? useCoreKitKey { get; set; } = false;
    public Web3Auth.ChainNamespace? chainNamespace { get; set; } = Web3Auth.ChainNamespace.EIP155;

    public string getSdkUrl(Web3Auth.Network network)
    {
        string sdkUrl = "";
        if (network == Web3Auth.Network.TESTNET)
        {
            sdkUrl = "https://dev-sdk.openlogin.com";
        }
        else
        {
            sdkUrl = "https://sdk.openlogin.com";
        }
        return sdkUrl;
    }
}