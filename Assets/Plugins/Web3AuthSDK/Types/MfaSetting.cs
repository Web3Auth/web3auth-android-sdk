using System.Collections.Generic;

public class MfaSetting
{
    public bool? enable { get; set; }
    public int priority { get; set; }
    public bool? mandatory { get; set; }

    // Constructor
    public MfaSetting(bool? enable, int priority, bool? mandatory)
    {
        enable = enable;
        priority = priority;
        mandatory = mandatory;
    }
}