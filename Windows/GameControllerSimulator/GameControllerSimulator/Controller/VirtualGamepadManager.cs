using CommonLibrary.Bean;
using CommonLibrary.Generator;
using GameControllerSimulator.Generator.GamepadProtocol;
using LogLibrary;
using System;
using System.Collections.Generic;
using System.Linq;
using ViGEmBusLibrary;

namespace GameControllerSimulator.Controller
{
    public class VirtualGamepadManager
    {
        public static readonly string TAG = "VirtualGamepadManager";

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
            LogUtils.I(TAG, $"init maxSize = {maxSize}");
            lock (this)
            {
                viGEmBusManager = new ViGEmBusManager();
                gameControllers = new ViGEmBusGameController[maxSize];
                gamepadStatePackets = Enumerable.Range(0, maxSize).Select(_ => new GamepadStatePacket()).ToArray();
            }
        }

        public void Dispose()
        {
            LogUtils.I(TAG, "Dispose");
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
            LogUtils.I(TAG, $"Create Virtual Gamepad index = {index}");
            lock (this)
            {
                if (index < 0 ||viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    LogUtils.E(TAG, "not init or index invalid.");
                    return false;
                }
                if (gameControllers[index] != null)
                {
                    LogUtils.W(TAG, "index has gamepad, dispose");
                    gameControllers[index]?.Dispose();
                    gameControllers[index] = null;
                }
                LogUtils.D(TAG, $"Create {index} gamepad success");
                gameControllers[index] = viGEmBusManager.CreateXboxController(index);
                return true;
            }
        }

        public void Connect(int index)
        {
            LogUtils.I(TAG, $"Connect {index}");
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    LogUtils.E(TAG, "not init or index invalid.");
                    return;
                }
                ViGEmBusGameController? gameController = gameControllers[index];
                if (gameController == null) 
                {
                    LogUtils.E(TAG, "not create game controller");
                    return;
                }
                LogUtils.D(TAG, $"preapare connect {index}");
                gameController.Connect();
                gameController.AddFeedbackEventHandler(feedbackReceived =>
                {
                    LogUtils.D(TAG, $"receive feedback index: {index}, data = {feedbackReceived}");
                    callback.OnFeedbackReceived(gameController.Index, feedbackReceived);
                });
            }
        }

        public void UpdateGameEvent(int index, byte[] events)
        {
            LogUtils.I(TAG, $"UpdateGameEvent {index}");
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length || gamepadStatePackets == null)
                {
                    LogUtils.E(TAG, "not init or index invalid.");
                    return;
                }
                LogUtils.D(TAG, $"UpdateGameEvent index: {index}, events: {Convert.ToHexString(events)}");
                gamepadStatePackets[index].CopyEventToCurrent(events);
                List<GamepadStateChange> changes = gamepadStatePackets[index].GetChanges();
                gameControllers[index]?.UpdateState(changes);
                gamepadStatePackets[index].CopyCurrentToLast();
            }
        }

        public void Dispose(int index)
        {
            LogUtils.I(TAG, $"Dispose {index}");
            lock (this)
            {
                if (index < 0 || viGEmBusManager == null || gameControllers == null || index >= gameControllers.Length)
                {
                    LogUtils.E(TAG, "not init or index invalid.");
                    return;
                }
                LogUtils.D(TAG, $"Dispose {index}");
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
