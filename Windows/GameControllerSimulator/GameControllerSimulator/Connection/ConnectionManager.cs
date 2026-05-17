using BluetoothLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace GameControllerSimulator.Connection
{
    public class ConnectionManager: IDisposable
    {
        #region 内部使用的常量
        private static readonly Guid HOST_GUID = Guid.Parse("0000180D-0000-1000-8000-00805f9b34fb");

        private const string BLUETOOTH_SERVICE_NAME = "GameControllerSimulator2";

        private static readonly int[] INTERNAL_ID_ARRAY = new int[] { EntityId.CONNECTION_MANAGER_INTERNAL_ID };
        #endregion

        #region 对外暴露接口
        public ConnectionManager(IConnectionManagerCallback callback)
        {
            this.callback = callback;
            bluetoothSocketServer = new BluetoothSocketServer(HOST_GUID, BLUETOOTH_SERVICE_NAME, new ServerCallback(this));
            IsBluetoothAvailable = false;
            IsTcpAvailable = false;
            IsUdpAvailable = false;
            CurrentConnectionType = ConnectionType.BLE;
        }

        public void Init()
        {
            bluetoothSocketServer?.StartListener();
        }

        public void Destroy()
        {
            bluetoothSocketServer?.StopListener();
            bluetoothSocketServer = null;
            for (int i = 0; i < clients.Length; i++)
            {
                clients[i]?.Disconnect();
            }
        }


        public void Dispose()
        {
            Destroy();
        }
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

        private void Post(Action action)
        {
            if (action == null) return;
            _ = Task.Run(() =>
            {
                try { action(); }
                catch { }
            });
        }

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

        private int GetClientIndex(BluetoothSocketServer.Client client)
        {
            for (int i = 0; i < clients.Length; i++)
            {
                if (clients[i] == client)
                {
                    return i;
                }
            }
            return -1;
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
            int type = -1;
            if (root.TryGetProperty("type", out JsonElement t))
            {
                type = t.ValueKind == JsonValueKind.Number ? t.GetInt32() : -1;
            }
            Type dataType = type == -1 ? typeof(JsonElement) : GetTypeClass(type);
            Type entityType = typeof(BaseEntity<>).MakeGenericType(dataType);
            object? entity = JsonSerializer.Deserialize(jsonStr, entityType);
            if (entity != null && entity is IBaseEntity baseEntity)
            {
                if (FilterById(baseEntity))
                {
                    OnDataReadyInner(client, baseEntity);
                }
                else
                {
                    callback?.OnDataReady(clientId, baseEntity);
                }
            }
            else
            {
                callback?.OnFaulted("OnDataReady. Deserialize data failed.", new Exception());
            }
        }

        private bool FilterById(IBaseEntity entity)
        {
            return INTERNAL_ID_ARRAY.Contains(entity.Id);
        }

        #endregion

        #region 结果处理方法
        private Type GetTypeClass(int type)
        {
            Type? typeClass = callback?.GetTypeClass(type);
            if (typeClass != null && typeClass != typeof(JsonElement))
            {
                return typeClass;
            }
            switch (type)
            {
                case EntityType.TYPE_REQUEST_CLIENT_ID:
                    return typeof(string);
                case EntityType.TYPE_UNREGISTER_CLIENT_ID:
                    return typeof(string);

                default:
                    return typeof(JsonElement);
            }
        }


        private void OnDataReadyInner(BluetoothSocketServer.Client client, IBaseEntity entity)
        {
            switch (entity.Type)
            {
                case EntityType.TYPE_REQUEST_CLIENT_ID:
                    client.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new BaseEntity<string>()
                    {
                        Type = EntityType.TYPE_REQUEST_CLIENT_ID,
                        Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                        Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        Data = GetClientId(client)
                    })));
                    callback?.OnNewClientConnection(GetClientId(client), GetClientIndex(client));
                    break;
                case EntityType.TYPE_UNREGISTER_CLIENT_ID:
                    RemoveClient(client);
                    client.Disconnect();
                    break;

                default:
                    break;
            }
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
        void OnManagerAvaiable();
        void OnManagerUnavailable();

        void OnFaulted(string msg, Exception ex);

        void OnDataReady(string clientId, IBaseEntity
            data);

        Type GetTypeClass(int type);

        void OnNewClientConnection(string clientId, int index);

    }

}
