using System.Collections.Generic;

public class WhiteLabelData { 
    public string? appName { get; set; }
    public string? logoLight { get; set; }
    public string? logoDark { get; set; }
    public string? defaultLanguage { get; set; } = "en";
    public string? mode { get; set; } = "light";
    public Dictionary<string, string>? theme { get; set; }
    public string? appUrl { get; set; }
    public bool? useLogoLoader { get; set; } = false;
}