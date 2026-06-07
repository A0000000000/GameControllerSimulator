using AsyncTaskLibrary;
using LogLibrary;
using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace SocketCommonLibrary
{
    public abstract class SocketServer<TServerSocker, TSocket> : IServerTransport where TServerSocker : class, IDisposable where TSocket : class, IDisposable
    {
        public static string TAG = "SocketServer";
        public abstract ConnectionType ConnectionType { get; }
        private readonly object _lock = new();
        private TServerSocker _serverSocker;
        private CancellationTokenSource cts;
        private SocketServerCallback<TServerSocker, TSocket> callback;
        private Task acceptLoopTask;

        public SocketServer(SocketServerCallback<TServerSocker, TSocket> callback)
        {
            LogUtils.I(TAG, "Create SocketServer");
            this.callback = callback;
        }

        protected abstract TServerSocker CreateServerSocket();
        protected abstract Task<TSocket> AcceptSocket(TServerSocker serverSocker);

        protected abstract SocketClient<TSocket> CreateAcceptClient(TSocket socket, SocketClientCallback<TSocket> clientCallback);
        protected abstract void Close(TServerSocker serverSocker);

        public void StartListener()
        {
            LogUtils.I(TAG, "SocketServer StartListener");
            AsyncTaskUtils.Post(() =>
            {
                LogUtils.I(TAG, "SocketServer StartListenerPost");
                lock (_lock)
                {
                    if (cts != null)
                    {
                        LogUtils.I(TAG, "SocketServer StartListenerPost Service has already running.");
                        return;
                    }
                    cts = new CancellationTokenSource();
                    try
                    {
                        LogUtils.I(TAG, "SocketServer StartListenerPost CreateServerSocket");
                        _serverSocker = CreateServerSocket();
                        callback?.OnStartServerSuccess();
                        acceptLoopTask = AsyncTaskUtils.Post(StartForeverLoop);
                    }
                    catch (Exception ex)
                    {
                        LogUtils.E(TAG, "SocketServer StartListenerPost CreateServerSocket failed.", ex);
                        _serverSocker = null;
                        cts.Dispose();
                        cts = null;
                        callback?.OnStartServerFailed(ex);
                    }
                }
            }, (Exception ex) =>
            {
                LogUtils.I(TAG, "SocketServer Post Failed", ex);
                callback?.OnTaskException(ex);
            }, TAG);
        }

        private async Task StartForeverLoop()
        {
            LogUtils.I(TAG, "SocketServer StartForeverLoop");
            try
            {
                while (!cts.Token.IsCancellationRequested)
                {
                    try
                    {
                        TSocket socket = await Task.Run(async () => await AcceptSocket(_serverSocker), cts.Token);
                        LogUtils.I(TAG, "SocketServer StartForeverLoop CreateAcceptClient");
                        SocketClient<TSocket> client = CreateAcceptClient(socket, callback?.CreateNewClientCallback());
                        _ = AsyncTaskUtils.Post(() => callback?.OnClientConnect(client));
                    }
                    catch (Exception ex)
                    {
                        LogUtils.W(TAG, "SocketServer StartForeverLoop AcceptSocket failed.", ex);
                        _  = AsyncTaskUtils.Post(() => callback?.OnNewClientException(ex));
                    }
                }
            }
            catch (Exception ex)
            {
                LogUtils.E(TAG, "SocketServer StartForeverLoop failed.", ex);
                callback?.OnForeverLoopException(ex);
            }
        }

        public void StopListener()
        {
            LogUtils.I(TAG, "SocketServer StopListener");
            AsyncTaskUtils.Post(() =>
            {
                Task taskToWait = null;
                lock (_lock)
                {
                    LogUtils.I(TAG, "SocketServer StopListenerPost");
                    if (cts == null)
                    {
                        LogUtils.I(TAG, "SocketServer StopListenerPost Service has already stop.");
                        return;
                    }
                    try
                    {
                        cts.Cancel();
                        Close(_serverSocker);
                        _serverSocker = null;
                        taskToWait = acceptLoopTask;
                    }
                    catch { }
                }
                try { taskToWait?.Wait(); } catch { }
                lock (_lock)
                {
                    cts.Dispose();
                    cts = null;
                    acceptLoopTask = null;
                }
                callback?.OnStopServer();
            }, (Exception ex) =>
            {
                LogUtils.I(TAG, "SocketServer Post Failed", ex);
                callback?.OnTaskException(ex);
            }, TAG);
        }

    }

    public abstract class SocketClient<TSocket>: IServerTransport.IClientTransport  where TSocket: class, IDisposable
    {
        public static string TAG = "SocketClient";
        public abstract ConnectionType ConnectionType { get; }
        private readonly object _lock = new();
        private TSocket socket;
        private SocketClientCallback<TSocket> callback;
        private Stream stream;
        public bool IsAvailable { get; private set; }

        protected abstract Stream GetStream(TSocket socket);
        protected abstract void Close(TSocket socket);


        public SocketClient(TSocket socket, SocketClientCallback<TSocket> callback)
        {
            LogUtils.I(TAG, "Create SocketClient");
            this.socket = socket;
            this.callback = callback;
            stream = GetStream(socket);
            IsAvailable = true;
            AsyncTaskUtils.Post(ReceiveLoop);
        }

        public void SendData(byte[] data, int id = -1)
        {
            LogUtils.I(TAG, $"SocketClient SendData id = [{id}]");
            AsyncTaskUtils.Post(() =>
            {
                try
                {
                    LogUtils.I(TAG, $"SocketClient SendDataPost id = [{id}]");
                    lock (_lock)
                    {
                        stream.Write(IntConverter.ToBigEndian(data.Length), 0, 4);
                        stream.Write(data, 0, data.Length);
                        stream.Flush();
                    }
                }
                catch (Exception ex)
                {
                    LogUtils.D(TAG, $"SocketClient SendData Failed id = [{id}]", ex);
                    callback?.OnSendDataException(this, ex, id);
                }
            }, (Exception ex) => 
            {
                LogUtils.W(TAG, "SocketClient Post Failed", ex);
                callback?.OnTaskException(ex);
            }, TAG);
        }
        public void Disconnect()
        {
            LogUtils.I(TAG, "SocketClient Disconnect");
            AsyncTaskUtils.Post(() =>
            {
                LogUtils.I(TAG, "SocketClient DisconnectPost");
                lock (_lock)
                {
                    if (!IsAvailable) 
                    {
                        LogUtils.I(TAG, "SocketClient DisconnectPost client has already disconnect");
                        return;
                    }
                    IsAvailable = false;
                }
                try
                {
                    Close(socket);
                }
                catch { }
                callback?.OnDisconnect(this);
            }, (Exception ex) =>
            {
                LogUtils.W(TAG, "SocketClient Post Failed", ex);
                callback?.OnTaskException(ex);
            }, TAG);
        }

        private async Task ReceiveLoop()
        {
            LogUtils.I(TAG, "SocketClient ReceiveLoop");
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
                    _ = AsyncTaskUtils.Post(() => callback?.OnDataReady(this, data));
                }
            }
            catch (Exception ex)
            {
                LogUtils.E(TAG, "SocketClient ReceiveLoop Failed", ex);
                callback?.OnDataRevException(this, ex);
            }
            finally
            {
                Disconnect();
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

    }

}
