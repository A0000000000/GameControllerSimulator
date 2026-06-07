using LogLibrary;
using System;
using System.Linq;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Radios;

namespace BluetoothLibrary
{
    public class BluetoothUtils
    {
        public static string TAG = "BluetoothUtils";
        public static async Task<bool> IsBluetoothAvailableAsync()
        {
            var btAdapter = await BluetoothAdapter.GetDefaultAsync();
            if (btAdapter == null)
            {
                LogUtils.W(TAG, "IsBluetoothAvailableAsync BluetoothAdapter is null.");
                return false;
            }
            var radios = await Radio.GetRadiosAsync();
            var bluetoothRadio = radios.FirstOrDefault(radio => radio.Kind == RadioKind.Bluetooth);
            if (bluetoothRadio == null)
            {
                LogUtils.W(TAG, "IsBluetoothAvailableAsync Radio is null.");
                return false;
            }
            return bluetoothRadio.State == RadioState.On;
        }

        public static async Task<bool> SupportBLEPerpheralAsync()
        {
            BluetoothAdapter adapter = await BluetoothAdapter.GetDefaultAsync();
            if (adapter == null)
            {
                LogUtils.W(TAG, "IsBluetoothAvailableAsync BluetoothAdapter is null.");
                return false;
            }
            return adapter.IsPeripheralRoleSupported;
        }

    }
}
