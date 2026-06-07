using AsyncTaskLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;

namespace GameControllerSimulator.Connection
{
    public class ProtocolRouter
    {
        private Dictionary<int, Dictionary<int, ProtocolHandler>> handlers = new();

        public static IBaseEntity? Dncode(byte[] data)
        {
            string jsonStr = Encoding.UTF8.GetString(data);
            using JsonDocument doc = JsonDocument.Parse(jsonStr);
            JsonElement root = doc.RootElement;
            int elementType = -1;
            if (root.TryGetProperty("type", out JsonElement t))
            {
                elementType = t.ValueKind == JsonValueKind.Number ? t.GetInt32() : -1;
            }
            Type dataType = elementType == -1 ? typeof(JsonElement) : (EntityType.TYPE_MAPPING.ContainsKey(elementType) ? EntityType.TYPE_MAPPING[elementType] : typeof(JsonElement));
            Type entityType = typeof(BaseEntity<>).MakeGenericType(dataType);
            object? entity = JsonSerializer.Deserialize(jsonStr, entityType);
            return entity as IBaseEntity;
        }

        public static byte[] Encode(IBaseEntity data)
        {
            return Encoding.UTF8.GetBytes(JsonSerializer.Serialize(data));
        }

        public void RegisterHandler(List<ProtocolHandler> hds)
        {
            lock (this)
            {
                if (hds != null && hds.Count > 0)
                {
                    foreach (ProtocolHandler handler in hds)
                    {
                        if (!handlers.ContainsKey(handler.Id))
                        {
                            handlers[handler.Id] = new();
                        }
                        handlers[handler.Id][handler.Type] = handler;
                    }
                }
            }
        }

        public void UnregisterHandler(List<ProtocolHandler> hds)
        {
            lock (this)
            {
                if (hds != null && hds.Count > 0)
                {
                    foreach (ProtocolHandler handler in hds)
                    {
                        if (handlers.ContainsKey(handler.Id) && handlers[handler.Id].ContainsKey(handler.Type))
                        {
                            handlers[handler.Id].Remove(handler.Type);
                        }
                        if (handlers.ContainsKey(handler.Id) && handlers[handler.Id].Count == 0)
                        {
                            handlers.Remove(handler.Id);
                        }
                    }
                }
            }
        }

        public void Clear()
        {
            lock (this)
            {
                handlers.Clear();
            }
        }

        public void DispatchData(IServerTransport.IClientTransport client, byte[] data)
        {
            _ = AsyncTaskUtils.Post(() =>
            {
                IBaseEntity? entity = Dncode(data);
                if (entity != null)
                {
                    Dictionary<int, ProtocolHandler> ids = handlers[entity.Id];
                    if (ids != null)
                    {
                        ProtocolHandler handler = ids[entity.Type];
                        handler?.Handler?.Invoke(entity, client);
                    }
                }
            });
        }
    }

    public class ProtocolHandler
    {
        public int Id { get; set; }
        public int Type { get; set; }
        public Action<IBaseEntity, IServerTransport.IClientTransport>? Handler { get; set; }

    }

}
