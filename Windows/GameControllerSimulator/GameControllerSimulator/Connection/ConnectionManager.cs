using BluetoothLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace GameControllerSimulator.Connection
{
    public class ConnectionManager
    {
        #region 内部使用的常量
        private static readonly Guid HOST_GUID = Guid.Parse("0000180D-0000-1000-8000-00805f9b34fb");

        private static readonly string BLUETOOTH_SERVICE_NAME = "GameControllerSimulator2";
        #endregion

        #region 对外暴露的属性
        public bool IsAvailable
        {
            get => IsBluetoothAvailable || IsTcpAvailable || IsUdpAvailable;
        }
        public ConnectionType CurrentConnectionType
        {
            get;
            set;
        }
        #endregion

        #region 内部字段
        private IConnectionManagerCallback? callback;
        private string[] clientIds = new string[4] { "", "", "", "" };
        #endregion

        #region 蓝牙相关逻辑
        private bool isBluetoothAvailable;
        private bool IsBluetoothAvailable
        {
            get => isBluetoothAvailable;
            set
            {
                bool current = IsAvailable;
                isBluetoothAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }
        private BluetoothSocketServer? bluetoothSocketServer;
        private BluetoothSocketServer.Client?[] clients = new BluetoothSocketServer.Client[4];
        #endregion

        #region Todo: TCP相关逻辑
        private bool isTcpAvailable;
        private bool IsTcpAvailable
        {
            get => isTcpAvailable;
            set
            {
                bool current = IsAvailable;
                isTcpAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }
        #endregion

        #region Todo: UDP相关逻辑
        private bool isUdpAvailable;
        private bool IsUdpAvailable
        {
            get => isUdpAvailable;
            set
            {
                bool current = IsAvailable;
                isUdpAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }
        #endregion



        #region 内部方法
        private string GetClientId(BluetoothSocketServer.Client client)
        {
            for (int i = 0; i < clients.Length; i++)
            {
                if (clients[i] == client)
                {
                    return clientIds[i];
                }
            }
            return "";
        }

        private bool SetClient(BluetoothSocketServer.Client client)
        {
            for (int i = 0; i < clientIds.Length; i++)
            {
                if (clientIds[i] == "" || clientIds[i] == null)
                {
                    clientIds[i] = Guid.NewGuid().ToString();
                    clients[i] = client;
                    return true;
                }
            }
            return false;
        }

        private bool RemoveClient(BluetoothSocketServer.Client client)
        {
            for (int i = 0; i < clients.Length; i++)
            {
                if (clients[i] == client) 
                {
                    clients[i] = null;
                    clientIds[i] = "";
                    return true;
                }
            }
            return false;
        }

        private void OnAvailableChange(bool current, bool now)
        {
            if (!current && now)
            {
                callback?.OnManagerAvaiable();
            }

            if (current && !now)
            {
                callback?.OnManagerUnavailable();
            }

        }

        private void OnManagerError(string msg, Exception ex)
        {
            callback?.OnFaulted(msg, ex);
        }

        private void OnDataReady(BluetoothSocketServer.Client client, byte[] data)
        {
            string clientId = GetClientId(client);
            if (clientId == null || clientId == "")
            {
                callback?.OnFaulted("OnDataReady. Unknow client data.", new Exception());
                return;
            }



            string jsonStr = Encoding.UTF8.GetString(data);
            using JsonDocument doc = JsonDocument.Parse(jsonStr);
            JsonElement root = doc.RootElement;



        }
        #endregion

        #region 辅助类型定义
        public enum ConnectionType
        {
            BLE, TCP, UDP
        }

        private class ServerCallback : BluetoothSocketCallback
        {

            private ConnectionManager? connectionManager;

            public ServerCallback(ConnectionManager? bluetoothSocketServer)
            {
                this.connectionManager = bluetoothSocketServer;
            }


            public BluetoothSocketCallback.ClientCallback CreateNewClientCallback()
            {
                return new ClientCallback(connectionManager);
            }

            public void OnForeverLoopException(Exception ex)
            {
                connectionManager?.OnManagerError("ServerCallback.OnForeverLoopException", ex);
            }

            public void OnNewClientConnect(BluetoothSocketServer.Client client)
            {
                if (false == connectionManager?.SetClient(client))
                {
                    client.Disconnect();
                }
            }

            public void OnNewClientException(Exception ex)
            {
                connectionManager?.OnManagerError("ServerCallback.OnNewClientException", ex);
            }

            public void OnStartServerFailed(Exception ex)
            {
                connectionManager?.OnManagerError("ServerCallback.OnStartServerFailed", ex);
            }

            public void OnStartServerSuccess()
            {
                if (connectionManager != null)
                {
                    connectionManager.IsBluetoothAvailable = true;
                }
            }

            public void OnStopServer()
            {
                if (connectionManager != null)
                {
                    connectionManager.IsBluetoothAvailable = false;
                }
            }

            public void OnStopServerException(Exception ex)
            {
                connectionManager?.OnManagerError("ServerCallback.OnStopServerException", ex);
            }
        }


        private class ClientCallback : BluetoothSocketCallback.ClientCallback
        {

            private ConnectionManager? connectionManager;
            public ClientCallback(ConnectionManager? connectionManager)
            {
                this.connectionManager = connectionManager;
            }

            public void OnDataReady(BluetoothSocketServer.Client client, byte[] data)
            {
                connectionManager?.OnDataReady(client, data);
            }

            public void OnDataRevException(BluetoothSocketServer.Client client, Exception ex)
            {
                connectionManager?.OnManagerError($"ClientCallback.OnDataRevException, clientId: {connectionManager.GetClientId(client)}", ex);
            }

            public void OnDisconnect(BluetoothSocketServer.Client client)
            {
               this.connectionManager?.RemoveClient(client);
            }

            public void OnSendDataException(BluetoothSocketServer.Client client, Exception ex, int id = -1)
            {
                connectionManager?.OnManagerError($"ClientCallback.OnSendDataException, clientId: {connectionManager.GetClientId(client)}, id: {id}", ex);
            }

        }
        #endregion
    }


    public interface IConnectionManagerCallback 
    {
        public void OnManagerAvaiable();
        public void OnManagerUnavailable();

        public void OnFaulted(string msg, Exception ex);

        public void OnDataReady(string clientId, object data);
    }

}
