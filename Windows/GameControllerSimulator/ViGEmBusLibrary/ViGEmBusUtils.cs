using Microsoft.Win32;
using System.Diagnostics;
using System.Reflection;
using System.IO;
using LogLibrary;

namespace ViGEmBusLibrary
{
    public static class ViGEmBusUtils
    {
        public static string TAG = "ViGEmBusUtils";
        public static bool IsDriverInstalled()
        {
            LogUtils.I(TAG, "IsDriverInstalled");
            using var key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\ViGEmBus");
            return key != null;
        }

        public static void InstallDriver()
        {
            LogUtils.I(TAG, "InstallDriver");
            string exePath = Path.Combine(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location)!, "ViGEmBus_1.22.0_x64_x86_arm64.exe");
            Process.Start(new ProcessStartInfo
            {
                FileName = exePath,
                UseShellExecute = true,
                Verb = "runas"
            }).WaitForExit();
        }

    }
}
