using Nefarius.ViGEm.Client;
using System;

namespace ViGEmBusLibrary
{
    public class ViGEmBusManager: IDisposable
    {
        private ViGEmClient client;

        public ViGEmBusManager()
        {
            client = new ViGEmClient();
        }

        public ViGEmBusGameController CreateXboxController(int id)
        {
            return new ViGEmBusGameController(client.CreateXbox360Controller(), id);
        }

        public void Dispose()
        {
            client.Dispose();
        }
    }
}
