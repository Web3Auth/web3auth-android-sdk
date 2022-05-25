using System;
using System.Linq;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using Newtonsoft.Json;

public class Web3AuthSample : MonoBehaviour
{
    List<LoginVerifier> verifierList = new List<LoginVerifier> {
        new LoginVerifier("Google", Provider.GOOGLE),
        new LoginVerifier("Facebook", Provider.FACEBOOK),
        new LoginVerifier("Twitch", Provider.TWITCH),
        new LoginVerifier("Discord", Provider.DISCORD),
        new LoginVerifier("Reddit", Provider.REDDIT),
        new LoginVerifier("Apple", Provider.APPLE),
        new LoginVerifier("Github", Provider.GITHUB),
        new LoginVerifier("LinkedIn", Provider.LINKEDIN),
        new LoginVerifier("Twitter", Provider.TWITTER),
        new LoginVerifier("Line", Provider.LINE),
        new LoginVerifier("Hosted Email Passwordless", Provider.EMAIL_PASSWORDLESS),
    };

    Web3Auth web3Auth;

    [SerializeField]
    InputField emailAddressField;

    [SerializeField]
    Dropdown verifierDropdown;

    [SerializeField]
    Button loginButton;

    [SerializeField]
    Text loginResponseText;

    [SerializeField]
    Button logoutButton;


    void Start()
    {
        web3Auth = new Web3Auth(new Web3AuthOptions()
        {
            redirectUrl = new Uri("torusapp://com.torus.Web3AuthUnity/auth"),
            clientId = "BGZg0VJ2kE434OoxaJS_HJ3rWTiA7P8tyjFWDv65bRvR7AwKrMe7yAuPpnSEuT0eCYnZiF1a3aWO26lT52HPNg8",
            network = Web3Auth.Network.MAINNET,
            whiteLabel = new WhiteLabelData()
            {
                name = "Web3Auth Sample App",
                logoLight = null,
                logoDark = null,
                defaultLanguage = "en",
                dark = true,
                theme = new Dictionary<string, string>
                {
                    { "primary", "#123456" }
                }
            }
        }); 
        web3Auth.onLogin += onLogin;
        web3Auth.onLogout += onLogout;

        emailAddressField.gameObject.SetActive(false);
        logoutButton.gameObject.SetActive(false);

        loginButton.onClick.AddListener(login);
        logoutButton.onClick.AddListener(logout);

        verifierDropdown.AddOptions(verifierList.Select(x => x.name).ToList());
        verifierDropdown.onValueChanged.AddListener(onVerifierDropDownChange);

        
        //onDeepLinkActivated("https://wondertree.co/#eyJwcml2S2V5IjoiMWMwZTU1YzE4NDYwYjI2YjY2MmY5NDBiYzA5YzRhZmM1NDM5YWY4YzFjYzk2YTg4ZGQ0Mzc5MTU4YzFjNzIzOCIsImVkMjU1MTlQcml2S2V5IjoiMWMwZTU1YzE4NDYwYjI2YjY2MmY5NDBiYzA5YzRhZmM1NDM5YWY4YzFjYzk2YTg4ZGQ0Mzc5MTU4YzFjNzIzODk1ZTYzMGQ4MTgxYjQxZTRlMDhjMTIxODkzMWUxNzRhZDYyZjBjOTFjMzUxZWNmNGFkZTkzZTEwOWFmMjY2MzUiLCJ1c2VySW5mbyI6eyJlbWFpbCI6InVzbWFua2FpekBnbWFpbC5jb20iLCJuYW1lIjoiTXVoYW1tYWQgVXNtYW4iLCJwcm9maWxlSW1hZ2UiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQU9oMTRHalI0OFUyZ25nUXhkaUVtZjBxS2M3WFo2VmVMZjA0X3Ntam1VU252Zz1zOTYtYyIsImFnZ3JlZ2F0ZVZlcmlmaWVyIjoidGtleS1nb29nbGUiLCJ2ZXJpZmllciI6InRvcnVzIiwidmVyaWZpZXJJZCI6InVzbWFua2FpekBnbWFpbC5jb20iLCJ0eXBlT2ZMb2dpbiI6Imdvb2dsZSIsImRhcHBTaGFyZSI6IiJ9fQ");
    }

    private void onLogin(Web3AuthResponse response)
    {
        loginResponseText.text = JsonConvert.SerializeObject(response, Formatting.Indented);

        loginButton.gameObject.SetActive(false);
        verifierDropdown.gameObject.SetActive(false);
        logoutButton.gameObject.SetActive(true);
    }

    private void onLogout()
    {
        loginButton.gameObject.SetActive(true);
        verifierDropdown.gameObject.SetActive(true);
        logoutButton.gameObject.SetActive(false);

        loginResponseText.text = "";
    }


    private void onVerifierDropDownChange(int selectedIndex)
    {
        if (verifierList[selectedIndex].loginProvider == Provider.EMAIL_PASSWORDLESS)
            emailAddressField.gameObject.SetActive(true);
        else
            emailAddressField.gameObject.SetActive(false);
    }

    private void login()
    {
        var selectedProvider = verifierList[verifierDropdown.value].loginProvider;

        var options = new LoginParams()
        {
            loginProvider = selectedProvider
        };

        if (selectedProvider == Provider.EMAIL_PASSWORDLESS)
        {
            options.extraLoginOptions = new ExtraLoginOptions()
            {
                login_hint = emailAddressField.text
            };
        }

        web3Auth.login(options);
    }

    private void logout()
    {
        web3Auth.logout();
    }
}
