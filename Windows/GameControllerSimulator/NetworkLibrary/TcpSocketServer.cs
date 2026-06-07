using SocketCommonLibrary;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace NetworkLibrary
{
    public class TcpSocketServer : SocketServer<TcpListener, TcpClient>
    {
        public override ConnectionType ConnectionType => ConnectionType.TCP;
        private int port;
        public TcpSocketServer(int port, SocketServerCallback<TcpListener, TcpClient> callback) : base(callback)
        {
            this.port = port;
        }

        protected override async Task<TcpClient> AcceptSocket(TcpListener serverSocker)
        {
            return await serverSocker.AcceptTcpClientAsync();
        }

        protected override void Close(TcpListener serverSocker)
        {
            serverSocker.Stop();
            serverSocker.Dispose();
        }

        protected override SocketClient<TcpClient> CreateAcceptClient(TcpClient socket, SocketClientCallback<TcpClient> clientCallback)
        {
            return new TcpSocketClient(socket, clientCallback);
        }

        protected override TcpListener CreateServerSocket()
        {
            TcpListener listener = new TcpListener(IPAddress.Any, port);
            listener.Start();
            return listener;
        }
    }

    public class TcpSocketClient : SocketClient<TcpClient>
    {
        public override ConnectionType ConnectionType => ConnectionType.TCP;
        public TcpSocketClient(TcpClient socket, SocketClientCallback<TcpClient> callback) : base(socket, callback)
        {
        }

        protected override void Close(TcpClient socket)
        {
            socket.Close();
            socket.Dispose();
        }

        protected override Stream GetStream(TcpClient socket)
        {
            return socket.GetStream();
        }
    }
}
