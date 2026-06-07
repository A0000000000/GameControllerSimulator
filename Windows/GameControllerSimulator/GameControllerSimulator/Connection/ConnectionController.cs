using GameControllerSimulator.Bean;
using LogLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;

namespace GameControllerSimulator.Connection
{
    public class ConnectionController : ITransportManagerCallback, ISessionManagerCallback
    {
        public static readonly string TAG = "ConnectionController";
        private TransportManager transportManager;
        private SessionManager sessionManager;
        private ProtocolRouter protocolRouter;
        private IConnectionControllerCallback callback;

        public ConnectionController(IConnectionControllerCallback callback)
        {
            this.callback = callback;
            transportManager = new TransportManager(this);
            sessionManager = new SessionManager(this);
            protocolRouter = new ProtocolRouter();
        }

        public void Init()
        {
            protocolRouter.RegisterHandler(sessionManager.GetSessionProtocolHandlers());
        }

        public void InitRFCOMM(Guid rfcommGuid, string svcName)
        {
            transportManager.InitRFCOMM(rfcommGuid, svcName);
        }

        public void InitTcp(int port)
        {
            transportManager.InitTcp(port);
        }

        public void SendData(string? guid, IBaseEntity entity)
        {
            if (guid == null)
            {
                LogUtils.E(TAG, $"Guid is null, cannot send data: {entity}");
                return;
            }
            IServerTransport.IClientTransport? client = sessionManager.GetClientByGuid(guid);
            if (client == null)
            {
                LogUtils.E(TAG, $"No client found for guid: {guid}");
            }
            client?.SendData(ProtocolRouter.Encode(entity));
        }

        public string GetGuidByClient(IServerTransport.IClientTransport client)
        {
            return sessionManager.GetGuidByClient(client);
        }

        public void RegisterProtocolHandlers(List<ProtocolHandler> handlers)
        {
            protocolRouter.RegisterHandler(handlers);
        }

        public void Destroy()
        {
            protocolRouter.Clear();
            sessionManager.Destroy();
            transportManager.Destroy();
        }

        #region 回调

        void ITransportManagerCallback.OnFault(string tag, string msg, ConnectionType type, Exception? ex)
        {
            callback.OnFault(tag, msg, type, ex);
        }

        void ITransportManagerCallback.OnStartServer(ConnectionType type)
        {
            callback.OnStartServer(type);
        }

        void ITransportManagerCallback.OnStopServer(ConnectionType type)
        {
            callback.OnStopServer(type);
        }

        void ITransportManagerCallback.OnDataReady(IServerTransport.IClientTransport client, byte[] data, ConnectionType type)
        {
            sessionManager.ChangeCurrentType(client, type);
            protocolRouter.DispatchData(client, data);
        }

        void ITransportManagerCallback.OnDisconnect(IServerTransport.IClientTransport client, ConnectionType type)
        {
            sessionManager.RemoveClient(client);
        }

        void ISessionManagerCallback.OnSessionAvailable(string guid)
        {
            callback.OnSessionAvailable(guid);
        }

        void ISessionManagerCallback.OnSessionUnAvailable(string guid)
        {
            callback.OnSessionUnAvailable(guid);
        }

        #endregion
    }


    public interface IConnectionControllerCallback
    {
        void OnFault(string tag, string msg, ConnectionType type, Exception? ex);
        void OnStartServer(ConnectionType type);
        void OnStopServer(ConnectionType type);
        void OnSessionAvailable(string guid);
        void OnSessionUnAvailable(string guid);

    }
}
