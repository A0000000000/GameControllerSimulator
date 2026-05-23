using LogLibrary;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace BluetoothLibrary
{
    public class BluetoothGATTManager : IDisposable
    {
        public static string TAG = "BluetoothGATTManager";
        private Guid funGuid;
        private List<GATTProperties> properties;
        private IBluetoothGATTManagerCallback callback;
        private volatile bool isServiceRunning;
        private readonly object _lock = new object();
        private GattServiceProvider serviceProvider;

        public BluetoothGATTManager(List<GATTProperties> properties, Guid funGuid, IBluetoothGATTManagerCallback callback)
        {
            this.properties = properties; 
            this.funGuid = funGuid;
            this.callback = callback;
            this.isServiceRunning = false;
            LogUtils.I(TAG, $"Create GATT Manager. guid = [{funGuid.ToString()}], properties = [{JsonSerializer.Serialize(properties)}]");
        }

        public void StartService()
        {
            LogUtils.I(TAG, "StartService");
            Post(async () =>
            {
                LogUtils.I(TAG, "StartServicePost");
                lock (_lock)
                {
                    if (isServiceRunning)
                    {
                        LogUtils.I(TAG, "StartServicePost service has already running.");
                        return;
                    }
                    isServiceRunning = true;
                }
                try
                {
                    await StartServiceAsync();
                    callback?.OnStartService();
                }
                catch (Exception ex)
                {
                    lock (_lock)
                    {
                        isServiceRunning = false;
                    }
                    LogUtils.E(TAG, "StartServicePost Failed.", ex);
                    callback?.OnException(ex);
                }
            });
        }

        private async Task StartServiceAsync()
        {
            LogUtils.I(TAG, "StartServiceAsync");
            GattServiceProviderResult serviceResult = await GattServiceProvider.CreateAsync(funGuid);
            serviceProvider = serviceResult.ServiceProvider;
            foreach (GATTProperties item in properties)
            {
                var prop = item;
                LogUtils.I(TAG, $"StartServiceAsync make characteristic prop = [{prop}]");
                DataWriter writer = new();
                writer.WriteBytes(prop.PropertiesValue);
                var buf = writer.DetachBuffer();
                LogUtils.I(TAG, $"StartServiceAsync make characteristic buf = [{Encoding.UTF8.GetString(buf.ToArray())}]");
                GattLocalCharacteristicParameters parameters = new()
                {
                    CharacteristicProperties = GattCharacteristicProperties.Read,
                    ReadProtectionLevel = GattProtectionLevel.Plain,
                    UserDescription = prop.PropertiesKey
                };
                GattLocalCharacteristicResult characteristicResult = await serviceProvider.Service.CreateCharacteristicAsync(prop.PropertiesGuid, parameters);
                if (characteristicResult.Error != BluetoothError.Success)
                {
                    LogUtils.E(TAG, $"StartServiceAsync create characteristic failed. error = [{characteristicResult.Error}]");
                    callback?.OnException(new Exception($"Create characteristic failed: {characteristicResult.Error}"));
                    return;
                }
                GattLocalCharacteristic characteristic = characteristicResult.Characteristic;
                characteristic.ReadRequested += async (GattLocalCharacteristic sender, GattReadRequestedEventArgs args) =>
                {
                    LogUtils.I(TAG, $"StartServiceAsync on ReadRequested.");
                    var deferral = args.GetDeferral();
                    try
                    {
                        var request = await args.GetRequestAsync(); 
                        if (request == null)
                        {
                            return;
                        }
                        LogUtils.I(TAG, $"StartServiceAsync on ReadRequested RespondWithValue buf = [{Encoding.UTF8.GetString(buf.ToArray())}]");
                        request.RespondWithValue(buf);
                    }
                    catch (Exception ex)
                    {
                        LogUtils.E(TAG, $"StartServiceAsync on ReadRequested failed.", ex);
                        callback?.OnException(ex);
                    }
                    finally
                    {
                        deferral.Complete();
                    }
                };
            }
            serviceProvider.AdvertisementStatusChanged += (s, e) => 
            {

                LogUtils.I(TAG, $"StartServiceAsync-OnGATTStatusChanged-[{e.Status.ToString()}]");
                callback?.OnGATTStatusChanged(e.Status);
            };
            LogUtils.I(TAG, "StartServiceAsync-StartAdvertising");
            serviceProvider.StartAdvertising(new GattServiceProviderAdvertisingParameters
            {
                IsConnectable = true,
                IsDiscoverable = true
            });
        }


        public void StopService()
        {
            LogUtils.I(TAG, "StopService");
            Post(() =>
            {
                LogUtils.I(TAG, "StopServicePost");
                lock (_lock)
                {
                    if (!isServiceRunning)
                    {
                        LogUtils.I(TAG, "StopServicePost service has already stop.");
                        return;
                    }
                    isServiceRunning = false;
                }
                LogUtils.I(TAG, "StopServicePost StopAdvertising");
                serviceProvider?.StopAdvertising();
                serviceProvider = null;
                callback?.OnStopService();
            });
        }

        public void Dispose()
        {
            StopService();
        }

        private void Post(Action task)
        {
            Task.Run(() =>
            {
                try
                {
                    task();
                }
                catch (Exception ex)
                {
                    callback?.OnException(ex);
                }
            });
        }

        private void Post(Func<Task> task)
        {
            Task.Run(async () =>
            {
                try
                {
                    await task();
                }
                catch (Exception ex)
                {
                    callback?.OnException(ex);
                }
            });
        }

    }


    public interface IBluetoothGATTManagerCallback
    {
        void OnStartService();
        void OnStopService();
        void OnGATTStatusChanged(GattServiceProviderAdvertisementStatus status);
        void OnException(Exception ex);

    }

    public class GATTProperties
    {
        public Guid PropertiesGuid { get; set; }
        public string PropertiesKey { get; set; }
        public byte[] PropertiesValue { get; set; }

        public override string ToString()
        {
            return JsonSerializer.Serialize(this);
        }

    }

}
