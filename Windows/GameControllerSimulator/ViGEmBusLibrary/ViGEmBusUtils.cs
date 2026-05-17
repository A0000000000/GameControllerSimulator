using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using System.Reflection;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace ViGEmBusLibrary
{
    public static class ViGEmBusUtils
    {
        public static bool IsDriverInstalled()
        {
            using var key = Registry.LocalMachine.OpenSubKey(@"SYSTEM\CurrentControlSet\Services\ViGEmBus");
            return key != null;
        }

        public static void InstallDriver()
        {
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
