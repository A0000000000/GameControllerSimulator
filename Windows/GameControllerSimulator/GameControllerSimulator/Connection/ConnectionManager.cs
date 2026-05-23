using BluetoothLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using InTheHand.Net.Sockets;
using LogLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace GameControllerSimulator.Connection
{
    public class ConnectionManager : IDisposable
    {
        public static string TAG = "ConnectionManager";

        #region 内部使用的常量

        private static readonly int[] INTERNAL_ID_ARRAY = new int[] { EntityId.CONNECTION_MANAGER_INTERNAL_ID };
        #endregion


        #region 对外暴露接口
        public ConnectionManager(Guid rfcommGuid, string svcName, int connectionCount, IConnectionManagerCallback callback)
        {
            LogUtils.I(TAG, $"Create ConnectionManager RFCOMM CUID = [{rfcommGuid.ToString()}], svcName = [{svcName}], connectionCount = [{connectionCount}]");
            this.callback = callback;
            clientIds = Enumerable.Repeat("", connectionCount).ToArray();
            bluetoothClients = new BluetoothSocketClient[connectionCount];
            bluetoothSocketServer = new BluetoothSocketServer(rfcommGuid, svcName, new ServerCallback(this));
            IsBluetoothAvailable = false;
            IsTcpAvailable = false;
            IsUdpAvailable = false;
            CurrentConnectionType = ConnectionType.BLE;
        }

        public void Init()
        {
            LogUtils.I(TAG, $"ConnectionManager Init");
            bluetoothSocketServer?.StartListener();
        }

        public void Destroy()
        {
            LogUtils.I(TAG, $"ConnectionManager Destroy");
            bluetoothSocketServer?.StopListener();
            bluetoothSocketServer = null;
            for (int i = 0; i < bluetoothClients.Length; i++)
            {
                bluetoothClients[i]?.Disconnect();
            }
        }

        public int GetClientIndex(string clientId)
        {
            LogUtils.I(TAG, $"ConnectionManager GetClientIndex clientId = [{clientId}]");
            for (int i = 0; i < clientIds.Length; i++)
            {
                if (clientIds[i] == clientId)
                {
                    return i;
                }
            }
            return -1;
        }

        public void SendData(string clientId, IBaseEntity data)
        {
            LogUtils.I(TAG, $"ConnectionManager SendData clientId = [{clientId}], data = [{JsonSerializer.Serialize(data)}]");
            for (int i = 0; i < clientIds.Length; i++)
            {
                if (clientIds[i] == clientId)
                {
                    bluetoothClients[i]?.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data)));
                    break;
                }
            }
        }

        public void Dispose()
        {
            LogUtils.I(TAG, $"ConnectionManager Dispose");
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
        private string[] clientIds;
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
        private SocketServer<BluetoothListener, BluetoothClient>? bluetoothSocketServer;
        private SocketClient<BluetoothClient>?[] bluetoothClients;
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

        private string GetClientId(SocketClient<BluetoothClient> client)
        {
            LogUtils.I(TAG, $"ConnectionManager GetClientId");
            for (int i = 0; i < bluetoothClients.Length; i++)
            {
                if (bluetoothClients[i] == client)
                {
                    return clientIds[i];
                }
            }
            return "";
        }

        private int GetClientIndex(SocketClient<BluetoothClient> client)
        {
            LogUtils.I(TAG, $"ConnectionManager GetClientIndex");
            for (int i = 0; i < bluetoothClients.Length; i++)
            {
                if (bluetoothClients[i] == client)
                {
                    return i;
                }
            }
            return -1;
        }

        private bool SetClient(SocketClient<BluetoothClient> client)
        {
            LogUtils.I(TAG, $"ConnectionManager SetClient");
            for (int i = 0; i < clientIds.Length; i++)
            {
                if (clientIds[i] == "" || clientIds[i] == null)
                {
                    clientIds[i] = Guid.NewGuid().ToString();
                    bluetoothClients[i] = client;
                    return true;
                }
            }
            return false;
        }

        private bool RemoveClient(SocketClient<BluetoothClient> client)
        {
            LogUtils.I(TAG, $"ConnectionManager RemoveClient");
            for (int i = 0; i < bluetoothClients.Length; i++)
            {
                if (bluetoothClients[i] == client)
                {
                    bluetoothClients[i] = null;
                    clientIds[i] = "";
                    callback?.OnClientDisconnected(i);
                    return true;
                }
            }
            return false;
        }

        private void OnAvailableChange(bool current, bool now)
        {
            LogUtils.I(TAG, $"ConnectionManager OnAvailableChange current = [{current}], now = [{now}]");
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
            LogUtils.I(TAG, $"ConnectionManager OnManagerError msg = [{msg}]", ex);
            callback?.OnFaulted(msg, ex);
        }

        private void OnDataReady(SocketClient<BluetoothClient> client, byte[] data)
        {
            string clientId = GetClientId(client);
            LogUtils.I(TAG, $"ConnectionManager OnDataReady clientId = [{clientId}]");
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
            LogUtils.I(TAG, $"ConnectionManager FilterById");
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
            return EntityType.TYPE_MAPPING.ContainsKey(type) ? EntityType.TYPE_MAPPING[type] : typeof(JsonElement);
        }


        private void OnDataReadyInner(SocketClient<BluetoothClient> client, IBaseEntity entity)
        {
            LogUtils.I(TAG, $"ConnectionManager OnDataReadyInner");
            switch (entity.Type)
            {
                case EntityType.TYPE_REQUEST_CLIENT_ID:
                    client.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new BaseEntity<string>()
                    {
                        Type = EntityType.TYPE_REQUEST_CLIENT_ID_RESULT,
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

        private class ServerCallback : SocketServerCallback<BluetoothListener, BluetoothClient>
        {
            public static string TAG = "ServerCallback";

            private ConnectionManager? connectionManager;

            public ServerCallback(ConnectionManager? bluetoothSocketServer)
            {
                LogUtils.I(TAG, "Create ServerCallback");
                this.connectionManager = bluetoothSocketServer;
            }

            public void OnForeverLoopException(Exception ex)
            {
                LogUtils.D(TAG, "ServerCallback OnForeverLoopException", ex);
                connectionManager?.OnManagerError("ServerCallback.OnForeverLoopException", ex);
            }

            public void OnNewClientConnect(SocketClient<BluetoothClient> client)
            {
                LogUtils.I(TAG, "ServerCallback OnNewClientConnect");
                if (false == connectionManager?.SetClient(client))
                {
                    client.Disconnect();
                }
            }

            public void OnNewClientException(Exception ex)
            {
                LogUtils.D(TAG, "ServerCallback OnNewClientException", ex);
                connectionManager?.OnManagerError("ServerCallback.OnNewClientException", ex);
            }

            public void OnStartServerFailed(Exception ex)
            {
                LogUtils.E(TAG, "ServerCallback OnStartServerFailed", ex);
                connectionManager?.OnManagerError("ServerCallback.OnStartServerFailed", ex);
            }

            public void OnStartServerSuccess()
            {
                LogUtils.I(TAG, "ServerCallback OnStartServerSuccess");
                if (connectionManager != null)
                {
                    connectionManager.IsBluetoothAvailable = true;
                }
            }

            public void OnStopServer()
            {
                LogUtils.I(TAG, "ServerCallback OnStopServer");
                if (connectionManager != null)
                {
                    connectionManager.IsBluetoothAvailable = false;
                }
            }

            public void OnStopServerException(Exception ex)
            {
                LogUtils.W(TAG, "ServerCallback OnStopServerException", ex);
                connectionManager?.OnManagerError("ServerCallback.OnStopServerException", ex);
            }

            public void OnTaskException(Exception ex)
            {
                LogUtils.W(TAG, "ServerCallback OnTaskException", ex);
                connectionManager?.OnManagerError("ServerCallback.OnTaskException", ex);
            }

            public SocketClientCallback<BluetoothClient> CreateNewClientCallback()
            {
                LogUtils.I(TAG, "ServerCallback CreateNewClientCallback");
                return new ClientCallback(connectionManager);
            }
        }


        private class ClientCallback : SocketClientCallback<BluetoothClient>
        {
            public static string TAG = "ClientCallback";
            private ConnectionManager? connectionManager;
            public ClientCallback(ConnectionManager? connectionManager)
            {
                LogUtils.I(TAG, "Create ClientCallback");
                this.connectionManager = connectionManager;
            }

            public void OnDataReady(SocketClient<BluetoothClient> client, byte[] data)
            {
                LogUtils.I(TAG, "ClientCallback OnDataReady");
                connectionManager?.OnDataReady(client, data);
            }

            public void OnDataRevException(SocketClient<BluetoothClient> client, Exception ex)
            {
                LogUtils.W(TAG, "ClientCallback OnDataRevException", ex);
                connectionManager?.OnManagerError($"ClientCallback.OnDataRevException, clientId: {connectionManager.GetClientId(client)}", ex);
            }


            public void OnDisconnect(SocketClient<BluetoothClient> client)
            {
                LogUtils.I(TAG, "ClientCallback OnDisconnect");
                this.connectionManager?.RemoveClient(client);
            }

            public void OnSendDataException(SocketClient<BluetoothClient> client, Exception ex, int id = -1)
            {
                LogUtils.W(TAG, $"ClientCallback OnSendDataException id = [{id}]", ex);
                connectionManager?.OnManagerError($"ClientCallback.OnSendDataException, clientId: {connectionManager.GetClientId(client)}, id: {id}", ex);
            }

            public void OnTaskException(Exception ex)
            {
                LogUtils.D(TAG, $"ClientCallback OnTaskException", ex);
                connectionManager?.OnManagerError("ClientCallback.OnTaskException", ex);
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
        void OnClientDisconnected(int index);

    }

}
