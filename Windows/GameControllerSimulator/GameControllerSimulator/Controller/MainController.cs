using CommonLibrary.Bean;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using LogLibrary;
using SocketCommonLibrary;
using System;

namespace GameControllerSimulator.Controller
{
    public class MainController: IConnectionControllerCallback, IProtocolHandlersFactoryCallback, IVirtualGamepadManagerCallback
    {
        public static readonly string TAG = "MainController";
        private int maxSize;
        private ControllerSlotManager controllerSlotManager;
        private ProtocolHandlersFactory protocolHandlersFactory;
        private VirtualGamepadManager virtualGamepadManager;
        private ConnectionController connectionController;
        private IMainControllerCallback callback;

        public MainController(int maxSize, IMainControllerCallback callback)
        {
            LogUtils.I(TAG, $"Create MainController maxSize = {maxSize}");
            this.maxSize = maxSize;
            this.callback = callback;
            controllerSlotManager = new ControllerSlotManager();
            protocolHandlersFactory = new ProtocolHandlersFactory(this);
            virtualGamepadManager = new VirtualGamepadManager(this);
            connectionController = new ConnectionController(this);
        }

        public void Init()
        {
            LogUtils.I(TAG, "Init");
            controllerSlotManager.Init(maxSize);
            connectionController.Init();
            connectionController.RegisterProtocolHandlers(protocolHandlersFactory.GetProtocolHandlers());
            virtualGamepadManager.Init(maxSize);
        }

        public void Dispose()
        {
            LogUtils.I(TAG, "Dispose");
            connectionController.Destroy();
            virtualGamepadManager.Dispose();
            controllerSlotManager.Clear();
        }

        public void InitRFCOMM(Guid guid, string svcName)
        {
            LogUtils.I(TAG, $"InitRFCOMM guid = {guid}, svcName = ${svcName}");
            connectionController.InitRFCOMM(guid, svcName);
        }

        public void InitTCP(int port)
        {
            LogUtils.I(TAG, $"InitTCP port = {port}");
            connectionController.InitTcp(port);
        }

        public void InitUDP(int port)
        {
            LogUtils.I(TAG, $"InitUDP port = {port}");

        }


        int? IProtocolHandlersFactoryCallback.GetIndex(string guid)
        {
            LogUtils.I(TAG, $"GetIndex guid = {guid}");
            int? res = controllerSlotManager.GetIndex(guid);
            LogUtils.D(TAG, $"GetIndex guid = {guid}, index = {res}");
            return res;
        }

        void IProtocolHandlersFactoryCallback.OnDeviceConnected(int index, DeviceInfo? deviceInfo)
        {
            LogUtils.I(TAG, $"OnDeviceConnected index = {index}");
            virtualGamepadManager.Connect(index);
            callback.OnDeviceInfoReceived(index, deviceInfo);
        }

        void IConnectionControllerCallback.OnFault(string msg, Exception? ex)
        {
            LogUtils.W(TAG, $"OnFault msg = {msg}, ex = {ex?.Message}");
            callback.OnFaulted(msg, ex);
        }

        void IVirtualGamepadManagerCallback.OnFeedbackReceived(int index, FeedbackReceived feedback)
        {
            LogUtils.I(TAG, $"OnFeedbackReceived index = {index}");
            // Todo 发送给客户端feedback数据
            //connectionController.SendData(controllerSlotManager.GetGuid(index), entity)
        }

        void IProtocolHandlersFactoryCallback.OnGameEventReceive(int index, byte[] events)
        {
            LogUtils.I(TAG, $"OnGameEventReceive index = {index}");
            virtualGamepadManager.UpdateGameEvent(index, events);
            callback.OnGameEventReceive(index, events);
        }

        void IConnectionControllerCallback.OnSessionAvailable(string guid)
        {
            LogUtils.I(TAG, $"OnSessionAvailable guid = {guid}");
            int? index = controllerSlotManager.Allocate(guid);
            if (index == null)
            {
                LogUtils.W(TAG, $"Allocate slot failed. guid = {guid}, prepare reject session.");
                connectionController.RejectSession(guid);
                return;
            }
            bool success = virtualGamepadManager.Create(index.Value);
            if (success)
            {
                LogUtils.D(TAG, $"Create Game Controller success. guid = {guid}, index = {index}");
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
                LogUtils.W(TAG, $"Create Game Controller failed. guid = {guid}, index = {index}. prepare release guid and reject session.");
                controllerSlotManager.Release(guid);
                connectionController.RejectSession(guid);
            }
        }

        void IConnectionControllerCallback.OnSessionUnAvailable(string guid)
        {
            LogUtils.I(TAG, $"OnSessionUnAvailable guid = {guid}");
            int? index = controllerSlotManager.GetIndex(guid);
            if (index == null)
            {
                LogUtils.E(TAG, $"get index is null. guid = {guid}");
                return;
            }
            LogUtils.D(TAG, $"prepare release guid = {guid}, index = {index}");
            controllerSlotManager.Release(guid);
            virtualGamepadManager.Dispose(index.Value);
            callback.OnSessionAvailableChange(index.Value, false);
        }

        void IConnectionControllerCallback.OnStartServer(ConnectionType type)
        {
            LogUtils.I(TAG, $"OnStartServer type = {type}");
            callback.OnConnectionAvaiableChange(true, type);
        }

        void IConnectionControllerCallback.OnStopServer(ConnectionType type)
        {
            LogUtils.I(TAG, $"OnStopServer type = {type}");
            callback.OnConnectionAvaiableChange(false, type);
        }
    }

    public interface IMainControllerCallback
    {
        void OnDeviceInfoReceived(int index, DeviceInfo? deviceInfo);
        void OnSessionAvailableChange(int index, bool available);
        void OnConnectionAvaiableChange(bool available, ConnectionType type);
        void OnGameEventReceive(int index, byte[] events);
        void OnFaulted(string msg, Exception? ex);
    }

}
