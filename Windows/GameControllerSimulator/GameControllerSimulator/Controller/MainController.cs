using CommonLibrary.Bean;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using SocketCommonLibrary;
using System;

namespace GameControllerSimulator.Controller
{
    public class MainController: IConnectionControllerCallback, IProtocolHandlersFactoryCallback, IVirtualGamepadManagerCallback
    {
        private int maxSize;
        private ControllerSlotManager controllerSlotManager;
        private ProtocolHandlersFactory protocolHandlersFactory;
        private VirtualGamepadManager virtualGamepadManager;
        private ConnectionController connectionController;
        private IMainControllerCallback callback;

        public MainController(int maxSize, IMainControllerCallback callback)
        {
            this.maxSize = maxSize;
            this.callback = callback;
            controllerSlotManager = new ControllerSlotManager();
            protocolHandlersFactory = new ProtocolHandlersFactory(this);
            virtualGamepadManager = new VirtualGamepadManager(this);
            connectionController = new ConnectionController(this);
        }

        public void Init()
        {
            controllerSlotManager.Init(maxSize);
            connectionController.Init();
            connectionController.RegisterProtocolHandlers(protocolHandlersFactory.GetProtocolHandlers());
            virtualGamepadManager.Init(maxSize);
        }

        public void Dispose()
        {
            connectionController.Destroy();
            virtualGamepadManager.Dispose();
            controllerSlotManager.Clear();
        }

        public void InitRFCOMM(Guid guid, string svcName)
        {
            connectionController.InitRFCOMM(guid, svcName);
        }

        public void InitTCP(int port)
        {
            connectionController.InitTcp(port);
        }

        public void InitUDP(int port)
        {
            
        }


        int? IProtocolHandlersFactoryCallback.GetIndex(string guid)
        {
            return controllerSlotManager.GetIndex(guid);
        }

        void IProtocolHandlersFactoryCallback.OnDeviceConnected(int index, DeviceInfo? deviceInfo)
        {
            virtualGamepadManager.Connect(index);
            callback.OnDeviceInfoReceived(index, deviceInfo);
        }

        void IConnectionControllerCallback.OnFault(string tag, string msg, ConnectionType type, Exception? ex)
        {
            callback.OnFaulted(tag, msg, type, ex);
        }

        void IVirtualGamepadManagerCallback.OnFeedbackReceived(int index, FeedbackReceived feedback)
        {
            // Todo 发送给客户端feedback数据
            //connectionController.SendData(controllerSlotManager.GetGuid(index), entity)
        }

        void IProtocolHandlersFactoryCallback.OnGameEventReceive(int index, byte[] events)
        {
            virtualGamepadManager.UpdateGameEvent(index, events);
            callback.OnGameEventReceive(index, events);
        }

        void IConnectionControllerCallback.OnSessionAvailable(string guid)
        {
            int? index = controllerSlotManager.Allocate(guid);
            if (index == null)
            {
                connectionController.RejectSession(guid);
                return;
            }
            bool success = virtualGamepadManager.Create(index.Value);
            if (success)
            {
                callback.OnSessionAvailableChange(index.Value, true);
                connectionController?.SendData(guid, new BaseEntity<object>
                {
                    Type = EntityType.TYPE_QUERY_CLIENT_INFO,
                    Id = EntityId.GAMEPAD_PAGE_EVENT,
                    Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    Data = null
                });
            }
            else
            {
                controllerSlotManager.Release(guid);
                connectionController.RejectSession(guid);
            }
        }

        void IConnectionControllerCallback.OnSessionUnAvailable(string guid)
        {
            int? index = controllerSlotManager.GetIndex(guid);
            if (index == null)
            {
                return;
            }
            controllerSlotManager.Release(guid);
            virtualGamepadManager.Dispose(index.Value);
            callback.OnSessionAvailableChange(index.Value, false);
        }

        void IConnectionControllerCallback.OnStartServer(ConnectionType type)
        {
            callback.OnConnectionAvaiableChange(true, type);
        }

        void IConnectionControllerCallback.OnStopServer(ConnectionType type)
        {
            callback.OnConnectionAvaiableChange(false, type);
        }
    }

    public interface IMainControllerCallback
    {
        void OnDeviceInfoReceived(int index, DeviceInfo? deviceInfo);
        void OnSessionAvailableChange(int index, bool available);
        void OnConnectionAvaiableChange(bool available, ConnectionType type);
        void OnGameEventReceive(int index, byte[] events);
        void OnFaulted(string tag, string msg, ConnectionType type, Exception? ex);
    }

}
