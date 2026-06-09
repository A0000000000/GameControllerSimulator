using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using LogLibrary;
using System;
using System.Collections.Generic;

namespace GameControllerSimulator.Controller
{
    public class ProtocolHandlersFactory
    {
        public static readonly string TAG = "ProtocolHandlersFactory";

        private IProtocolHandlersFactoryCallback callback;
        private object gameEventLock = new();

        public ProtocolHandlersFactory(IProtocolHandlersFactoryCallback callback)
        {
            this.callback = callback;
        }

        public List<ConnectionProtocolHandler> GetProtocolHandlers()
        {
            return [
                new ConnectionProtocolHandler()
                {
                    Id = EntityId.GAMEPAD_PAGE_EVENT,
                    Type = EntityType.TYPE_QUERY_CLIENT_INFO_RESULT,
                    Handler = (entity, guid) =>
                    {
                        LogUtils.I(TAG, $"Received query client info result for GUID: {guid}");
                        if (guid == "")
                        {
                            LogUtils.W(TAG, "Invalid GUID received");
                            return;
                        }
                        int? index = callback.GetIndex(guid);
                        if (index == null)
                        {
                            LogUtils.W(TAG, "GUID not found in any slot");
                            return;
                        }
                        LogUtils.I(TAG, $"Client info ({guid}) received for index: {index.Value}");
                        callback.OnDeviceConnected(index.Value, entity.Data as DeviceInfo);
                    }
                },
                new ConnectionProtocolHandler()
                {
                    Id = EntityId.GAMEPAD_PAGE_EVENT,
                    Type = EntityType.TYPE_SEND_GAME_EVENT,
                    Handler = (entity, guid) =>
                    {
                        LogUtils.I(TAG, $"Received game event for GUID: {guid}");
                        if (guid == "")
                        {
                            LogUtils.W(TAG, "Invalid GUID received");
                            return;
                        }
                        int? index = callback.GetIndex(guid);
                        if (index == null)
                        {
                            LogUtils.W(TAG, "GUID not found in any slot");
                            return;
                        }
                        LogUtils.I(TAG, $"Game event received for index: {index.Value}");
                        lock (gameEventLock)
                        {
                            string? eventsBase64 = entity.Data as string;
                            if (eventsBase64 != null)
                            {
                                byte[] events = Convert.FromBase64String(eventsBase64);
                                callback.OnGameEventReceive(index.Value, events);
                            }
                        }
                    }
                },

            ];
        }
    }

    public interface IProtocolHandlersFactoryCallback
    {
        int? GetIndex(string guid);
        void OnDeviceConnected(int index, DeviceInfo? deviceInfo);
        void OnGameEventReceive(int index, byte[] events);
    }

}
