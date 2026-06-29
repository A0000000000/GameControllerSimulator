using BluetoothLibrary;
using InTheHand.Net.Sockets;
using LogLibrary;
using NetworkLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
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

        public void InitUdp(int port)
        {
            lock (this)
            {
                UdpServer udpServer = new UdpServer(port, new UdpServerCallbakImpl(this));
                udpServer.StartListener();
                transports[ConnectionType.UDP] = udpServer;
            }
        }

        public void Destroy()
        {
            lock (this)
            {
                foreach (KeyValuePair<ConnectionType, HashSet<IServerTransport.IClientTransport>> items in clientTransports)
                {
                    List<IServerTransport.IClientTransport> clientList = items.Value.ToList();
                    foreach (IServerTransport.IClientTransport item in clientList)
                    {
                        item.Disconnect();
                    }
                    items.Value.Clear();
                }
                List<IServerTransport> serverList = transports.Values.ToList();
                foreach (IServerTransport item in serverList)
                {
                    item.StopListener();
                }
                transports.Clear();
            }
        }

        private void OnFault(string tag, string msg, ConnectionType type, Exception? ex = null)
        {
            LogUtils.E(TAG, $"[{tag}]-[{msg}]-[{type}]", ex);
            callback.OnFault($"[{tag}]-[{msg}]-[{type}]", ex);
        }

        private void OnClientConnect(IServerTransport.IClientTransport client, ConnectionType type)
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

        private void OnDataReady(IServerTransport.IClientTransport client, byte[] data, ConnectionType type)
        {
            callback.OnDataReady(client, data, type);
        }

        private void OnDisconnect(IServerTransport.IClientTransport client, ConnectionType type)
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
                LogUtils.I(TAG, $"OnDataReady type is {type}");
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

        private class UdpServerCallbakImpl : UdpServerCallback
        {
            public static readonly string TAG = "UdpServerCallbakImpl";
            private TransportManager manager;
            public UdpServerCallbakImpl(TransportManager manager)
            {
                this.manager = manager;
            }

            public void OnClientConnected(IServerTransport.IClientTransport client)
            {
                manager.OnClientConnect(client, ConnectionType.UDP);
            }

            public void OnClientDisConnected(IServerTransport.IClientTransport client)
            {
                manager.OnDisconnect(client, ConnectionType.UDP);
            }

            public void OnDataReady(UdpClientWrapper client, byte[] data)
            {
                manager.OnDataReady(client, data, ConnectionType.UDP);
            }

            public void OnException(Exception ex)
            {
                manager.OnFault(TAG, "OnException", ConnectionType.UDP, ex);
            }

            public void OnStartServer()
            {
                manager.OnStartServer(ConnectionType.UDP);
            }

            public void OnStopServer()
            {
                manager.OnStopServer(ConnectionType.UDP);
            }
        }

        #endregion

    }

    public interface ITransportManagerCallback
    {
        void OnFault(string msg, Exception? ex);
        void OnStartServer(ConnectionType type);
        void OnStopServer(ConnectionType type);
        void OnDataReady(IServerTransport.IClientTransport client, byte[] data, ConnectionType type);
        void OnDisconnect(IServerTransport.IClientTransport client, ConnectionType type);

    }

}
