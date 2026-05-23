using CommonLibrary.Bean;
using GameControllerSimulator.Bean;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace GameControllerSimulator.Constant
{
    public static class EntityType
    {
        public const int TYPE_REQUEST_CLIENT_ID = 1;
        public const int TYPE_REQUEST_CLIENT_ID_RESULT = 2;
        public const int TYPE_UNREGISTER_CLIENT_ID = 3;
        public const int TYPE_UNREGISTER_CLIENT_ID_RESULT = 4;
        public const int TYPE_SEND_GAME_EVENT = 5;
        public const int TYPE_SEND_GAME_EVENT_RESULT = 6;
        public const int TYPE_FEEDBACK_RECEIVED = 7;
        public const int TYPE_FEEDBACK_RECEIVED_RESULT = 8;
        public const int TYPE_QUERY_CLIENT_INFO = 9;
        public const int TYPE_QUERY_CLIENT_INFO_RESULT = 10;
        public const int TYPE_NEW_TYPE_CONNECT = 11;
        public const int TYPE_NEW_TYPE_CONNECT_RESULT = 12;

        public static readonly Dictionary<int, Type> TYPE_MAPPING = new()
    {
        { TYPE_REQUEST_CLIENT_ID, typeof(object) },
        { TYPE_REQUEST_CLIENT_ID_RESULT, typeof(string) },
        { TYPE_UNREGISTER_CLIENT_ID, typeof(string) },
        { TYPE_UNREGISTER_CLIENT_ID_RESULT, typeof(object) },
        { TYPE_SEND_GAME_EVENT, typeof(string) },
        { TYPE_SEND_GAME_EVENT_RESULT, typeof(object) },
        { TYPE_FEEDBACK_RECEIVED, typeof(FeedbackReceived) },
        { TYPE_FEEDBACK_RECEIVED_RESULT, typeof(object) },
        { TYPE_QUERY_CLIENT_INFO, typeof(object) },
        { TYPE_QUERY_CLIENT_INFO_RESULT, typeof(DeviceInfo) },
        { TYPE_NEW_TYPE_CONNECT, typeof(string) },
        { TYPE_NEW_TYPE_CONNECT_RESULT, typeof(string) },
    };
    }
}
