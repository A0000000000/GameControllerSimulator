using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace GameControllerSimulator.Bean
{
    public interface IBaseEntity
    {
        [JsonPropertyName("type")]
        int Type { get; }
        [JsonPropertyName("id")]
        int Id { get; }
        [JsonPropertyName("timestamp")]
        long Timestamp { get; }

        [JsonPropertyName("data")]

        object? Data { get; }
    }

    public class BaseEntity<T> : IBaseEntity where T : class
    {
        [JsonPropertyName("type")]
        public int Type
        {
            get;
            set;
        }

        [JsonPropertyName("id")]
        public int Id
        {
            get;
            set;
        }

        [JsonPropertyName("timestamp")]
        public long Timestamp
        {
            get;
            set;
        }

        [JsonPropertyName("data")]
        public T? Data
        {
            get;
            set;
        }
        object? IBaseEntity.Data { get => Data; }
    }
}
