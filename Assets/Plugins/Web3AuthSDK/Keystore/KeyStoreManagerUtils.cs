using Org.BouncyCastle.Asn1;
using Org.BouncyCastle.Asn1.Sec;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Signers;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Utilities.Encoders;

public class KeyStoreManagerUtils
{

    public static string SESSION_ID = "sessionId";
    public static string IV_KEY = "ivKey";
    public static string EPHEM_PUBLIC_Key = "ephemPublicKey";
    public static string MAC = "mac";

    public static string getPubKey(string sessionId)
    {
        var domain = SecNamedCurves.GetByName("secp256k1");
        var parameters = new ECDomainParameters(domain.Curve, domain.G, domain.H);

        var key = new ECPrivateKeyParameters(new BigInteger(sessionId, 16), parameters);
        var q = new ECPublicKeyParameters("EC", domain.G.Multiply(key.D), parameters).Q;

        return Hex.ToHexString(domain.Curve.CreatePoint(q.XCoord.ToBigInteger(), q.YCoord.ToBigInteger()).GetEncoded(false));
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
        var curve = SecNamedCurves.GetByName("secp256k1");
        var domain = new ECDomainParameters(curve.Curve, curve.G, curve.N, curve.H);
        var keyParameters = new ECPrivateKeyParameters(new BigInteger(privateKey, 16), domain);

        var signer = new ECDsaSigner(new HMacDsaKCalculator(new Sha256Digest()));
        signer.Init(true, keyParameters);

        var hashAlgorithm = new KeccakDigest(256);
        byte[] input = System.Text.Encoding.UTF8.GetBytes(data);
        hashAlgorithm.BlockUpdate(input, 0, input.Length);

        byte[] messageHash = new byte[32];
        hashAlgorithm.DoFinal(messageHash, 0);

        var signature = signer.GenerateSignature(messageHash);

        var r = signature[0];
        var s = signature[1];

        var other = curve.Curve.Order.Subtract(s);
        if (s.CompareTo(other) == 1)
            s = other;

        var v = new Asn1EncodableVector();
        v.Add(new DerInteger(r));
        v.Add(new DerInteger(s));

        var derSignature = new DerSequence(v).GetDerEncoded();

        return Hex.ToHexString(derSignature);
    }
}
