using InTheHand.Net.Sockets;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net.Sockets;
using System.Threading;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace BluetoothLibrary
{

    public class BluetoothSocketServer
    {
        private readonly object _lock = new();

        public Guid guid { get; private set; }
        public string serviceName { get; private set; }

        private BluetoothListener bluetoothListener;
        private readonly BluetoothSocketCallback callback;

        private CancellationTokenSource cts;
        private Task acceptLoopTask;

        public BluetoothSocketServer(Guid guid, string serviceName, BluetoothSocketCallback callback)
        {
            this.guid = guid;
            this.serviceName = serviceName;
            this.callback = callback;
        }

        #region ================= SERVER CONTROL =================

        public void StartListener()
        {
            Post(StartAsync);
        }

        public void StopListener()
        {
            Post(StopAsync);
        }

        private void StartAsync()
        {
            lock (_lock)
            {
                if (cts != null) return;
                cts = new CancellationTokenSource();
                try
                {
                    bluetoothListener = new BluetoothListener(guid)
                    {
                        ServiceName = serviceName
                    };
                    bluetoothListener.Start();
                }
                catch (Exception ex)
                {
                    try
                    {
                        bluetoothListener?.Stop();
                        bluetoothListener?.Dispose();
                        cts.Cancel();
                    }
                    catch { }
                    bluetoothListener = null;
                    cts.Dispose();
                    cts = null;
                    Post(() => callback?.OnStartServerFailed(ex));
                    return;
                }
                acceptLoopTask = Task.Run(() => AcceptLoop(cts.Token));
                Post(() => callback?.OnStartServerSuccess());
            }
        }

        private void StopAsync()
        {
            Task taskToWait = null;
            lock (_lock)
            {
                if (cts == null) return;
                try
                {
                    cts.Cancel();
                    bluetoothListener?.Stop();
                    bluetoothListener?.Dispose();
                    bluetoothListener = null;
                    taskToWait = acceptLoopTask;
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnStopServerException(ex));
                }
            }
            try { taskToWait?.Wait(); } catch { }
            lock (_lock)
            {
                cts.Dispose();
                cts = null;
                acceptLoopTask = null;
            }
            Post(() => callback?.OnStopServer());
        }

        #endregion

        #region ================= ACCEPT LOOP =================

        private async Task AcceptLoop(CancellationToken token)
        {
            try
            {
                while (!token.IsCancellationRequested)
                {
                    BluetoothClient client = null;

                    try
                    {
                        client = await Task.Run(() =>
                        {
                            return bluetoothListener?.AcceptBluetoothClient();
                        }, token);

                        if (client == null) continue;

                        _ = Task.Run(() => OnClientAccept(client, token));
                    }
                    catch (Exception ex)
                    {
                        Post(() => callback?.OnForeverLoopException(ex));
                    }
                }
            }
            catch (Exception ex)
            {
                Post(() => callback?.OnForeverLoopException(ex));
            }
        }

        private void OnClientAccept(BluetoothClient client, CancellationToken token)
        {
            try
            {
                var clientCallback = callback?.CreateNewClientCallback();
                var c = new Client(client, clientCallback, token);
                Post(() => callback?.OnNewClientConnect(c));
            }
            catch (Exception ex)
            {
                try { client.Close(); client.Dispose(); } catch { }
                Post(() => callback?.OnNewClientException(ex));
            }
        }

        #endregion

        #region ================= SAFE CALLBACK POST =================

        private void Post(Action action)
        {
            if (action == null) return;
            _ = Task.Run(() =>
            {
                try { action(); }
                catch { }
            });
        }

        #endregion

        #region ================= CLIENT =================

        public class Client
        {
            private readonly object _lock = new();
            private readonly BluetoothClient client;
            private readonly BluetoothSocketCallback.ClientCallback callback;
            private readonly CancellationToken token;

            private NetworkStream stream;

            public bool IsAvailable { get; private set; }

            public Client(BluetoothClient client,
                          BluetoothSocketCallback.ClientCallback callback,
                          CancellationToken token)
            {
                this.client = client;
                this.callback = callback;
                this.token = token;
                stream = client.GetStream();
                IsAvailable = true;
                _ = Task.Run(ReceiveLoop);
            }

            private void Post(Action action)
            {
                if (action == null) return;
                _ = Task.Run(() =>
                {
                    try { action(); }
                    catch { }
                });
            }

            public void SendData(byte[] data, int id = -1)
            {
                _ = Task.Run(() => SendInternal(data, id));
            }

            private void SendInternal(byte[] data, int id)
            {
                try
                {
                    stream.Write(IntConverter.ToBigEndian(data.Length), 0, 4);
                    stream.Write(data, 0, data.Length);
                    stream.Flush();
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnSendDataException(ex, id));
                }
            }

            private async Task ReceiveLoop()
            {
                try
                {
                    byte[] sizeBuff = new byte[4];
                    while (!token.IsCancellationRequested && IsAvailable)
                    {
                        int read = await ReadExact(sizeBuff, 4);
                        if (read == 0) break;
                        int size = IntConverter.FromBigEndian(sizeBuff);
                        byte[] data = new byte[size];
                        read = await ReadExact(data, size);
                        if (read == 0) break;
                        Post(() => callback?.OnDataReady(data));
                    }
                }
                catch (Exception ex)
                {
                    Post(() => callback?.OnDataRevException(ex));
                }
                finally
                {
                    Post(DisconnectInternal);
                }
            }

            private async Task<int> ReadExact(byte[] buffer, int size)
            {
                int offset = 0;
                while (offset < size)
                {
                    if (token.IsCancellationRequested) return 0;
                    int read = await stream.ReadAsync(buffer, offset, size - offset);
                    if (read <= 0) return 0;

                    offset += read;
                }
                return offset;
            }

            public void Disconnect()
            {
                Post(DisconnectInternal);
            }

            private void DisconnectInternal()
            {
                lock (_lock)
                {
                    if (!IsAvailable) return;
                    IsAvailable = false;
                }
                try
                {
                    client.Close();
                    client.Dispose();
                }
                catch { }
                Post(() => callback?.OnDisconnect());
                return;
            }
        }

        #endregion
    }

}
