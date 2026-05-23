using BluetoothLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using InTheHand.Net.Sockets;
using LogLibrary;
using NetworkLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace GameControllerSimulator.Connection
{
    public class ConnectionManager : IDisposable
    {

        #region 内部使用的常量
        public static readonly string TAG = "ConnectionManager";
        private static readonly int[] INTERNAL_ID_ARRAY = new int[] { EntityId.CONNECTION_MANAGER_INTERNAL_ID };
        #endregion

        #region 对外暴露类型
        public enum ConnectionType
        {
            BLE, TCP, UDP
        }

        #endregion

        #region 对外暴露接口
        public ConnectionManager(int connectionCount, IConnectionManagerCallback callback)
        {
            LogUtils.I(TAG, $"Create ConnectionManager connectionCount = [{connectionCount}]");
            this.callback = callback;
            this.connectionCount = connectionCount;
            clientIds = Enumerable.Repeat("", connectionCount).ToArray();
            bluetoothClients = new BluetoothSocketClient[connectionCount];
            tcpClients = new TcpSocketClient[connectionCount];
            IsBluetoothAvailable = false;
            IsTcpAvailable = false;
            IsUdpAvailable = false;
            CurrentConnectionType = ConnectionType.BLE;
        }


        public void Destroy()
        {
            LogUtils.I(TAG, $"ConnectionManager Destroy");
            DestroyRFCOMM();
            DestroyTcp();
        }

        public void SendData(int index, IBaseEntity data)
        {
            LogUtils.I(TAG, $"ConnectionManager SendData index = [{index}], data = [{JsonSerializer.Serialize(data)}]");
            if (index < 0 || index > connectionCount)
            {
                return;
            }
            switch (CurrentConnectionType)
            {
                case ConnectionType.BLE:
                    if (bluetoothClients[index] != null)
                    {
                        bluetoothClients[index]?.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data)));
                        return;
                    }
                    break;
                case ConnectionType.TCP:
                    if (tcpClients[index] != null)
                    {
                        tcpClients[index]?.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data)));
                        return;
                    }
                    break;

                case ConnectionType.UDP:

                    break;
            }
            if (bluetoothClients[index] != null)
            {
                bluetoothClients[index]?.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data)));
                return;
            }
            if (tcpClients[index] != null)
            {
                tcpClients[index]?.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data)));
                return;
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
        private int connectionCount;
        #endregion

        #region 通用处理逻辑
        private int GetClientIndex(string clientId)
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

        private string GetClientId<T>(SocketClient<T> client) where T: class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager GetClientId<{typeof(T).Name}>");
            SocketClient<T>[]? socketClients = typeof(T) switch
            {
                Type t when t == typeof(BluetoothClient) => bluetoothClients as SocketClient<T>[],
                Type t when t == typeof(TcpClient) => tcpClients as SocketClient<T>[],
                _ => null
            };
            if (socketClients == null)
            {
                return "";
            }
            for (int i = 0; i < socketClients.Length; i++)
            {
                if (socketClients[i] == client)
                {
                    return clientIds[i];
                }
            }
            return "";
        }

        private int GetClientIndex<T>(SocketClient<T> client) where T : class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager GetClientIndex<{typeof(T).Name}>");
            SocketClient<T>[]? socketClients = typeof(T) switch
            {
                Type t when t == typeof(BluetoothClient) => bluetoothClients as SocketClient<T>[],
                Type t when t == typeof(TcpClient) => tcpClients as SocketClient<T>[],
                _ => null
            };
            if (socketClients == null)
            {
                return -1;
            }
            for (int i = 0; i < socketClients.Length; i++)
            {
                if (socketClients[i] == client)
                {
                    return i;
                }
            }
            return -1;
        }

        private bool SetClient<T>(SocketClient<T> client) where T : class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager SetClient<{typeof(T).Name}>");
            SocketClient<T>[]? socketClients = typeof(T) switch
            {
                Type t when t == typeof(BluetoothClient) => bluetoothClients as SocketClient<T>[],
                Type t when t == typeof(TcpClient) => tcpClients as SocketClient<T>[],
                _ => null
            };
            if (socketClients == null)
            {
                return false;
            }
            for (int i = 0; i < clientIds.Length; i++)
            {
                if (clientIds[i] == "" || clientIds[i] == null)
                {
                    clientIds[i] = Guid.NewGuid().ToString();
                    socketClients[i] = client;
                    return true;
                }
            }
            return false;
        }

        private bool SetClient<T>(SocketClient<T> client, string clientId) where T : class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager SetClient<{typeof(T).Name}> clientId = [{clientId}]");
            SocketClient<T>[]? socketClients = typeof(T) switch
            {
                Type t when t == typeof(BluetoothClient) => bluetoothClients as SocketClient<T>[],
                Type t when t == typeof(TcpClient) => tcpClients as SocketClient<T>[],
                _ => null
            };
            int index = GetClientIndex(clientId);
            if (socketClients == null || (index < 0 || index >= connectionCount))
            {
                return false;
            }
            socketClients[index] = client;
            return true;
        }

        private bool RemoveClient<T>(SocketClient<T> client) where T : class, IDisposable 
        {
            /**
             * 这里有问题,应该根据类型去移除,全部都被移除才回调,暂时现这样写,后续想想怎么改
             */
            LogUtils.I(TAG, $"ConnectionManager RemoveClient<{typeof(T).Name}>");
            int index = GetClientIndex(client);
            if (index >= 0 && index < connectionCount)
            {
                clientIds[index] = "";
                bluetoothClients[index]?.Disconnect();
                bluetoothClients[index] = null;
                tcpClients[index]?.Disconnect();
                tcpClients[index] = null;
                callback?.OnClientDisconnected(index);
                return true;
            }
            return false;
        }

        #endregion

        #region 通用回调
        private class ServerCallbackImpl<TServerSocket, TSocket>: SocketServerCallback<TServerSocket, TSocket> where TServerSocket : class, IDisposable where TSocket : class, IDisposable
        {
            public static string TAG = $"ServerCallbackImpl<{typeof(TServerSocket).Name}, {typeof(TSocket).Name}>";
            private ConnectionManager? connectionManager;

            public ServerCallbackImpl(ConnectionManager? bluetoothSocketServer)
            {
                LogUtils.I(TAG, "Create ServerCallbackImpl");
                this.connectionManager = bluetoothSocketServer;
            }

            public SocketClientCallback<TSocket> CreateNewClientCallback()
            {
                LogUtils.I(TAG, "ServerCallbackImpl CreateNewClientCallback");
                return new SocketClientCallbackImpl<TSocket>(connectionManager);
            }

            public void OnForeverLoopException(Exception ex)
            {
                LogUtils.D(TAG, "ServerCallbackImpl OnForeverLoopException", ex);
                connectionManager?.OnManagerError("ServerCallbackImpl.OnForeverLoopException", ex);
            }

            public void OnNewClientConnect(SocketClient<TSocket> client)
            {
                LogUtils.I(TAG, "ServerCallbackImpl OnNewClientConnect");
            }

            public void OnNewClientException(Exception ex)
            {
                LogUtils.D(TAG, "ServerCallbackImpl OnNewClientException", ex);
                connectionManager?.OnManagerError("ServerCallbackImpl.OnNewClientException", ex);
            }

            public void OnStartServerFailed(Exception ex)
            {
                LogUtils.E(TAG, "ServerCallbackImpl OnStartServerFailed", ex);
                connectionManager?.OnManagerError("ServerCallbackImpl.OnStartServerFailed", ex);
            }

            public void OnStartServerSuccess()
            {
                LogUtils.I(TAG, "ServerCallbackImpl OnStartServerSuccess");
                if (connectionManager != null)
                {
                    Type type = typeof(TServerSocket);
                    if (type == typeof(BluetoothListener))
                    {
                        connectionManager.IsBluetoothAvailable = true;
                    }
                    if (type == typeof(TcpListener))
                    {
                        connectionManager.IsTcpAvailable = true;
                    }
                }
            }

            public void OnStopServer()
            {
                LogUtils.I(TAG, "ServerCallbackImpl OnStopServer");
                if (connectionManager != null)
                {
                    Type type = typeof(TServerSocket);
                    if (type == typeof(BluetoothListener))
                    {
                        connectionManager.IsBluetoothAvailable = false;
                    }
                    if (type == typeof(TcpListener))
                    {
                        connectionManager.isTcpAvailable = false;
                    }
                }
            }

            public void OnStopServerException(Exception ex)
            {
                LogUtils.W(TAG, "ServerCallbackImpl OnStopServerException", ex);
                connectionManager?.OnManagerError("ServerCallbackImpl.OnStopServerException", ex);
            }

            public void OnTaskException(Exception ex)
            {
                LogUtils.W(TAG, "ServerCallbackImpl OnTaskException", ex);
                connectionManager?.OnManagerError("ServerCallbackImpl.OnTaskException", ex);
            }
        }

        private class SocketClientCallbackImpl<TSocket> : SocketClientCallback<TSocket> where TSocket: class, IDisposable
        {
            public static string TAG = $"SocketClientCallbackImpl<{typeof(TSocket).Name}>";
            private ConnectionManager? connectionManager;

            public SocketClientCallbackImpl(ConnectionManager? connectionManager)
            {
                LogUtils.I(TAG, "Create SocketClientCallbackImpl");
                this.connectionManager = connectionManager;
            }

            public void OnDataReady(SocketClient<TSocket> client, byte[] data)
            {
                LogUtils.I(TAG, "SocketClientCallbackImpl OnDataReady");
                connectionManager?.OnDataReady(client, data);
            }

            public void OnDataRevException(SocketClient<TSocket> client, Exception ex)
            {
                LogUtils.W(TAG, "SocketClientCallbackImpl OnDataRevException", ex);
                connectionManager?.OnManagerError($"SocketClientCallbackImpl.OnDataRevException, clientId: {connectionManager.GetClientId(client)}", ex);
            }

            public void OnDisconnect(SocketClient<TSocket> client)
            {
                LogUtils.I(TAG, "SocketClientCallbackImpl OnDisconnect");
                this.connectionManager?.RemoveClient(client);
            }

            public void OnSendDataException(SocketClient<TSocket> client, Exception ex, int id = -1)
            {
                LogUtils.W(TAG, $"SocketClientCallbackImpl OnSendDataException id = [{id}]", ex);
                connectionManager?.OnManagerError($"SocketClientCallbackImpl.OnSendDataException, clientId: {connectionManager.GetClientId(client)}, id: {id}", ex);
            }

            public void OnTaskException(Exception ex)
            {
                LogUtils.D(TAG, $"SocketClientCallbackImpl OnTaskException", ex);
                connectionManager?.OnManagerError("SocketClientCallbackImpl.OnTaskException", ex);
            }
        }

        #endregion

        #region 蓝牙相关逻辑
        private bool isBluetoothAvailable;
        private bool IsBluetoothAvailable
        {
            get => isBluetoothAvailable;
            set
            {
                if (value && !isBluetoothAvailable)
                {
                    callback?.OnConnectionAvaiableChange(true, ConnectionType.BLE);
                }
                if (!value && isBluetoothAvailable)
                {
                    callback?.OnConnectionAvaiableChange(false, ConnectionType.BLE);
                }
                bool current = IsAvailable;
                isBluetoothAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }
        private SocketServer<BluetoothListener, BluetoothClient>? bluetoothSocketServer;
        private SocketClient<BluetoothClient>?[] bluetoothClients;

        public void InitRFCOMM(Guid rfcommGuid, string svcName)
        {
            LogUtils.I(TAG, $"ConnectionManager InitRFCOMM rfcommGuid = [{rfcommGuid.ToString()}], svcName = [{svcName}]");
            bluetoothSocketServer = new BluetoothSocketServer(rfcommGuid, svcName, new ServerCallbackImpl<BluetoothListener, BluetoothClient>(this));
            bluetoothSocketServer?.StartListener();
        }

        public void DestroyRFCOMM()
        {
            LogUtils.I(TAG, $"ConnectionManager DestroyRFCOMM");
            bluetoothSocketServer?.StopListener();
            bluetoothSocketServer = null;
            for (int i = 0; i < bluetoothClients.Length; i++)
            {
                bluetoothClients[i]?.Disconnect();
                bluetoothClients[i] = null;
            }
        }

        #endregion

        #region TCP相关逻辑
        private bool isTcpAvailable;
        private bool IsTcpAvailable
        {
            get => isTcpAvailable;
            set
            {
                if (value && !isTcpAvailable)
                {
                    callback?.OnConnectionAvaiableChange(true, ConnectionType.TCP);
                }
                if (!value && isTcpAvailable)
                {
                    callback?.OnConnectionAvaiableChange(false, ConnectionType.TCP);
                }
                bool current = IsAvailable;
                isTcpAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }

        private SocketServer<TcpListener, TcpClient>? tcpSocketServer;
        private SocketClient<TcpClient>?[] tcpClients; 

        public void InitTcp(int port)
        {
            LogUtils.I(TAG, $"ConnectionManager InitTcp port = [{port}]");
            tcpSocketServer = new TcpSocketServer(port, new ServerCallbackImpl<TcpListener, TcpClient>(this));
            tcpSocketServer?.StartListener();
        }

        public void DestroyTcp()
        {
            LogUtils.I(TAG, $"ConnectionManager DestroyTcp");
            tcpSocketServer?.StopListener();
            tcpSocketServer = null;
            for (int i = 0; i < tcpClients.Length; i++)
            {
                tcpClients[i]?.Disconnect();
                tcpClients[i] = null;
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
                if (value && !isUdpAvailable)
                {
                    callback?.OnConnectionAvaiableChange(true, ConnectionType.UDP);
                }
                if (!value && isUdpAvailable)
                {
                    callback?.OnConnectionAvaiableChange(false, ConnectionType.UDP);
                }
                bool current = IsAvailable;
                isUdpAvailable = value;
                OnAvailableChange(current, IsAvailable);
            }
        }
        #endregion

        #region 内部方法
        private void OnAvailableChange(bool current, bool now)
        {
            LogUtils.I(TAG, $"ConnectionManager OnAvailableChange current = [{current}], now = [{now}]");
            if (!current && now)
            {
                callback?.OnManagerAvaiableChange(true);
            }
            if (current && !now)
            {
                callback?.OnManagerAvaiableChange(false);
            }
        }

        private void OnManagerError(string msg, Exception ex)
        {
            LogUtils.I(TAG, $"ConnectionManager OnManagerError msg = [{msg}]", ex);
            callback?.OnFaulted(msg, ex);
        }
        #endregion

        #region 结果处理方法
        private bool FilterById(IBaseEntity entity)
        {
            LogUtils.I(TAG, $"ConnectionManager FilterById");
            return INTERNAL_ID_ARRAY.Contains(entity.Id);
        }

        private Type GetTypeClass(int type)
        {
            Type? typeClass = callback?.GetTypeClass(type);
            if (typeClass != null && typeClass != typeof(JsonElement))
            {
                return typeClass;
            }
            return EntityType.TYPE_MAPPING.ContainsKey(type) ? EntityType.TYPE_MAPPING[type] : typeof(JsonElement);
        }

        private void OnDataReady<T>(SocketClient<T> client, byte[] data) where T : class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager OnDataReady<{typeof(T).Name}>");
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
                    callback?.OnDataReady(GetClientIndex(client), baseEntity);
                }
            }
            else
            {
                callback?.OnFaulted($"OnDataReady<{typeof(T).Name}>. Deserialize data failed.", new Exception());
            }
        }

        private void OnDataReadyInner<T>(SocketClient<T> client, IBaseEntity entity) where T : class, IDisposable
        {
            LogUtils.I(TAG, $"ConnectionManager OnDataReadyInner<{typeof(T).Name}>");
            switch (entity.Type)
            {
                case EntityType.TYPE_REQUEST_CLIENT_ID:
                    SetClient(client);
                    client.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new BaseEntity<string>()
                    {
                        Type = EntityType.TYPE_REQUEST_CLIENT_ID_RESULT,
                        Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                        Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        Data = GetClientId(client)
                    })));
                    callback?.OnNewClientConnection(GetClientIndex(client));
                    break;
                case EntityType.TYPE_UNREGISTER_CLIENT_ID:
                    RemoveClient(client);
                    client.Disconnect();
                    break;
                case EntityType.TYPE_NEW_TYPE_CONNECT:
                    SocketClient<T>[]? socketClients = typeof(T) switch
                    {
                        Type t when t == typeof(BluetoothClient) => bluetoothClients as SocketClient<T>[],
                        Type t when t == typeof(TcpClient) => tcpClients as SocketClient<T>[],
                        _ => null
                    };
                    int index = GetClientIndex(entity.Data?.ToString() ?? "");
                    if (socketClients != null && (index >= 0 && index < connectionCount))
                    {
                        socketClients[index] = client;
                        client.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_NEW_TYPE_CONNECT_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = "success"
                        })));
                    }
                    else
                    {
                        client.SendData(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_NEW_TYPE_CONNECT_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = "failed"
                        })));
                    }
                    break;
                default:
                    break;
            }
        }

        #endregion

    }

}
