using BluetoothLibrary;
using CommonLibrary.Generator;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using GameControllerSimulator.Generator;
using GameControllerSimulator.Generator.GamepadProtocol;
using GameControllerSimulator.UIManager;
using LogLibrary;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using NetworkLibrary;
using System;
using System.Buffers;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using ViGEmBusLibrary;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Storage.Streams;
using static System.Runtime.InteropServices.JavaScript.JSType;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace GameControllerSimulator
{
    /// <summary>
    /// An empty window that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainWindow : Window
    {
        public static string TAG = "MainWindow";
        public static readonly int MIN_PORT = 40000;
        public static readonly int MAX_PORT = 49152;
        public const int CONTROLLER_COUNT = 4;
        private const string APP_NAME = "GameControllerSimulator2";

        private DispatcherQueue _dispatcher = DispatcherQueue.GetForCurrentThread();
        private bool _isInitialized = false;
        private DeviceUIManager[] deviceUIManagers = new DeviceUIManager[CONTROLLER_COUNT];
        private GamepadStatePacket[] gamepadStatePackets = new GamepadStatePacket[CONTROLLER_COUNT]
        {
            new GamepadStatePacket(),
            new GamepadStatePacket(),
            new GamepadStatePacket(),
            new GamepadStatePacket()
        };

        public MainWindow()
        {
            InitializeComponent();
            this.Activated += OnActivated;
            this.Closed += OnClosed;
        }

        

        private async void OnActivated(object sender, WindowActivatedEventArgs args)
        {
            if (_isInitialized) return;
            _isInitialized = true;
            this.Activated -= OnActivated;
            try
            {
                await Init();
            }
            catch (Exception ex)
            {
                LogUtils.E(TAG, "OnActivated Failed", ex);
            }
        }

        private async void OnClosed(object sender, WindowEventArgs args)
        {
            try
            {
                await Destroy();
            }
            catch (Exception ex)
            {
                LogUtils.E(TAG, "OnClosed Failed", ex);
            }
        }
        private async Task Init()
        {
            LogUtils.I(TAG, "init window");
            await CheckBaseComponent();
            await InitControllerUI();
            await InitGameControllerManager();
            await InitGATTService();
            await InitConnectionManager();
        }
        private async Task Destroy()
        {
            LogUtils.I(TAG, "destroy window");
            await DestroyConnectionManager();
            await DestroyGATTService();
            await DestroyGameControllerManager();
        }

        private async Task CheckBaseComponent()
        {
            if (!ViGEmBusUtils.IsDriverInstalled())
            {
                DriverStatus.Text = "Driver Status: Uninstalled";
                ViGEmBusUtils.InstallDriver();
            }
            if (!ViGEmBusUtils.IsDriverInstalled())
            {
                LogUtils.E(TAG, "driver not installed.");
                var messageDialog = new ContentDialog
                {
                    Title = "提示",
                    Content = "未安装驱动，应用不可用。",
                    CloseButtonText = "确定"
                };
                messageDialog.XamlRoot = this.Content.XamlRoot;
                await messageDialog.ShowAsync();
                this.Close();
                return;
            }
            if (!await BluetoothUtils.IsBluetoothAvailableAsync())
            {
                LogUtils.E(TAG, "bluetooth not available.");
                var messageDialog = new ContentDialog
                {
                    Title = "提示",
                    Content = "蓝牙不可用，应用不可用。",
                    CloseButtonText = "确定"
                };
                messageDialog.XamlRoot = this.Content.XamlRoot;
                await messageDialog.ShowAsync();
                this.Close();
                return;
            }
        }


        #region 控制器UI管理

        private async Task InitControllerUI()
        {
            deviceUIManagers[0] = new DeviceUIManager(_dispatcher, ControllerStatus1, ControllerDeviceName1, ControllerOsName1, ControllerCurrentEvent1);
            deviceUIManagers[1] = new DeviceUIManager(_dispatcher, ControllerStatus2, ControllerDeviceName2, ControllerOsName2, ControllerCurrentEvent2);
            deviceUIManagers[2] = new DeviceUIManager(_dispatcher, ControllerStatus3, ControllerDeviceName3, ControllerOsName3, ControllerCurrentEvent3);
            deviceUIManagers[3] = new DeviceUIManager(_dispatcher, ControllerStatus4, ControllerDeviceName4, ControllerOsName4, ControllerCurrentEvent4);
        }

        #endregion


        #region GATT管理

        private List<GATTProperties> GATTProperties = new List<GATTProperties>();
        private BluetoothGATTManager? bluetoothGATTManager;
        private async Task InitGATTService()
        {
            LogUtils.I(TAG, "prepare init gatt.");
            if (await BluetoothUtils.SupportBLEPerpheralAsync())
            {
                LogUtils.I(TAG, "init gatt.");
                rfcommGuid = Guid.NewGuid();
                LogUtils.I(TAG, $"generator RFCOMM GUID. guid = [{rfcommGuid.ToString()}]");
                GATTProperties.Add(new()
                {
                    PropertiesGuid = GUIDConstant.GATT_DATA_RFCOMM_GUID,
                    PropertiesKey = "RFCOMMGuid",
                    PropertiesValue = Encoding.UTF8.GetBytes(rfcommGuid.ToString())
                });
                if (address != null && address != string.Empty && address != "")
                {
                    Random random = new Random();
                    tcpPort = random.Next(MIN_PORT, MAX_PORT);
                    udpPort = random.Next(MIN_PORT, MAX_PORT);
                    GATTProperties.Add(new()
                    {
                        PropertiesGuid = GUIDConstant.TCP_INFO_GUID,
                        PropertiesKey = "TCPServer",
                        PropertiesValue = Encoding.UTF8.GetBytes($"{address}:{tcpPort}")
                    });
                    GATTProperties.Add(new()
                    {
                        PropertiesGuid = GUIDConstant.UDP_INFO_GUID,
                        PropertiesKey = "UDPServer",
                        PropertiesValue = Encoding.UTF8.GetBytes($"{address}:{udpPort}")
                    });
                }
                bluetoothGATTManager = new BluetoothGATTManager(GATTProperties, GUIDConstant.GATT_FUN_GUID, new GATTCallback(this));
                LogUtils.I(TAG, "start gatt service.");
                bluetoothGATTManager?.StartService();

            }
            else
            {
                LogUtils.W(TAG, "not support gatt.");
            }
        }

        private async Task DestroyGATTService()
        {
            LogUtils.I(TAG, "prepare destroy gatt.");
            if (await BluetoothUtils.SupportBLEPerpheralAsync())
            {
                LogUtils.I(TAG, "destroy gatt.");
                bluetoothGATTManager?.StopService();
                bluetoothGATTManager = null;
                GATTProperties.Clear();
            }
            else
            {
                LogUtils.W(TAG, "not support gatt.");
            }
        }

        private void OnGATTStatusChanged(GattServiceProviderAdvertisementStatus status)
        {
            LogUtils.I(TAG, $"OnGATTStatusChanged status = [{status}].");
            _dispatcher.TryEnqueue(() =>
            {
                switch(status)
                {
                    case GattServiceProviderAdvertisementStatus.Started:
                        BluetoothGATT.Text = $"Bluetooth GATT Status: Running";
                        break;
                    case GattServiceProviderAdvertisementStatus.Aborted:
                    case GattServiceProviderAdvertisementStatus.Stopped:
                        BluetoothGATT.Text = $"Bluetooth GATT Status: Stopped";
                        break;
                    default:
                        BluetoothGATT.Text = $"Bluetooth GATT Status: {status}";
                        break;
                }
            });
        }

        class GATTCallback : IBluetoothGATTManagerCallback
        {
            public static string TAG = "GATTCallback";
            private MainWindow window;
            public GATTCallback(MainWindow window)
            {
                LogUtils.I(TAG, "Create GATTCallback");
                this.window = window;
            }

            public void OnException(Exception ex)
            {
                LogUtils.E(TAG, "GATTCallback OnException", ex);
                window.OnFaulted("GATT Service Exception", ex);
            }

            public void OnGATTStatusChanged(GattServiceProviderAdvertisementStatus status)
            {
                LogUtils.I(TAG, $"GATTCallback OnGATTStatusChanged status = [{status}]");
                window.OnGATTStatusChanged(status);
            }

            public void OnStartService()
            {
                LogUtils.I(TAG, "GATTCallback OnStartService");
            }

            public void OnStopService()
            {
                LogUtils.I(TAG, "GATTCallback OnStopService");
            }
        }

        #endregion

        #region 连接管理处理
        private ConnectionManager? connectionManager;
        private Guid rfcommGuid = GUIDConstant.DEFAULT_RFCOMM_GUID;
        private string address = NetworkUtils.GetLocalIPv4();
        private int tcpPort;
        private int udpPort;

        private async Task InitConnectionManager()
        {
            LogUtils.I(TAG, "InitConnectionManager");
            connectionManager = new ConnectionManager(CONTROLLER_COUNT, new ConnectionManagerCallback(this));
            connectionManager?.InitRFCOMM(rfcommGuid, APP_NAME);
            connectionManager?.InitTcp(tcpPort);
        }

        private void OnConnectionAvaiableChange(bool available, ConnectionManager.ConnectionType type)
        {
            LogUtils.I(TAG, $"OnConnectionAvaiableChange avaiable = [{available}] type = [{type}]");
            _dispatcher.TryEnqueue(() =>
            {
                switch (type) 
                {
                    case ConnectionManager.ConnectionType.BLE:
                        if (available)
                        {
                            BluetoothRFCOMM.Text = $"Bluetooth RFCOMM Status: Running at {rfcommGuid.ToString()}";
                        }
                        else
                        {
                            BluetoothRFCOMM.Text = "Bluetooth RFCOMM Status: Stopped";
                        }
                        break;
                    case ConnectionManager.ConnectionType.TCP:
                        if (available)
                        {
                            TcpInfo.Text = $"TCP Status: Running at {address}:{tcpPort}";
                        }
                        else
                        {
                            TcpInfo.Text = "TCP Status: Stopped";
                        }
                        break;
                    case ConnectionManager.ConnectionType.UDP:
                        if (available)
                        {
                            UdpInfo.Text = $"UDP Status: Running at {address}:{udpPort}";
                        }
                        else
                        {
                            UdpInfo.Text = "UDP Status: Stopped";
                        }
                        break;
                }
            });
        }

        private void OnManagerAvaiableChange(bool available)
        {
            LogUtils.I(TAG, $"OnManagerAvaiableChange avaiable = [{available}]");
        }

        private async Task DestroyConnectionManager()
        {
            LogUtils.I(TAG, "DestroyConnectionManager");
            if (await BluetoothUtils.IsBluetoothAvailableAsync())
            {
                connectionManager?.Destroy();
            }
            else
            {
                LogUtils.I(TAG, "not support bluetooth");
            }
        }

        class ConnectionManagerCallback : IConnectionManagerCallback
        {
            public static string TAG = "ConnectionManagerCallback";

            private MainWindow window;
            public ConnectionManagerCallback(MainWindow window)
            {
                LogUtils.I(TAG, "Create ConnectionManagerCallback");
                this.window = window;
            }

            public Type GetTypeClass(int type)
            {
                return EntityType.TYPE_MAPPING.ContainsKey(type) ? EntityType.TYPE_MAPPING[type] : typeof(JsonElement);
            }

            public void OnClientDisconnected(int index)
            {
                LogUtils.I(TAG, $"ConnectionManagerCallback OnClientDisconnected index = [{index}]");
                window.OnClientDisconnected(index);
            }

            public void OnConnectionAvaiableChange(bool available, ConnectionManager.ConnectionType type)
            {
                LogUtils.I(TAG, "ConnectionManagerCallback OnManagerAvaiable");
                window.OnConnectionAvaiableChange(available, type);
            }

            public void OnDataReady(int index, IBaseEntity data)
            {
                LogUtils.I(TAG, $"ConnectionManagerCallback OnClientDisconnected index = [{index}], data = [{JsonSerializer.Serialize(data)}]");
                window.OnDataReady(index, data);
            }

            public void OnFaulted(string msg, Exception ex)
            {
                LogUtils.W(TAG, $"ConnectionManagerCallback OnFaulted msg = [{msg}]", ex);
                window.OnFaulted(msg, ex);
            }

            public void OnManagerAvaiableChange(bool available)
            {
                LogUtils.I(TAG, "ConnectionManagerCallback OnManagerAvaiable");
                window.OnManagerAvaiableChange(available);
            }

            public void OnNewClientConnection(int index)
            {
                LogUtils.I(TAG, $"ConnectionManagerCallback OnNewClientConnection index = [{index}]");
                window.OnNewClientConnection(index);
            }
        }

        #endregion

        #region 手柄管理器
        private ViGEmBusManager? viGEmBusManager;
        private ViGEmBusGameController?[] gameControllers = new ViGEmBusGameController[CONTROLLER_COUNT];

        private async Task InitGameControllerManager()
        {
            LogUtils.I(TAG, "InitGameControllerManager");
            DriverStatus.Text = "Driver Status: Installed";
            viGEmBusManager = new ViGEmBusManager();
        }

        private void CreateGameController(int index)
        {
            LogUtils.I(TAG, $"CreateGameController index = [{index}]");
            if (viGEmBusManager == null || index < 0 || index >= gameControllers.Length)
            {
                LogUtils.I(TAG, $"CreateGameController failed index = [{index}], viGEmBusManager = [{(viGEmBusManager == null ? "null" : "not null")}]");
                return;
            }
            gameControllers[index] = viGEmBusManager.CreateXboxController(index);
        }

        private async Task DestroyGameControllerManager()
        {
            LogUtils.I(TAG, "DestroyGameControllerManager");
            if (ViGEmBusUtils.IsDriverInstalled())
            {
                viGEmBusManager?.Dispose();
                viGEmBusManager = null;
                for (int i = 0; i < gameControllers.Length; i++)
                {
                    LogUtils.I(TAG, $"DestroyGameController index = [{i}]");
                    gameControllers[i]?.Dispose();
                    gameControllers[i] = null;
                }
            }
            else
            {
                LogUtils.W(TAG, "DestroyGameControllerManager. driver not install");
            }
        }
        #endregion

        #region 数据处理
        private object gameEventLock = new object();

        private void OnDataReady(int index, IBaseEntity data)
        {
            LogUtils.I(TAG, $"OnDataReady index = [{index}], data = [{JsonSerializer.Serialize(data)}]");
            if (index < 0 || index >= deviceUIManagers.Length)
            {
                LogUtils.W(TAG, $"OnDataReady not find index. index = [{index}]");
                return;
            }
            switch (data.Type)
            {
                case EntityType.TYPE_QUERY_CLIENT_INFO_RESULT:
                    DeviceInfo? deviceInfo = data.Data as DeviceInfo;
                    deviceUIManagers[index].SetDeviceName(deviceInfo?.Model ?? "N/A");
                    deviceUIManagers[index].SetOsName(deviceInfo?.OsVersion ?? "N/A");
                    gameControllers[index]?.Connect();
                    break;
                case EntityType.TYPE_SEND_GAME_EVENT:
                    lock (gameEventLock)
                    {
                        string? eventsBase64 = data.Data as string;
                        if (eventsBase64 != null)
                        {
                            byte[] events = Convert.FromBase64String(eventsBase64);
                            deviceUIManagers[index].SetCurrentEvent(Convert.ToHexString(events));
                            gamepadStatePackets[index].CopyEventToCurrent(events);
                            List<GamepadStateChange> changes = gamepadStatePackets[index].GetChanges();
                            gameControllers[index]?.UpdateState(changes);
                            gamepadStatePackets[index].CopyCurrentToLast();
                        }
                    }
                    break;
                default:
                    deviceUIManagers[index].SetCurrentEvent($"Unknown Event Type: {data.Type}");
                    break;
            }

        }

        private void OnFaulted(string msg, Exception ex)
        {
            Debug.WriteLine($"OnFaulted msg: {msg}, ex: {ex.Message}");
        }

        private void OnNewClientConnection(int index)
        {
            LogUtils.I(TAG, $"OnNewClientConnection index = [{index}]");
            if (index < 0 || index > deviceUIManagers.Length)
            {
                return;
            }
            deviceUIManagers[index].SetStatus("Connected");
            CreateGameController(index);
            connectionManager?.SendData(index, new BaseEntity<object>
            {
                Type = EntityType.TYPE_QUERY_CLIENT_INFO,
                Id = EntityId.GAMEPAD_PAGE_EVENT,
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                Data = null
            });
        }

        private void OnClientDisconnected(int index)
        {
            LogUtils.I(TAG, $"OnClientDisconnected index = [{index}]");
            if (index < 0 || index > deviceUIManagers.Length)
            {
                return;
            }
            deviceUIManagers[index].Reset();
            gameControllers[index]?.Dispose();
            gameControllers[index] = null;
        }

        #endregion


    }
}
