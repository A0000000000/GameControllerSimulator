using LogLibrary;
using System.Linq;

namespace GameControllerSimulator.Controller
{
    public class ControllerSlotManager
    {

        public static readonly string TAG = "ControllerSlotManager";

        private int maxSlots;
        private string[]? slots;

        public void Init(int maxSlots)
        {
            LogUtils.I(TAG, $"init max slots = {maxSlots}");
            this.maxSlots = maxSlots;
            this.slots = Enumerable.Repeat<string>("", maxSlots).ToArray();
        }

        public int? Allocate(string guid)
        {
            LogUtils.I(TAG, $"allocate guid = {guid}");
            lock (this)
            {
                if (this.slots == null)
                {
                    LogUtils.E(TAG, "Slots not initialized");
                    return null;
                }
                foreach (string slot in slots)
                {
                    if (slot == guid)
                    {
                        LogUtils.W(TAG, $"GUID {guid} is already allocated");
                        return null;
                    }
                }
                for (int i = 0; i < maxSlots; i++)
                {
                    if (string.IsNullOrEmpty(slots[i]))
                    {
                        slots[i] = guid;
                        return i;
                    }
                }
            }
            LogUtils.W(TAG, "No available slots to allocate");
            return null;
        }

        public void Release(string guid)
        {
            LogUtils.I(TAG, $"release guid = {guid}");
            lock (this)
            {
                if (this.slots == null)
                {
                    LogUtils.E(TAG, "Slots not initialized");
                    return;
                }
                for (int i = 0; i < maxSlots; i++)
                {
                    if (slots[i] == guid)
                    {
                        slots[i] = "";
                        break;
                    }
                }
            }
        }

        public int? GetIndex(string guid)
        {
            LogUtils.I(TAG, $"get index for guid = {guid}");
            lock (this)
            {
                if (this.slots == null)
                {
                    LogUtils.E(TAG, "Slots not initialized");
                    return null;
                }
                for (int i = 0; i < maxSlots; i++)
                {
                    if (slots[i] == guid)
                    {
                        LogUtils.I(TAG, $"GUID {guid} is at index {i}");
                        return i;
                    }
                }
                LogUtils.W(TAG, $"GUID {guid} not found in any slot");
                return null;
            }
        }

        public string GetGuid(int index)
        {
            LogUtils.I(TAG, $"get guid for index = {index}");
            lock (this)
            {
                if (this.slots == null)
                {
                    LogUtils.E(TAG, "Slots not initialized");
                    return "";
                }
                if (index >= 0 && index < maxSlots)
                {
                    LogUtils.I(TAG, $"Index {index} has GUID {slots[index]}");
                    return slots[index];
                }
                LogUtils.E(TAG, $"Index {index} is out of bounds");
                return "";
            }
        }

        public bool IsFull()
        {
            LogUtils.I(TAG, "check if slots are full");
            lock (this)
            {
                if (this.slots == null)
                {
                    LogUtils.E(TAG, "Slots not initialized");
                    return true;
                }
                return slots.All(s => !string.IsNullOrEmpty(s));
            }
        }

        public void Clear()
        {
            LogUtils.I(TAG, "clear all slots");
            lock (this)
            {
                slots = null;
            }
        }

    }
}
