using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml.Controls;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameControllerSimulator.UIManager
{
    public class DeviceUIManager
    {
        private DispatcherQueue mainDispatcher;

        private TextBlock statusText;
        private TextBlock deviceNameText;
        private TextBlock osNameText;
        private TextBlock currentEventText;

        public DeviceUIManager(DispatcherQueue dispatcher, TextBlock statusText, TextBlock deviceNameText, TextBlock osNameText, TextBlock currentEventText)
        {
            mainDispatcher = dispatcher;
            this.statusText = statusText;
            this.deviceNameText = deviceNameText;
            this.osNameText = osNameText;
            this.currentEventText = currentEventText;
            Reset();
        }


        public void Reset()
        {
            mainDispatcher.TryEnqueue(() =>
            {
                statusText.Text = "状态: Disconnected";
                deviceNameText.Text = "设备名称: N/A";
                osNameText.Text = "系统版本: N/A";
                currentEventText.Text = "当前事件: N/A";
            });
        }

        public void SetStatus(string status)
        {
            mainDispatcher.TryEnqueue(() =>
            {
                statusText.Text = $"状态: {status}";
            });
        }
        public void SetDeviceName(string deviceName)
        {
            mainDispatcher.TryEnqueue(() =>
            {
                deviceNameText.Text = $"设备名称: {deviceName}";
            });
        }

        public void SetOsName(string osName)
        {
            mainDispatcher.TryEnqueue(() =>
            {
                osNameText.Text = $"系统版本: {osName}";
            });
        }

        public void SetCurrentEvent(string currentEvent)
        {
            mainDispatcher.TryEnqueue(() =>
            {
                currentEventText.Text = $"当前事件: {currentEvent}";
            });
        }

    }
}
