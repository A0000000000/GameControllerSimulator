using System.Text.Json.Serialization;

namespace CommonLibrary.Bean
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
