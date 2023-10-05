public class MfaSettings
{
    private MfaSetting deviceShareFactor;
    private MfaSetting backUpShareFactor;
    private MfaSetting socialBackupFactor;
    private MfaSetting passwordFactor;

    public MfaSetting DeviceShareFactor
    {
        get { return deviceShareFactor; }
        set { deviceShareFactor = value; }
    }

    public MfaSetting BackUpShareFactor
    {
        get { return backUpShareFactor; }
        set { backUpShareFactor = value; }
    }

    public MfaSetting SocialBackupFactor
    {
        get { return socialBackupFactor; }
        set { socialBackupFactor = value; }
    }

    public MfaSetting PasswordFactor
    {
        get { return passwordFactor; }
        set { passwordFactor = value; }
    }

    // Constructors
    public MfaSettings(
        MfaSetting deviceShareFactor = null,
        MfaSetting backUpShareFactor = null,
        MfaSetting socialBackupFactor = null,
        MfaSetting passwordFactor = null)
    {
        deviceShareFactor = deviceShareFactor;
        backUpShareFactor = backUpShareFactor;
        socialBackupFactor = socialBackupFactor;
        passwordFactor = passwordFactor;
    }
}