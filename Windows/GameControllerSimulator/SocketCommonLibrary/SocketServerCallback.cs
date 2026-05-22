using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;

namespace SocketCommonLibrary
{
    public interface SocketServerCallback<TServerSocket, TSocket> where TServerSocket: class, IDisposable where TSocket : class, IDisposable
    {

        void OnStartServerSuccess();
        void OnStartServerFailed(Exception ex);
        void OnStopServer();
        void OnStopServerException(Exception ex);

        void OnForeverLoopException(Exception ex);

        ClientCallback<TSocket> CreateNewClientCallback();
        void OnNewClientConnect(Client<TSocket> client);
        void OnNewClientException(Exception ex);

        void OnTaskException(Exception ex);

    }

    public interface ClientCallback<TSocket> where TSocket : class, IDisposable
    {
        void OnSendDataException(Client<TSocket> client, Exception ex, int id = -1);
        void OnDisconnect(Client<TSocket> client);
        void OnDataReady(Client<TSocket> client, byte[] data);
        void OnDataRevException(Client<TSocket> client, Exception ex);
        void OnTaskException(Exception ex);
    }

}
