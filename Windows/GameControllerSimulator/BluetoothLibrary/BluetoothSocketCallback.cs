using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace BluetoothLibrary
{
    public interface BluetoothSocketCallback
    {
        void OnStartServerSuccess();
        void OnStartServerFailed(Exception ex);
        void OnStopServer();
        void OnStopServerException(Exception ex);

        void OnForeverLoopException(Exception ex);

        ClientCallback CreateNewClientCallback();
        void OnNewClientConnect(BluetoothSocketServer.Client client);
        void OnNewClientException(Exception ex);


        public interface ClientCallback
        {

            void OnSendDataException(Exception ex, int id = -1);
            void OnDisconnect();
            void OnDataReady(byte[] data);
            void OnDataRevException(Exception ex);

        }
    }

}
