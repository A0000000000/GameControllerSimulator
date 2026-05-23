using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace CommonLibrary.Generator
{
    public class GamepadStateChange
    {
        public GamepadChangeType Type { get; set; }

        public GamepadButton? Button { get; set; }
        public bool ButtonPressed { get; set; }

        public GamepadAxis? Axis { get; set; }
        public short AxisValue { get; set; }

        public GamepadTrigger? Trigger { get; set; }
        public byte TriggerValue { get; set; }

        public override string ToString()
        {
            return JsonSerializer.Serialize(this);
        }

    }
}
