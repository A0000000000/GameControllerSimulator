using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameControllerSimulator.Generator
{
    using CommonLibrary.Generator;
    using System;
    using System.Collections.Generic;

    namespace GamepadProtocol
    {
        public class GamepadStatePacket
        {
            public byte[] Current { get; } = new byte[12];
            public byte[] Last { get; } = new byte[12];

            private const int BUTTON_OFFSET = 0;
            private const int AXIS_OFFSET = 2;
            private const int TRIGGER_OFFSET = 10;

            public GamepadStatePacket()
            {
                InitializeState(Current);
                InitializeState(Last);
            }

            public void Reset()
            {
                InitializeState(Current);
                InitializeState(Last);
            }

            private void InitializeState(byte[] data)
            {
                WriteUInt16(data, BUTTON_OFFSET, 0);

                WriteInt16(data, 2, 0);
                WriteInt16(data, 4, 0);
                WriteInt16(data, 6, 0);
                WriteInt16(data, 8, 0);

                data[10] = 0;
                data[11] = 0;
            }

            public void CopyEventToCurrent(byte[] events)
            {
                Buffer.BlockCopy(events, 0, Current, 0, 12);
            }

            public void CopyCurrentToLast()
            {
                Buffer.BlockCopy(Current, 0, Last, 0, 12);
            }

            private short GetAxis(GamepadAxis axis)
            {
                int offset = AXIS_OFFSET + ((int)axis * 2);

                return ReadInt16(Current, offset);
            }

            private byte GetTrigger(GamepadTrigger trigger)
            {
                int offset = TRIGGER_OFFSET + (int)trigger;

                return Current[offset];
            }

            public List<GamepadStateChange> GetChanges()
            {
                List<GamepadStateChange> changes = new();

                ushort currentButtons = ReadUInt16(Current, BUTTON_OFFSET);
                ushort lastButtons = ReadUInt16(Last, BUTTON_OFFSET);

                ushort buttonDiff = (ushort)(currentButtons ^ lastButtons);

                for (int i = 0; i < 16; i++)
                {
                    ushort mask = (ushort)(1 << i);

                    if ((buttonDiff & mask) != 0)
                    {
                        bool pressed = (currentButtons & mask) != 0;

                        changes.Add(new GamepadStateChange
                        {
                            Type = GamepadChangeType.Button,
                            Button = (GamepadButton)i,
                            ButtonPressed = pressed
                        });
                    }
                }

                foreach (GamepadAxis axis in Enum.GetValues<GamepadAxis>())
                {
                    short current = GetAxis(axis);

                    int offset = AXIS_OFFSET + ((int)axis * 2);
                    short last = ReadInt16(Last, offset);

                    if (current != last)
                    {
                        changes.Add(new GamepadStateChange
                        {
                            Type = GamepadChangeType.Axis,
                            Axis = axis,
                            AxisValue = current
                        });
                    }
                }

                foreach (GamepadTrigger trigger in Enum.GetValues<GamepadTrigger>())
                {
                    byte current = GetTrigger(trigger);

                    int offset = TRIGGER_OFFSET + (int)trigger;
                    byte last = Last[offset];

                    if (current != last)
                    {
                        changes.Add(new GamepadStateChange
                        {
                            Type = GamepadChangeType.Trigger,
                            Trigger = trigger,
                            TriggerValue = current
                        });
                    }
                }

                return changes;
            }

            private static ushort ReadUInt16(byte[] data, int offset)
            {
                return (ushort)(data[offset] | (data[offset + 1] << 8));
            }

            private static short ReadInt16(byte[] data, int offset)
            {
                return (short)(data[offset] | (data[offset + 1] << 8));
            }

            private static void WriteUInt16(byte[] data, int offset, ushort value)
            {
                data[offset] = (byte)(value & 0xFF);
                data[offset + 1] = (byte)((value >> 8) & 0xFF);
            }

            private static void WriteInt16(byte[] data, int offset, short value)
            {
                data[offset] = (byte)(value & 0xFF);
                data[offset + 1] = (byte)((value >> 8) & 0xFF);
            }
        }
    }
}
