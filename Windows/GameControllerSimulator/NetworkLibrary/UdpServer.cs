using AsyncTaskLibrary;
using LogLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;

namespace NetworkLibrary
{
    public class UdpServer : IServerTransport
    {
        public ConnectionType ConnectionType => ConnectionType.UDP;

        private int port;
        private UdpServerCallback callback;
        private UdpClient udpClient;
        private Dictionary<string, UdpClientWrapper> clients = new Dictionary<string, UdpClientWrapper>();

        public UdpServer(int port, UdpServerCallback callback)
        {
            this.port = port;
            this.callback = callback;
        }


        public void StartListener()
        {
            lock (this)
            {
                if (udpClient != null)
                {
                    return;
                }
                udpClient = new UdpClient(port);
            }
            callback.OnStartServer();
            ReceiveDataLoop();
        }

        private void ReceiveDataLoop()
        {
            AsyncTaskUtils.Post(() =>
            {
                try
                {
                    UdpClient client = udpClient;
                    while (client != null)
                    {
                        IPEndPoint remote = new IPEndPoint(IPAddress.Any, 0);
                        byte[] data = client.Receive(ref remote);
                        string clientKey = $"{remote.Address.ToString()}:{remote.Port}";
                        bool first = false;
                        UdpClientWrapper clientWrapper = null;
                        lock (this)
                        {
                            if (!clients.ContainsKey(clientKey))
                            {
                                UdpClientWrapper wrapper = new UdpClientWrapper(this, remote);
                                clients[clientKey] = wrapper;
                                first = true;
                            }
                            clientWrapper = clients[clientKey];
                        }
                        if (clientWrapper != null)
                        {
                            if (first)
                            {
                                callback.OnClientConnected(clientWrapper);
                            }
                            callback.OnDataReady(clientWrapper, data);
                        }
                    }
                }
                catch (ObjectDisposedException ignore) { }
                catch (Exception ex)
                {
                    callback.OnException(ex);
                    StopListener();
                }
            }, ex => callback.OnException(ex));
        }

        public void SendData(IPEndPoint remote, byte[] data, int id = -1)
        {
            lock (this)
            {
                if (udpClient != null)
                {
                    udpClient.Send(data, remote.Address.ToString(), remote.Port);
                }
            }
        }

        public void RemoveClient(IPEndPoint remote)
        {
            UdpClientWrapper wrapper = null;
            lock (this)
            {
                string clientKey = $"{remote.Address.ToString()}:{remote.Port}";
                if (clients.ContainsKey(clientKey))
                {
                    wrapper = clients[clientKey];
                    clients.Remove(clientKey);
                }
            }
            if (wrapper != null)
            {
                callback.OnClientDisConnected(wrapper);
            }
        }

        public void StopListener()
        {
            lock (this)
            {
                if (udpClient == null)
                {
                    return;
                }
                udpClient.Close();
                udpClient = null;
            }
            callback.OnStopServer();
        }
    }

    public class UdpClientWrapper : IServerTransport.IClientTransport
    {
        public static readonly string TAG = "UdpClientWrapper";
        public ConnectionType ConnectionType => ConnectionType.UDP;

        private UdpServer udpServer;
        private IPEndPoint remote;

        public UdpClientWrapper(UdpServer udpServer, IPEndPoint remote)
        {
            this.udpServer = udpServer;
            this.remote = remote;
        }

        public bool IsAvailable => udpServer != null && remote != null;

        public void Disconnect()
        {
            UdpServer server = udpServer;
            IPEndPoint endpoint = remote;
            udpServer = null;
            remote = null;
            if (server == null || endpoint == null)
            {
                return;
            }
            server.RemoveClient(endpoint);
        }

        public void SendData(byte[] data, int id = -1)
        {
            LogUtils.I(TAG, $"UdpClientWrapper SendData id = [{id}], type = [{ConnectionType}]");
            if (IsAvailable)
            {
                udpServer.SendData(remote, data, id);
            }
        }
    }


    public interface UdpServerCallback 
    {
        void OnStartServer();
        void OnStopServer();
        void OnClientConnected(IServerTransport.IClientTransport client);
        void OnClientDisConnected(IServerTransport.IClientTransport client);
        void OnDataReady(UdpClientWrapper client, byte[] data);
        void OnException(Exception ex);

    }

}
