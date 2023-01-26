using Nethereum.Hex.HexConvertors.Extensions;
using Nethereum.Signer;
using Nethereum.Util;
using Org.BouncyCastle.Asn1;

public class KeyStoreManagerUtils
{

    public static string SESSION_ID = "sessionId";
    public static string IV_KEY = "ivKey";
    public static string EPHEM_PUBLIC_Key = "ephemPublicKey";
    public static string MAC = "mac";

    public static string getPubKey(string sessionId)
    {
        var privKey = new EthECKey(sessionId);
        return privKey.GetPubKey().ToHex();
    }

    static KeyStoreManagerUtils()
    {
        SecurePlayerPrefs.Init();
    }

    public static void savePreferenceData(string key, string value)
    {
        SecurePlayerPrefs.SetString(key, value);
    }

    public static string getPreferencesData(string key)
    {
        return SecurePlayerPrefs.GetString(key);
    }
    public static void deletePreferencesData(string key)
    {
        SecurePlayerPrefs.DeleteKey(key);
    }

    public static string getECDSASignature(string privateKey, string data){
        var derivedECKeyPair = new EthECKey(privateKey);
        byte[] hashedData = new Sha3Keccack().CalculateHash(System.Text.Encoding.UTF8.GetBytes(data));

        var signature = derivedECKeyPair.Sign(hashedData);
        var v = new Asn1EncodableVector();
        v.Add(new DerInteger(signature.R));
        v.Add(new DerInteger(signature.S));

        var der = new DerSequence(v);
        var sigBytes = der.GetEncoded();
        return sigBytes.ToHexCompact();
    }
}
