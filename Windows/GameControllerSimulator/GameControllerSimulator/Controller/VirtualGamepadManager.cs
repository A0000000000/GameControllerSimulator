using CommonLibrary.Bean;
using CommonLibrary.Generator;
using GameControllerSimulator.Generator.GamepadProtocol;
using System.Collections.Generic;
using System.Linq;
using ViGEmBusLibrary;

namespace GameControllerSimulator.Controller
{
    public class VirtualGamepadManager
    {
        private ViGEmBusManager? viGEmBusManager;
        private ViGEmBusGameController?[]? gameControllers;
        private GamepadStatePacket[]? gamepadStatePackets;
        private IVirtualGamepadManagerCallback callback;
        public VirtualGamepadManager(IVirtualGamepadManagerCallback callback)
        {
            this.callback = callback;
        }

        public void Init(int maxSize)
        {
            lock (this)
            {
                viGEmBusManager = new ViGEmBusManager();
                gameControllers = new ViGEmBusGameController[maxSize];
                gamepadStatePackets = Enumerable.Range(0, maxSize).Select(_ => new GamepadStatePacket()).ToArray();
            }
        }

        public void Dispose()
        {
            lock (this)
            {
                if (gameControllers != null)
                {
                    for (int i = 0; i < gameControllers.Length; i++)
                    {
                        if (gameControllers[i] != null)
                        {
                            gameControllers[i]?.Dispose();
                            gameControllers[i] = null;
                        }
                    }
                }
                gamepadStatePackets = null;
                gameControllers = null;
                viGEmBusManager?.Dispose();
                viGEmBusManager = null;
            }
        }

        public bool Create(int index)
        {
            lock (this)
            {
                if (index < 0 ||viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    return false;
                }
                if (gameControllers[index] != null)
                {
                    gameControllers[index]?.Dispose();
                    gameControllers[index] = null;
                }
                gameControllers[index] = viGEmBusManager.CreateXboxController(index);
                return true;
            }
        }

        public void Connect(int index)
        {
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    return;
                }
                ViGEmBusGameController? gameController = gameControllers[index];
                if (gameController == null) 
                {
                    return;
                }
                gameController.Connect();
                gameController.AddFeedbackEventHandler(feedbackReceived =>
                {
                    callback.OnFeedbackReceived(gameController.Index, feedbackReceived);
                });
            }
        }

        public void UpdateGameEvent(int index, byte[] events)
        {
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length || gamepadStatePackets == null)
                {
                    return;
                }
                gamepadStatePackets[index].CopyEventToCurrent(events);
                List<GamepadStateChange> changes = gamepadStatePackets[index].GetChanges();
                gameControllers[index]?.UpdateState(changes);
                gamepadStatePackets[index].CopyCurrentToLast();
            }
        }

        public void Dispose(int index)
        {
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    return;
                }
                gameControllers[index]?.Dispose();
                gameControllers[index] = null;
                if (gamepadStatePackets != null)
                {
                    gamepadStatePackets[index].Reset();
                }
            }
        }

    }

    public interface IVirtualGamepadManagerCallback
    {
        void OnFeedbackReceived(int index, FeedbackReceived feedback);
    }

}
