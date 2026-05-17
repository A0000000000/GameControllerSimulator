using BluetoothLibrary;
using CommonLibrary.Generator;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using GameControllerSimulator.Generator;
using GameControllerSimulator.Generator.GamepadProtocol;
using GameControllerSimulator.UIManager;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Data;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Navigation;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using ViGEmBusLibrary;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.Storage.Streams;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace GameControllerSimulator
{
    /// <summary>
    /// An empty window that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainWindow : Window
    {
        private DispatcherQueue _dispatcher = DispatcherQueue.GetForCurrentThread();
        private bool _isInitialized = false;
        private DeviceUIManager[] deviceUIManagers = new DeviceUIManager[4];
        private GamepadStatePacket[] gamepadStatePackets = new GamepadStatePacket[4]
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
            }
        }
        private async Task Init()
        {
            await CheckBaseComponent();
            await InitControllerUI();
            await InitGameControllerManager();
            await InitConnectionManager();
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

        private async Task Destroy()
        {
            if (await BluetoothUtils.IsBluetoothAvailableAsync())
            {
                await DestroyConnectionManager();
            }
            if (ViGEmBusUtils.IsDriverInstalled())
            {
                await DestroyGameControllerManager(); 
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

        #region 连接管理处理
        private ConnectionManager? connectionManager;
        private async Task InitConnectionManager()
        {
            connectionManager = new ConnectionManager(new ConnectionManagerCallback(this));
            connectionManager?.Init();
        }

        private void OnConnectionAvaiableChange(bool avaiable)
        {
            _dispatcher.TryEnqueue(() =>
            {
                if (avaiable)
                {
                    Bluetooth.Text = "Bluetooth Status: Running";
                }
                else
                {
                    Bluetooth.Text = "Bluetooth Status: Stopped";
                }
            });
        }

        private async Task DestroyConnectionManager()
        {
            connectionManager?.Destroy();
        }

        class ConnectionManagerCallback : IConnectionManagerCallback
        {
            private MainWindow window;
            public ConnectionManagerCallback(MainWindow window)
            {
                this.window = window;
            }

            public Type GetTypeClass(int type)
            {
                return EntityType.TYPE_MAPPING.ContainsKey(type) ? EntityType.TYPE_MAPPING[type] : typeof(JsonElement);
            }

            public void OnClientDisconnected(int index)
            {
                window.OnClientDisconnected(index);
            }

            public void OnDataReady(string clientId, IBaseEntity data)
            {
                window.OnDataReady(clientId, data);
            }

            public void OnFaulted(string msg, Exception ex)
            {
                window.OnFaulted(msg, ex);
            }

            public void OnManagerAvaiable()
            {
                window.OnConnectionAvaiableChange(true);
            }

            public void OnManagerUnavailable()
            {
                window.OnConnectionAvaiableChange(false);
            }

            public void OnNewClientConnection(string clientId, int index)
            {
                window.OnNewClientConnection(clientId, index);
            }
        }

        #endregion

        #region 手柄管理器
        private ViGEmBusManager? viGEmBusManager;
        private ViGEmBusGameController?[] gameControllers = new ViGEmBusGameController[4];

        private async Task InitGameControllerManager()
        {
            DriverStatus.Text = "Driver Status: Installed";
            viGEmBusManager = new ViGEmBusManager();
        }

        private void CreateGameController(int index)
        {
            if (viGEmBusManager == null || index < 0 || index >= gameControllers.Length)
            {
                return;
            }
            gameControllers[index] = viGEmBusManager.CreateXboxController(index);
        }

        private async Task DestroyGameControllerManager()
        {
            viGEmBusManager?.Dispose();
            viGEmBusManager = null;
            for (int i = 0; i < gameControllers.Length; i++)
            {
                gameControllers[i]?.Dispose();
                gameControllers[i] = null;
            }
        }
        #endregion

        #region 数据处理

        private void OnDataReady(string clientId, IBaseEntity data)
        {
            int index = connectionManager?.GetClientIndex(clientId) ?? -1;
            if (index < 0 || index >= deviceUIManagers.Length)
            {
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

        private void OnNewClientConnection(string clientId, int index)
        {
            if (index < 0 || index > deviceUIManagers.Length)
            {
                return;
            }
            deviceUIManagers[index].SetStatus("Connected");
            CreateGameController(index);
            connectionManager?.SendData(clientId, new BaseEntity<object>
            {
                Type = EntityType.TYPE_QUERY_CLIENT_INFO,
                Id = EntityId.GAMEPAD_PAGE_EVENT,
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                Data = null
            });
        }

        private void OnClientDisconnected(int index)
        {
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
