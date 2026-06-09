using BluetoothLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using GameControllerSimulator.Controller;
using GameControllerSimulator.UIManager;
using LogLibrary;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using NetworkLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Threading.Tasks;
using ViGEmBusLibrary;
using Windows.Devices.Bluetooth.GenericAttributeProfile;


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
            await InitGATTService();
            await InitMainController();
        }
        private async Task Destroy()
        {
            LogUtils.I(TAG, "destroy window");
            await DestroyMainController();
            await DestroyGATTService();
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

        private void OnFaulted(string msg, Exception? ex)
        {
            LogUtils.W(TAG, $"OnFaulted msg: {msg}", ex);
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
        private Guid rfcommGuid = GUIDConstant.DEFAULT_RFCOMM_GUID;
        private string address = NetworkUtils.GetLocalIPv4();
        private int tcpPort;
        private int udpPort;
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

        #region MainController管理

        private MainController? mainController;

        private async Task InitMainController()
        {
            LogUtils.I(TAG, "InitMainController");
            mainController = new MainController(CONTROLLER_COUNT, new MainControllerCallback(this));
            mainController.Init();
            mainController.InitRFCOMM(rfcommGuid, APP_NAME);
            mainController.InitTCP(tcpPort);
            mainController.InitUDP(udpPort);
        }

        private async Task DestroyMainController()
        {
            LogUtils.I(TAG, "DestroyMainController");
            mainController?.Dispose();
            mainController = null;
        }
        
        private void OnConnectionAvaiableChange(bool available, ConnectionType type)
        {
            LogUtils.I(TAG, $"OnConnectionAvaiableChange avaiable = [{available}] type = [{type}]");
            _dispatcher.TryEnqueue(() =>
            {
                switch (type)
                {
                    case ConnectionType.BLE:
                        if (available)
                        {
                            BluetoothRFCOMM.Text = $"Bluetooth RFCOMM Status: Running at {rfcommGuid.ToString()}";
                        }
                        else
                        {
                            BluetoothRFCOMM.Text = "Bluetooth RFCOMM Status: Stopped";
                        }
                        break;
                    case ConnectionType.TCP:
                        if (available)
                        {
                            TcpInfo.Text = $"TCP Status: Running at {address}:{tcpPort}";
                        }
                        else
                        {
                            TcpInfo.Text = "TCP Status: Stopped";
                        }
                        break;
                    case ConnectionType.UDP:
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

        private void OnDeviceInfoReceived(int index, DeviceInfo? deviceInfo)
        {
            LogUtils.I(TAG, $"OnDeviceInfoReceived index = {index}, deviceInfo = {deviceInfo}");
            deviceUIManagers[index].SetDeviceName(deviceInfo?.Model ?? "N/A");
            deviceUIManagers[index].SetOsName(deviceInfo?.OsVersion ?? "N/A");
        }

        private void OnGameEventReceive(int index, byte[] events)
        {
            LogUtils.I(TAG, $"OnGameEventReceive index = {index}, deviceInfo = {Convert.ToHexString(events)}");
            deviceUIManagers[index].SetCurrentEvent(Convert.ToHexString(events));
        }

        private void OnSessionAvailableChange(int index, bool available)
        {
            LogUtils.I(TAG, $"OnGameEventReceive index = {index}, available = {available}");
            if (available) 
            {
                deviceUIManagers[index].SetStatus("Connected");
            }
            else
            {
                deviceUIManagers[index].Reset();
            }
        }

        class MainControllerCallback: IMainControllerCallback
        {
            private MainWindow window;
            public MainControllerCallback(MainWindow window)
            { 
                this.window = window; 
            }

            public void OnConnectionAvaiableChange(bool available, ConnectionType type)
            {
                window.OnConnectionAvaiableChange(available, type);
            }

            public void OnDeviceInfoReceived(int index, DeviceInfo? deviceInfo)
            {
                window.OnDeviceInfoReceived(index, deviceInfo);
            }

            public void OnFaulted(string msg, Exception? ex)
            {
                window.OnFaulted(msg, ex);
            }

            public void OnGameEventReceive(int index, byte[] events)
            {
                window.OnGameEventReceive(index, events);
            }

            public void OnSessionAvailableChange(int index, bool available)
            {
                window.OnSessionAvailableChange(index, available);
            }
        }
        #endregion

    }
}
