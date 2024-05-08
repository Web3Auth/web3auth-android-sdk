using System.Collections.Generic;

public static class WhiteLabelDataExtensions
{
    public static WhiteLabelData merge(this WhiteLabelData target, WhiteLabelData other)
    {
        if (target == null)
            return other;
        if (other == null)
            return target;

        return new WhiteLabelData
        {
            appName = other.appName ?? target.appName,
            appUrl = other.appUrl ?? target.appUrl,
            logoLight = other.logoLight ?? target.logoLight,
            logoDark = other.logoDark ?? target.logoDark,
            defaultLanguage = other.defaultLanguage ?? target.defaultLanguage,
            mode = other.mode ?? target.mode,
            useLogoLoader = other.useLogoLoader ?? target.useLogoLoader,
            theme = mergeThemes(target.theme, other.theme)
        };
    }

    private static Dictionary<string, string> mergeThemes(Dictionary<string, string> targetTheme, Dictionary<string, string> otherTheme)
    {
        if (otherTheme == null || otherTheme.Count == 0)
            return targetTheme;
        if (targetTheme == null || targetTheme.Count == 0)
            return otherTheme;

        var mergedTheme = new Dictionary<string, string>(targetTheme);
        foreach (var kvp in otherTheme)
        {
            if (mergedTheme.ContainsKey(kvp.Key))
            {
                mergedTheme[kvp.Key] = kvp.Value; // Overwrite with otherTheme's value
            }
            else
            {
                mergedTheme.Add(kvp.Key, kvp.Value);
            }
        }
        return mergedTheme;
    }
}