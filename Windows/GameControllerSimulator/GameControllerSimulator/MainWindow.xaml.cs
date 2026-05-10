using BluetoothLibrary;
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

        private BluetoothLibrary.BluetoothSocketServer bluetoothSocketServer;
        public MainWindow()
        {
            InitializeComponent();
        }
        private void StatusText_PointerPressed(object sender, PointerRoutedEventArgs e)
        {
            //bluetoothSocketServer = new BluetoothLibrary.BluetoothSocketServer(Guid.Parse("0000180D-0000-1000-8000-00805f9b34fb"), "BLE", new Callback());
            //System.Diagnostics.Debug.WriteLine("start listener");
            //bluetoothSocketServer.StartListener();
            if (ViGEmBusUtils.IsDriverInstalled())
            {
                System.Diagnostics.Debug.WriteLine("installed");
                ViGEmBusUtils.test();
            }
            else
            {
                System.Diagnostics.Debug.WriteLine("not installed");
                ViGEmBusUtils.InstallDriver();
            }


        }

        class ClientCallback : BluetoothLibrary.BluetoothSocketCallback.ClientCallback
        {
            public void OnDataReady(byte[] data)
            {
                string received = Encoding.UTF8.GetString(data, 0, data.Length);
                System.Diagnostics.Debug.WriteLine(received);
            }

            public void OnDataRevException(Exception ex)
            {
                
            }

            public void OnDisconnect()
            {
               
            }

            public void OnSendDataException(Exception ex, int id = -1)
            {
               
            }
        }

        class Callback : BluetoothLibrary.BluetoothSocketCallback
        {
            public BluetoothSocketCallback.ClientCallback CreateNewClientCallback()
            {
                return new ClientCallback();
            }

            public void OnForeverLoopException(Exception ex)
            {
                
            }

            public void OnNewClientConnect(BluetoothSocketServer.Client client)
            {
                System.Diagnostics.Debug.WriteLine("OnNewClientConnect");
                client.SendData(Encoding.UTF8.GetBytes("Hello from server!"));
            }

            public void OnNewClientException(Exception ex)
            {
                
            }

            public void OnStartServerFailed(Exception ex)
            {
                
            }

            public void OnStartServerSuccess()
            {
                System.Diagnostics.Debug.WriteLine("OnStartServerSuccess");
            }

            public void OnStopServer()
            {
                
            }

            public void OnStopServerException(Exception ex)
            {
                
            }
        }

    }
}
