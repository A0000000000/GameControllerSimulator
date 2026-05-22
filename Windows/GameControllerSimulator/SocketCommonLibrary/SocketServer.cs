using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace SocketCommonLibrary
{
    public abstract class SocketServer<TServerSocker, TSocket> where TServerSocker : class, IDisposable where TSocket : class, IDisposable
    {
        private readonly object _lock = new();
        private TServerSocker _serverSocker;
        private CancellationTokenSource cts;
        private SocketServerCallback<TServerSocker, TSocket> callback;

        public SocketServer(SocketServerCallback<TServerSocker, TSocket> callback)
        {
            this.callback = callback;
        }

        protected abstract TServerSocker CreateServerSocket();
        protected abstract Task<TSocket> AcceptSocket(TServerSocker serverSocker);

        protected abstract Client<TSocket> CreateAcceptClient(TSocket socket, ClientCallback<TSocket> clientCallback);
        protected abstract void Close(TServerSocker serverSocker);


        public void StartListener()
        {
            Post(() =>
            {
                lock (_lock)
                {
                    if (cts != null)
                    {
                        return;
                    }
                    cts = new CancellationTokenSource();
                    try
                    {
                        _serverSocker = CreateServerSocket();
                        callback?.OnStartServerSuccess();
                        Task.Run(async () => StartForeverLoop());
                    }
                    catch (Exception ex)
                    {
                        _serverSocker = null;
                        cts.Dispose();
                        cts = null;
                        Post(() => callback?.OnStartServerFailed(ex));
                    }
                }
            });
        }

        private async Task StartForeverLoop()
        {
            try
            {
                while (!cts.IsCancellationRequested)
                {
                    try
                    {
                        TSocket socket = await Task.Run(async () => await AcceptSocket(_serverSocker), cts.Token);
                        Client<TSocket> client = CreateAcceptClient(socket, callback?.CreateNewClientCallback());
                        Post(() => callback?.OnNewClientConnect(client));
                    }
                    catch (Exception ex)
                    {
                        Post(() => callback?.OnNewClientException(ex));
                    }
                }
            }
            catch (Exception ex)
            {
                Post(() => callback?.OnForeverLoopException(ex));
            }
        }



        public void StopListener()
        {
            Post(() =>
            {
                lock (_lock)
                {
                    if (cts == null)
                    {
                        return;
                    }
                    try
                    {
                        cts.Cancel();
                        cts.Dispose();
                        cts = null;
                        Close(_serverSocker);
                        _serverSocker = null;
                    }
                    catch { }
                    Post(() => callback?.OnStopServer());
                }
            });
        }



        private void Post(Action action)
        {
            if (action == null) return;
            _ = Task.Run(() =>
            {
                try
                {
                    action();
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnTaskException(ex));
                }
            });
        }

    }

    public abstract class Client<TSocket> where TSocket: class, IDisposable
    {
        private readonly object _lock = new();
        private TSocket socket;
        private ClientCallback<TSocket> callback;
        private Stream stream;
        public bool IsAvailable { get; private set; }

        protected abstract Stream GetStream(TSocket socket);
        protected abstract void Close(TSocket socket);


        public Client(TSocket socket, ClientCallback<TSocket> callback)
        {
            this.socket = socket;
            this.callback = callback;
            stream = GetStream(socket);
            Task.Run(async () => ReceiveLoop());
            IsAvailable = true;
        }

        public void SendData(byte[] data, int id = -1)
        {
            Post(() =>
            {
                try
                {
                    lock (_lock)
                    {
                        stream.Write(IntConverter.ToBigEndian(data.Length), 0, 4);
                        stream.Write(data, 0, data.Length);
                        stream.Flush();
                    }
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnSendDataException(this, ex, id));
                }
            });
        }
        public void Disconnect()
        {
            Post(() =>
            {
                lock (_lock)
                {
                    if (!IsAvailable) return;
                    IsAvailable = false;
                }
                try
                {
                    Close(socket);
                }
                catch { }
                Post(() => callback?.OnDisconnect(this));
            });
        }

        private async Task ReceiveLoop()
        {
            try
            {
                byte[] sizeBuff = new byte[4];
                while (IsAvailable)
                {
                    int read = await ReadExact(sizeBuff, 4);
                    if (read == 0)
                    {
                        break;
                    }
                    int size = IntConverter.FromBigEndian(sizeBuff);
                    byte[] data = new byte[size];
                    read = await ReadExact(data, size);
                    if (read == 0)
                    {
                        break;
                    }
                    Post(() => callback?.OnDataReady(this, data));
                }
            }
            catch (Exception ex)
            {
                Post(() => callback?.OnDataRevException(this, ex));
            }
        }

        private async Task<int> ReadExact(byte[] buffer, int size)
        {
            int offset = 0;
            while (offset < size)
            {
                int read = await stream.ReadAsync(buffer, offset, size - offset);
                if (read <= 0) return 0;
                offset += read;
            }
            return offset;
        }

        private void Post(Action action)
        {
            if (action == null) return;
            _ = Task.Run(() =>
            {
                try
                {
                    action();
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnTaskException(ex));
                }
            });
        }
    }

}
