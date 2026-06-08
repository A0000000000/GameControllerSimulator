using GameControllerSimulator.Bean;
using GameControllerSimulator.Connection;
using GameControllerSimulator.Constant;
using System;
using System.Collections.Generic;

namespace GameControllerSimulator.Controller
{
    public class ProtocolHandlersFactory
    {

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
                        if (guid == "")
                        {
                            return;
                        }
                        int? index = callback.GetIndex(guid);
                        if (index == null)
                        {
                            return;
                        }
                        callback.OnDeviceConnected(index.Value, entity.Data as DeviceInfo);
                    }
                },
                new ConnectionProtocolHandler()
                {
                    Id = EntityId.GAMEPAD_PAGE_EVENT,
                    Type = EntityType.TYPE_SEND_GAME_EVENT,
                    Handler = (entity, guid) =>
                    {
                        if (guid == "")
                        {
                            return;
                        }
                        int? index = callback.GetIndex(guid);
                        if (index == null)
                        {
                            return;
                        }
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
