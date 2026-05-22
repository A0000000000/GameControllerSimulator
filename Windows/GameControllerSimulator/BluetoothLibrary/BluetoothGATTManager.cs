using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace BluetoothLibrary
{
    public class BluetoothGATTManager : IDisposable
    {

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
        }

        public void StartService()
        {
            Post(async () =>
            {
                lock(_lock)
                {
                    if (isServiceRunning)
                    {
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
                    callback?.OnException(ex);
                }
            });
        }

        private async Task StartServiceAsync()
        {
            GattServiceProviderResult serviceResult = await GattServiceProvider.CreateAsync(funGuid);
            serviceProvider = serviceResult.ServiceProvider;
            foreach (GATTProperties item in properties)
            {
                var prop = item;
                DataWriter writer = new();
                writer.WriteBytes(prop.PropertiesValue);
                var buf = writer.DetachBuffer();
                GattLocalCharacteristicParameters parameters = new()
                {
                    CharacteristicProperties = GattCharacteristicProperties.Read,
                    StaticValue = buf,
                    ReadProtectionLevel = GattProtectionLevel.Plain,
                    UserDescription = prop.PropertiesKey
                };
                GattLocalCharacteristicResult characteristicResult = await serviceProvider.Service.CreateCharacteristicAsync(prop.PropertiesGuid, parameters);
                if (characteristicResult.Error != BluetoothError.Success)
                {
                    callback?.OnException(new Exception($"Create characteristic failed: {characteristicResult.Error}"));
                    return;
                }
                GattLocalCharacteristic characteristic = characteristicResult.Characteristic;
                characteristic.ReadRequested += async (GattLocalCharacteristic sender, GattReadRequestedEventArgs args) =>
                {
                    var deferral = args.GetDeferral();
                    try
                    {
                        var request = await args.GetRequestAsync();
                        if (request == null)
                        {
                            return;
                        }
                        request.RespondWithValue(buf);
                    }
                    catch (Exception ex)
                    {
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
                callback?.OnGATTStatusChanged(e.Status);
            };
            serviceProvider.StartAdvertising(new GattServiceProviderAdvertisingParameters
            {
                IsConnectable = true,
                IsDiscoverable = true
            });
        }


        public void StopService()
        {
            Post(() =>
            {
                lock (_lock)
                {
                    if (!isServiceRunning)
                    {
                        return;
                    }
                    isServiceRunning = false;
                }
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

    }

}
