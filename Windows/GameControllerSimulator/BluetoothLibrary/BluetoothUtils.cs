using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Radios;

namespace BluetoothLibrary
{
    public class BluetoothUtils
    {
        public static async Task<bool> IsBluetoothAvailableAsync()
        {
            var btAdapter = await BluetoothAdapter.GetDefaultAsync();
            if (btAdapter == null)
            {
                return false;
            }
            var radios = await Radio.GetRadiosAsync();
            var bluetoothRadio = radios.FirstOrDefault(radio => radio.Kind == RadioKind.Bluetooth);
            if (bluetoothRadio == null)
            {
                return false;
            }
            return bluetoothRadio.State == RadioState.On;
        }
    }
}
