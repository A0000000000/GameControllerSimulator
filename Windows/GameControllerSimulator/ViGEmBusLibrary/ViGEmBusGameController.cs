using CommonLibrary.Bean;
using CommonLibrary.Generator;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ViGEmBusLibrary
{
    public class ViGEmBusGameController: IDisposable
    {
        private IXbox360Controller controller;
        private int index;

        public ViGEmBusGameController(IXbox360Controller controller, int index)
        {
            this.controller = controller;
            this.index = index;
        }


        public void Connect()
        {
            controller.Connect();
        }

        public void Disconnect()
        {
            controller.Disconnect();
        }

        public void AddFeedbackEventHandler(Action<FeedbackReceived> handler)
        {
            controller.FeedbackReceived += (sender, args) =>
            {
                var feedback = new FeedbackReceived
                {
                    LargeMotor = args.LargeMotor,
                    SmallMotor = args.SmallMotor,
                    LedNumber = args.LedNumber
                };
                handler(feedback);
            };
        }

        public void UpdateState(List<GamepadStateChange> changes)
        {
            if (changes == null || changes.Count == 0)
            {
                return;
            }
            foreach (var change in changes)
            {
                UpdateState(change);
            }
        }

        public void UpdateState(GamepadStateChange change)
        {
            if (change.Type == GamepadChangeType.Button && change.Button != null)
            {
                if (change.Button.Value != GamepadButton.Function)
                {
                    controller.SetButtonState(ButtonConvert(change.Button.Value), change.ButtonPressed);
                }
                else
                {
                    // TODO: Handle the Function button if needed
                }
            }
            if (change.Type == GamepadChangeType.Axis && change.Axis != null)
            {
                controller.SetAxisValue(AxisConvert(change.Axis.Value), change.AxisValue);
            }
            if (change.Type == GamepadChangeType.Trigger && change.Trigger != null)
            {
                controller.SetSliderValue(TriggerConvert(change.Trigger.Value), change.TriggerValue);
            }
        }

        private Xbox360Button ButtonConvert(GamepadButton button)
        {
            return button switch
            {
                GamepadButton.A => Xbox360Button.A,
                GamepadButton.B => Xbox360Button.B,
                GamepadButton.X => Xbox360Button.X,
                GamepadButton.Y => Xbox360Button.Y,
                GamepadButton.Left => Xbox360Button.Left,
                GamepadButton.Top => Xbox360Button.Up,
                GamepadButton.Right => Xbox360Button.Right,
                GamepadButton.Bottom => Xbox360Button.Down,
                GamepadButton.LB => Xbox360Button.LeftShoulder,
                GamepadButton.RB => Xbox360Button.RightShoulder,
                GamepadButton.LS => Xbox360Button.LeftThumb,
                GamepadButton.RS => Xbox360Button.RightThumb,
                GamepadButton.Start => Xbox360Button.Start,
                GamepadButton.Back => Xbox360Button.Back,
                GamepadButton.Guide => Xbox360Button.Guide,
                _ => null
            };
        }

        private Xbox360Axis AxisConvert(GamepadAxis axis)
        {
            return axis switch
            {
                GamepadAxis.LeftX => Xbox360Axis.LeftThumbX,
                GamepadAxis.LeftY => Xbox360Axis.LeftThumbY,
                GamepadAxis.RightX => Xbox360Axis.RightThumbX,
                GamepadAxis.RightY => Xbox360Axis.RightThumbY,
                _ => null
            };
        }

        private Xbox360Slider TriggerConvert(GamepadTrigger trigger)
        {
            return trigger switch
            {
                GamepadTrigger.LeftTrigger => Xbox360Slider.LeftTrigger,
                GamepadTrigger.RightTrigger => Xbox360Slider.RightTrigger,
                _ => null
            };
        }

        public void Dispose()
        {
            controller.Disconnect();
        }
    }
}
