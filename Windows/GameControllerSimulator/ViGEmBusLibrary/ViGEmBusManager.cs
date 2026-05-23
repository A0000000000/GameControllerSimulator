using LogLibrary;
using Nefarius.ViGEm.Client;
using System;

namespace ViGEmBusLibrary
{
    public class ViGEmBusManager: IDisposable
    {
        public static string TAG = "ViGEmBusManager";

        private ViGEmClient client;

        public ViGEmBusManager()
        {
            LogUtils.E(TAG, "Create ViGEmBusManager");
            client = new ViGEmClient();
        }

        public ViGEmBusGameController CreateXboxController(int id)
        {
            LogUtils.E(TAG, $"Create CreateXboxController id = [{id}]");
            return new ViGEmBusGameController(client.CreateXbox360Controller(), id);
        }

        public void Dispose()
        {
            LogUtils.E(TAG, "Dispose ViGEmBusManager");
            client.Dispose();
        }
    }
}
