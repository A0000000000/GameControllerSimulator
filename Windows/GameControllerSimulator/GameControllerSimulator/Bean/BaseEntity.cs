using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace GameControllerSimulator.Bean
{
    public class BaseEntity<T> where T : class
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

    }
}
