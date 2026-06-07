using System;

namespace SocketCommonLibrary
{
    public interface SocketServerCallback<TServerSocket, TSocket> where TServerSocket: class, IDisposable where TSocket : class, IDisposable
    {

        void OnStartServerSuccess();
        void OnStartServerFailed(Exception ex);
        void OnStopServer();
        void OnStopServerException(Exception ex);

        void OnForeverLoopException(Exception ex);

        SocketClientCallback<TSocket> CreateNewClientCallback();
        void OnClientConnect(SocketClient<TSocket> client);
        void OnNewClientException(Exception ex);

        void OnTaskException(Exception ex);

    }

    public interface SocketClientCallback<TSocket> where TSocket : class, IDisposable
    {
        void OnSendDataException(SocketClient<TSocket> client, Exception ex, int id = -1);
        void OnDisconnect(SocketClient<TSocket> client);
        void OnDataReady(SocketClient<TSocket> client, byte[] data);
        void OnDataRevException(SocketClient<TSocket> client, Exception ex);
        void OnTaskException(Exception ex);
    }

}
