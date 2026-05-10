using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace BluetoothLibrary
{
    public static class IntConverter
    {
        public static byte[] ToBigEndian(int value)
        {
            return new byte[]
            {
            (byte)(value >> 24),
            (byte)(value >> 16),
            (byte)(value >> 8),
            (byte)value
            };
        }

        public static byte[] ToLittleEndian(int value)
        {
            return new byte[]
            {
            (byte)value,
            (byte)(value >> 8),
            (byte)(value >> 16),
            (byte)(value >> 24)
            };
        }

        public static int FromBigEndian(byte[] bytes)
        {
            if (bytes == null || bytes.Length < 4)
                throw new ArgumentException("ByteArray size must be at least 4");

            return
                (bytes[0] << 24) |
                (bytes[1] << 16) |
                (bytes[2] << 8) |
                bytes[3];
        }

        public static int FromLittleEndian(byte[] bytes)
        {
            if (bytes == null || bytes.Length < 4)
                throw new ArgumentException("ByteArray size must be at least 4");

            return
                bytes[0] |
                (bytes[1] << 8) |
                (bytes[2] << 16) |
                (bytes[3] << 24);
        }
    }
}
