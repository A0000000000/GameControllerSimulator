using BluetoothLibrary;
using InTheHand.Net.Sockets;
using LogLibrary;
using NetworkLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Net.Sockets;

namespace GameControllerSimulator.Connection
{
    public class TransportManager
    {
        public static readonly string TAG = "TransportManager";

        private Dictionary<ConnectionType, IServerTransport> transports = new();
        private Dictionary<ConnectionType, HashSet<IServerTransport.IClientTransport>> clientTransports = new()
        {
            { ConnectionType.BLE, new HashSet<IServerTransport.IClientTransport>() },
            { ConnectionType.TCP, new HashSet<IServerTransport.IClientTransport>() },
            { ConnectionType.UDP, new HashSet<IServerTransport.IClientTransport>() }
        };
        private ITransportManagerCallback callback;
        public TransportManager(ITransportManagerCallback callback)
        {
            this.callback = callback;
        }

        public void InitRFCOMM(Guid rfcommGuid, string svcName)
        {
            lock (this)
            {
                BluetoothSocketServer bluetoothSocketServer = new BluetoothSocketServer(rfcommGuid, svcName, new ServerCallbackImpl<BluetoothListener, BluetoothClient>(this, ConnectionType.BLE));
                bluetoothSocketServer.StartListener();
                transports[ConnectionType.BLE] = bluetoothSocketServer;
            }
        }

        public void InitTcp(int port)
        {
            lock (this)
            {
                TcpSocketServer tcpSocketServer = new TcpSocketServer(port, new ServerCallbackImpl<TcpListener, TcpClient>(this, ConnectionType.TCP));
                tcpSocketServer.StartListener();
                transports[ConnectionType.TCP] = tcpSocketServer;
            }
        }

        public void Destroy()
        {
            lock (this)
            {
                foreach (KeyValuePair<ConnectionType, HashSet<IServerTransport.IClientTransport>> items in clientTransports)
                {
                    foreach (IServerTransport.IClientTransport item in items.Value)
                    {
                        item.Disconnect();
                    }
                    items.Value.Clear();
                }
                foreach (KeyValuePair<ConnectionType, IServerTransport> item in transports)
                {
                    item.Value.StopListener();
                }
                transports.Clear();
            }
        }

        private void OnFault(string tag, string msg, ConnectionType type, Exception? ex = null)
        {
            LogUtils.E(TAG, $"[{tag}]-[{msg}]-[{type}]", ex);
            callback.OnFault(tag, msg, type, ex);
        }

        private void OnClientConnect<TSocket>(SocketClient<TSocket> client, ConnectionType type) where TSocket : class, IDisposable
        {
            clientTransports[type].Add(client);
        }

        private void OnStartServer(ConnectionType type)
        {
            callback.OnStartServer(type);
        }

        private void OnStopServer(ConnectionType type)
        {
            callback.OnStopServer(type);
        }

        private void OnDataReady<TSocket>(SocketClient<TSocket> client, byte[] data, ConnectionType type) where TSocket : class, IDisposable
        {
            callback.OnDataReady(client, data, type);
        }

        private void OnDisconnect<TSocket>(SocketClient<TSocket> client, ConnectionType type) where TSocket : class, IDisposable
        {
            clientTransports[type].Remove(client);
            callback.OnDisconnect(client, type);
        }

        #region 内部回调
        private class ServerCallbackImpl<TServerSocket, TSocket> : SocketServerCallback<TServerSocket, TSocket> where TServerSocket : class, IDisposable where TSocket : class, IDisposable
        {
            public static readonly string TAG = $"ServerCallbackImpl<{typeof(TServerSocket).Name}, {typeof(TSocket).Name}>";
            private TransportManager manager;
            private ConnectionType connectionType;

            public ServerCallbackImpl(TransportManager manager, ConnectionType connectionType)
            {
                this.manager = manager;
                this.connectionType = connectionType;
            }

            public SocketClientCallback<TSocket> CreateNewClientCallback()
            {
                return new SocketClientCallbackImpl<TSocket>(manager, connectionType);
            }

            public void OnForeverLoopException(Exception ex)
            {
                manager.OnFault(TAG, "OnForeverLoopException", connectionType, ex);
            }

            public void OnClientConnect(SocketClient<TSocket> client)
            {
                manager.OnClientConnect(client, connectionType);
            }

            public void OnNewClientException(Exception ex)
            {
                manager.OnFault(TAG, "OnNewClientException", connectionType, ex);
            }

            public void OnStartServerFailed(Exception ex)
            {
                manager.OnFault(TAG, "OnStartServerFailed", connectionType, ex);
            }

            public void OnStartServerSuccess()
            {
                manager.OnStartServer(connectionType);
            }

            public void OnStopServer()
            {
                manager.OnStopServer(connectionType);
            }

            public void OnStopServerException(Exception ex)
            {
                manager.OnFault(TAG, "OnStopServerException", connectionType, ex);
            }

            public void OnTaskException(Exception ex)
            {
                manager.OnFault(TAG, "OnTaskException", connectionType, ex);
            }

        }

        private class SocketClientCallbackImpl<TSocket> : SocketClientCallback<TSocket> where TSocket : class, IDisposable
        {
            public static readonly string TAG = $"SocketClientCallbackImpl<{typeof(TSocket).Name}>";
            private TransportManager manager;
            private ConnectionType type;

            public SocketClientCallbackImpl(TransportManager manager, ConnectionType type)
            {
                this.manager = manager;
                this.type = type;
            }
            public void OnDataReady(SocketClient<TSocket> client, byte[] data)
            {
                manager.OnDataReady(client, data, type);
            }

            public void OnDataRevException(SocketClient<TSocket> client, Exception ex)
            {
                manager.OnFault(TAG, "OnDataRevException", type, ex);
            }

            public void OnDisconnect(SocketClient<TSocket> client)
            {
                manager.OnDisconnect(client, type);
            }

            public void OnSendDataException(SocketClient<TSocket> client, Exception ex, int id = -1)
            {
                manager.OnFault(TAG, "OnSendDataException", type, ex);
            }

            public void OnTaskException(Exception ex)
            {
                manager.OnFault(TAG, "OnTaskException", type, ex);
            }
        }
        #endregion

    }

    public interface ITransportManagerCallback
    {
        void OnFault(string tag, string msg, ConnectionType type, Exception? ex);
        void OnStartServer(ConnectionType type);
        void OnStopServer(ConnectionType type);
        void OnDataReady(IServerTransport.IClientTransport client, byte[] data, ConnectionType type);
        void OnDisconnect(IServerTransport.IClientTransport client, ConnectionType type);

    }

}
