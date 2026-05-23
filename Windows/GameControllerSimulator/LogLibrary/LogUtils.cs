using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace LogLibrary
{
    public static class LogUtils
    {
        public static void I(string tag, string msg, Exception ex = null)
        {
            Debug.WriteLine($"[info]-[{DateTime.UtcNow.ToString()}]-[{tag}]-[{msg}]-[{ex?.Message}]-[{ex?.StackTrace}]");
        }

        public static void D(string tag, string msg, Exception ex = null)
        {
            Debug.WriteLine($"[debug]-[{DateTime.UtcNow.ToString()}]-[{tag}]-[{msg}]-[{ex?.Message}]-[{ex?.StackTrace}]");
        }

        public static void W(string tag, string msg, Exception ex = null)
        {
            Debug.WriteLine($"[warning]-[{DateTime.UtcNow.ToString()}]-[{tag}]-[{msg}]-[{ex?.Message}]-[{ex?.StackTrace}]");
        }

        public static void E(string tag, string msg, Exception ex = null)
        {
            Debug.WriteLine($"[error]-[{DateTime.UtcNow.ToString()}]-[{tag}]-[{msg}]-[{ex?.Message}]-[{ex?.StackTrace}]");
        }


    }
}
