using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace GameControllerSimulator.Bean
{
    public class FeedbackReceived
    {
        [JsonPropertyName("large_motor")]
        public int LargeMotor { get; set; }

        [JsonPropertyName("small_motor")]
        public int SmallMotor { get; set; }

        [JsonPropertyName("led_number")]
        public int LedNumber { get; set; }
    }
}
