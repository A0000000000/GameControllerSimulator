using AsyncTaskLibrary;
using GameControllerSimulator.Bean;
using GameControllerSimulator.Constant;
using LogLibrary;
using SocketCommonLibrary;
using System;
using System.Collections.Generic;
using System.Linq;

namespace GameControllerSimulator.Connection
{
    class ControllerSession
    {
        public string Guid { get; init; } = "";
        public Dictionary<ConnectionType, IServerTransport.IClientTransport> Connections { get; } = new();
        public ConnectionType CurrentType { get; set; }
    }


    public class SessionManager
    {
        public static readonly string TAG = "SessionManager";
        private Dictionary<string, ControllerSession> sessions = new();
        private Dictionary<IServerTransport.IClientTransport, string> sessionMappings = new();
        private ISessionManagerCallback callback;

        public SessionManager(ISessionManagerCallback callback)
        {
            this.callback = callback;
        }

        public string RegisterClient(IServerTransport.IClientTransport clientTransport)
        {
            lock (sessions)
            {
                if (sessionMappings.ContainsKey(clientTransport))
                {
                    LogUtils.W(TAG, "Client already registered, return existing guid");
                    return sessionMappings[clientTransport];
                }
                Guid guid = Guid.NewGuid();
                sessions[guid.ToString()] = new ControllerSession()
                {
                    Guid = guid.ToString(),
                    CurrentType = clientTransport.ConnectionType
                };
                sessions[guid.ToString()].Connections.Add(clientTransport.ConnectionType, clientTransport);
                sessionMappings.Add(clientTransport, guid.ToString());
                AsyncTaskUtils.Post(() =>
                {
                    callback.OnSessionAvailable(guid.ToString());
                });
                return guid.ToString();
            }
        }

        public bool AddNewClient(IServerTransport.IClientTransport clientTransport, string guid)
        {
            lock (sessions)
            {
                if (sessions.ContainsKey(guid))
                {
                    if (sessionMappings.ContainsKey(clientTransport))
                    {
                        LogUtils.W(TAG, "Client already registered, cannot add new client");
                        return false;
                    }
                    else
                    {
                        if (sessions[guid].Connections.ContainsKey(clientTransport.ConnectionType))
                        {
                            if (sessionMappings.ContainsKey(sessions[guid].Connections[clientTransport.ConnectionType]))
                            {
                                sessionMappings.Remove(sessions[guid].Connections[clientTransport.ConnectionType]);
                            }
                            sessions[guid].Connections[clientTransport.ConnectionType] = clientTransport;
                            sessionMappings.Add(clientTransport, guid);
                            LogUtils.W(TAG, $"Session with guid {guid} already has connection with type {clientTransport.ConnectionType}, replace new client");
                        }
                        else
                        {
                            sessions[guid].Connections.Add(clientTransport.ConnectionType, clientTransport);
                            sessionMappings.Add(clientTransport, guid);
                        }
                        return true;
                    }
                }
                else
                {
                    LogUtils.W(TAG, $"Session with guid {guid} not found, cannot add new client");
                }
                return false;
            }
        }

        public string RemoveClient(IServerTransport.IClientTransport clientTransport)
        {
            lock(sessions)
            {
                if (sessionMappings.ContainsKey(clientTransport))
                {
                    string guid = sessionMappings[clientTransport];
                    IServerTransport.IClientTransport? clientToRemove = null;
                    if (sessions.ContainsKey(guid))
                    {
                        if (sessions[guid].Connections.TryGetValue(clientTransport.ConnectionType, out var current) && current == clientTransport)
                        {
                            sessions[guid].Connections.Remove(clientTransport.ConnectionType);
                        }
                        if (sessions[guid].Connections.Count == 1 && sessions[guid].Connections.ContainsKey(ConnectionType.UDP))
                        {
                            // 只剩下UDP, 视为Session不可用
                            clientToRemove = sessions[guid].Connections[ConnectionType.UDP];
                            sessionMappings.Remove(sessions[guid].Connections[ConnectionType.UDP]);
                            sessions[guid].Connections.Remove(ConnectionType.UDP);
                        }
                        if (sessions[guid].Connections.Count == 0)
                        {
                            sessions.Remove(guid);
                            AsyncTaskUtils.Post(() =>
                            {
                                callback.OnSessionUnAvailable(guid);
                            });
                        }
                        else
                        {
                            if (clientTransport.ConnectionType == sessions[guid].CurrentType)
                            {
                                var transport = sessions[guid].Connections.Values.FirstOrDefault();
                                if (transport != null)
                                {
                                    sessions[guid].CurrentType = transport.ConnectionType;
                                }
                            }
                        }
                    }
                    sessionMappings.Remove(clientTransport);
                    AsyncTaskUtils.Post(() => clientToRemove?.Disconnect());
                    return guid;
                }
                else
                {
                    LogUtils.W(TAG, "Client not registered, cannot remove client");
                }
                return "";
            }
        }

        public IServerTransport.IClientTransport? GetCurrentClientByGuid(string guid)
        {
            lock (sessions)
            {
                if (sessions.ContainsKey(guid))
                {
                    if (sessions[guid].Connections.ContainsKey(sessions[guid].CurrentType))
                    {
                        return sessions[guid].Connections[sessions[guid].CurrentType];
                    }
                    else
                    {
                        return sessions[guid].Connections.Values.FirstOrDefault();
                    }
                }
                return null;
            }
        }

        public List<IServerTransport.IClientTransport> GetClientsByGuid(string guid)
        {
            lock (sessions)
            {
                if (sessions.ContainsKey(guid))
                {
                    return sessions[guid].Connections.Values.ToList();
                }
                return new List<IServerTransport.IClientTransport>();
            }
        }

        public void RemoveSession(string guid)
        {
            List<IServerTransport.IClientTransport> clientsToRemove = new();
            lock (sessions)
            {
                if (sessions.ContainsKey(guid))
                {
                    foreach (var client in sessions[guid].Connections.Values)
                    {
                        sessionMappings.Remove(client);
                        clientsToRemove.Add(client);
                    }
                    sessions.Remove(guid);
                    AsyncTaskUtils.Post(() =>
                    {
                        callback.OnSessionUnAvailable(guid);
                    });
                }
                else
                {
                    LogUtils.W(TAG, $"Session with guid {guid} not found, cannot remove session");
                }
            }
            foreach (var item in clientsToRemove)
            {
                item.Disconnect();
            }
        }

        public void ChangeCurrentType(IServerTransport.IClientTransport transport)
        {
            lock (sessions)
            {
                if (!sessionMappings.ContainsKey(transport))
                {
                    LogUtils.W(TAG, "Client not registered, cannot change current type");
                    return;
                }
                string guid = sessionMappings[transport];
                sessions[guid].CurrentType = transport.ConnectionType;
            }
        }

        public string GetGuidByClient(IServerTransport.IClientTransport client)
        {
            lock (sessions)
            {
                if (sessionMappings.ContainsKey(client))
                {
                    return sessionMappings[client];
                }
                return "";
            }
        }

        public void Destroy()
        {
            lock (sessions)
            {
                sessions.Clear();
                sessionMappings.Clear();
            }
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
                        if (client.ConnectionType == ConnectionType.UDP)
                        {
                            // UDP 不允许注册
                            client.Disconnect();
                            return;
                        }
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