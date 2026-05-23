using GameControllerSimulator.Bean;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameControllerSimulator.Connection
{
    public interface IConnectionManagerCallback
    {
        void OnManagerAvaiable();
        void OnManagerUnavailable();

        void OnFaulted(string msg, Exception ex);

        void OnDataReady(int index, IBaseEntity data);

        Type GetTypeClass(int type);

        void OnNewClientConnection(int index);
        void OnClientDisconnected(int index);

    }
}
