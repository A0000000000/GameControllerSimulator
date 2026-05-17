using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace GameControllerSimulator.Bean
{

    public class DeviceInfo
    {
        [JsonPropertyName("os_version")]
        public string OsVersion { get; set; } = string.Empty;

        [JsonPropertyName("sdk")]
        public string Sdk { get; set; } = string.Empty;

        [JsonPropertyName("brand")]
        public string Brand { get; set; } = string.Empty;

        [JsonPropertyName("manufacturer")]
        public string Manufacturer { get; set; } = string.Empty;

        [JsonPropertyName("model")]
        public string Model { get; set; } = string.Empty;

        [JsonPropertyName("device")]
        public string Device { get; set; } = string.Empty;

        [JsonPropertyName("product")]
        public string Product { get; set; } = string.Empty;

        [JsonPropertyName("board")]
        public string Board { get; set; } = string.Empty;

        [JsonPropertyName("hardware")]
        public string Hardware { get; set; } = string.Empty;

        [JsonPropertyName("codename")]
        public string Codename { get; set; } = string.Empty;

        [JsonPropertyName("build_id")]
        public string BuildId { get; set; } = string.Empty;

        [JsonPropertyName("fingerprint")]
        public string Fingerprint { get; set; } = string.Empty;

        [JsonPropertyName("supported_abis")]
        public string SupportedAbis { get; set; } = string.Empty;
    }
}
