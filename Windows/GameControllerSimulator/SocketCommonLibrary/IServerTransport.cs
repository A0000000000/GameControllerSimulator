namespace SocketCommonLibrary
{
    public enum ConnectionType { BLE, TCP, UDP }
    public interface IServerTransport
    {
        public ConnectionType ConnectionType { get; }


        void StartListener();
        void StopListener();


        public interface IClientTransport
        {
            public ConnectionType ConnectionType { get; }

            public bool IsAvailable { get; }

            void SendData(byte[] data, int id = -1);
            void Disconnect();

        }

    }
}
