using System;
using System.Diagnostics;

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
