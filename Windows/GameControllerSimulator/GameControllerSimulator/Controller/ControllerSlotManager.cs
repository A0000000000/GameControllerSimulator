using System.Linq;

namespace GameControllerSimulator.Controller
{
    public class ControllerSlotManager
    {
        private int maxSlots;
        private string[]? slots;

        public void Init(int maxSlots)
        {
            this.maxSlots = maxSlots;
            this.slots = Enumerable.Repeat<string>("", maxSlots).ToArray();
        }

        public int? Allocate(string guid)
        {
            lock (this)
            {
                if (this.slots == null)
                {
                    return null;
                }
                foreach (string slot in slots)
                {
                    if (slot == guid)
                    {
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
            return null;
        }

        public void Release(string guid)
        {
            lock (this)
            {
                if (this.slots == null)
                {
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
            lock (this)
            {
                if (this.slots == null)
                {
                    return null;
                }
                for (int i = 0; i < maxSlots; i++)
                {
                    if (slots[i] == guid)
                    {
                        return i;
                    }
                }
                return null;
            }
        }

        public string GetGuid(int index)
        {
            lock (this)
            {
                if (this.slots == null)
                {
                    return "";
                }
                if (index >= 0 && index < maxSlots)
                {
                    return slots[index];
                }
                return "";
            }
        }

        public bool IsFull()
        {
            lock (this)
            {
                if (this.slots == null)
                {
                    return true;
                }
                return slots.All(s => !string.IsNullOrEmpty(s));
            }
        }

        public void Clear()
        {
            lock (this)
            {
                slots = null;
            }
        }

    }
}
