using BluetoothLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
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
            await InitConnectionManager();
        }

        private async Task CheckBaseComponent()
        {
            if (!ViGEmBusUtils.IsDriverInstalled())
            {
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
        }

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
                return typeof(JsonElement);
            }

            public void OnDataReady(string clientId, IBaseEntity data)
            {

            }

            public void OnFaulted(string msg, Exception ex)
            {

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
                
            }
        }

        #endregion
    }
}
