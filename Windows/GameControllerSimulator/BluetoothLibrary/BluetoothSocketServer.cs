using InTheHand.Net.Sockets;
using SocketCommonLibrary;
using System;
using System.IO;
using System.Threading.Tasks;

namespace BluetoothLibrary
{
    public class BluetoothSocketServer: SocketServer<BluetoothListener, BluetoothClient>
    {

        public Guid guid { get; private set; }
        public string serviceName { get; private set; }

        public override ConnectionType ConnectionType => ConnectionType.BLE;

        public BluetoothSocketServer(Guid guid, string serviceName, SocketServerCallback<BluetoothListener, BluetoothClient> callback): base(callback)
        {
            this.guid = guid;
            this.serviceName = serviceName;
        }

        protected override BluetoothListener CreateServerSocket()
        {
            BluetoothListener listener = new BluetoothListener(guid)
            {
                ServiceName = serviceName
            };
            listener.Start();
            return listener;
        }

        protected override async Task<BluetoothClient> AcceptSocket(BluetoothListener serverSocker)
        {
            return await serverSocker.AcceptBluetoothClientAsync();
        }

        protected override SocketClient<BluetoothClient> CreateAcceptClient(BluetoothClient socket, SocketClientCallback<BluetoothClient> clientCallback)
        {
            return new BluetoothSocketClient(socket, clientCallback);
        }

        protected override void Close(BluetoothListener serverSocker)
        {
            serverSocker.Stop();
            serverSocker.Dispose();
        }
    }


    public class BluetoothSocketClient: SocketClient<BluetoothClient>
    {
        public override ConnectionType ConnectionType => ConnectionType.BLE;

        public BluetoothSocketClient(BluetoothClient socket, SocketClientCallback<BluetoothClient> callback): base(socket, callback)
        { }

        protected override void Close(BluetoothClient socket)
        {
            socket.Close();
            socket.Dispose();
        }

        protected override Stream GetStream(BluetoothClient socket)
        {
            return socket.GetStream();
        }
    }

}
