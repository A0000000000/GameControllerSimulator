using AsyncTaskLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Linq;

namespace GameControllerSimulator.Connection
{
    public class SessionManager
    {
        private Dictionary<string, List<IServerTransport.IClientTransport>> sessions = new();
        private Dictionary<IServerTransport.IClientTransport, string> sessionMapping = new();
        private Dictionary<string, ConnectionType> currentType = new();
        private ISessionManagerCallback callback;

        public SessionManager(ISessionManagerCallback callback)
        {
            this.callback = callback;
        }

        public string RegisterClient(IServerTransport.IClientTransport clientTransport)
        {
            Guid guid = Guid.NewGuid();
            sessions[guid.ToString()] = [clientTransport];
            sessionMapping[clientTransport] = guid.ToString();
            currentType[guid.ToString()] = clientTransport.ConnectionType;
            AsyncTaskUtils.Post(() =>
            {
                callback.OnSessionAvailable(guid.ToString());
            });
            return guid.ToString();
        }

        public bool AddNewClient(IServerTransport.IClientTransport clientTransport, string guid)
        {
            if (sessions.ContainsKey(guid))
            {
                sessions[guid].Add(clientTransport);
                sessionMapping[clientTransport] = guid;
                return true;
            }
            return false;
        }

        public string RemoveClient(IServerTransport.IClientTransport clientTransport)
        {
            if (sessionMapping.ContainsKey(clientTransport))
            {
                string guid = sessionMapping[clientTransport];
                sessions[guid].Remove(clientTransport);
                sessionMapping.Remove(clientTransport);
                if (sessions[guid].Count == 0)
                {
                    sessions.Remove(guid);
                    currentType.Remove(guid);
                    AsyncTaskUtils.Post(() =>
                    {
                        callback.OnSessionUnAvailable(guid);
                    });
                }
                else
                {
                    if (sessions[guid].Find(c => c.ConnectionType == currentType[guid]) == null)
                    {
                        currentType[guid] = sessions[guid][0].ConnectionType;
                    }
                }
                return guid;
            }
            return "";
        }

        public IServerTransport.IClientTransport? GetClientByGuid(string guid)
        {
            if (sessions.ContainsKey(guid) && sessions[guid].Count > 0)
            {
                if (currentType.ContainsKey(guid))
                {
                    ConnectionType type = currentType[guid];
                    IServerTransport.IClientTransport? client = sessions[guid].FirstOrDefault(c => c.ConnectionType == type);
                    if (client != null)
                    {
                        return client;
                    }
                    else
                    {
                        return sessions[guid][0];
                    }
                }
                else
                {
                    return sessions[guid][0];
                }
            }
            return null;
        }

        public void ChangeCurrentType(IServerTransport.IClientTransport transport, ConnectionType type)
        {
            if (sessionMapping.ContainsKey(transport) && currentType.ContainsKey(sessionMapping[transport]))
            {
                currentType[sessionMapping[transport]] = type;
            }
        }

        public string GetGuidByClient(IServerTransport.IClientTransport client)
        {
            if (sessionMapping.ContainsKey(client))
            {
                return sessionMapping[client];
            }
            return "";
        }

        public void Destroy()
        {
            sessions.Clear();
            sessionMapping.Clear();
        }

        public List<ProtocolHandler> GetSessionProtocolHandlers()
        {
            return [
                new ProtocolHandler()
                {
                    Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    Type = EntityType.TYPE_REQUEST_CLIENT_ID,
                    Handler = (entity, client) =>
                    {
                        string guid = RegisterClient(client);
                        client.SendData(ProtocolRouter.Encode(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_REQUEST_CLIENT_ID_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = guid
                        }));
                    }
                },
                new ProtocolHandler()
                {
                    Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    Type = EntityType.TYPE_UNREGISTER_CLIENT_ID,
                    Handler = (entity, client) =>
                    {
                        RemoveClient(client);
                        client.Disconnect();
                    }
                },
                new ProtocolHandler()
                {
                    Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    Type = EntityType.TYPE_NEW_TYPE_CONNECT,
                    Handler = (entity, client) =>
                    {
                        string guid = entity.Data?.ToString() ?? "";
                        bool result = AddNewClient(client, guid);
                        client.SendData(ProtocolRouter.Encode(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_NEW_TYPE_CONNECT_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = result ? "success" : "failed"
                        }));
                    }
                },
                new ProtocolHandler()
                {
                    Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    Type = EntityType.TYPE_ECHO,
                    Handler = (entity, client) =>
                    {
                        client.SendData(ProtocolRouter.Encode(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_ECHO_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = entity.Data?.ToString() ?? ""
                        }));
                    }
                },
                new ProtocolHandler()
                {
                    Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    Type = EntityType.TYPE_RTT,
                    Handler = (entity, client) =>
                    {
                        client.SendData(ProtocolRouter.Encode(new BaseEntity<string>()
                        {
                            Type = EntityType.TYPE_RTT_RESULT,
                            Id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                            Data = entity.Data?.ToString() ?? entity.Timestamp.ToString()
                        }));
                    }
                }
            ];
        }
    }


    public interface ISessionManagerCallback
    {
        void OnSessionAvailable(string guid);
        void OnSessionUnAvailable(string guid);
    }

}