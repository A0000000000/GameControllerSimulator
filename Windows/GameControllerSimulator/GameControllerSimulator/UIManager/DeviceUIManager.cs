using LogLibrary;
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
        public static string TAG = "DeviceUIManager";
        private Action<Action> enqueueUi;
        private TextBlock statusText;
        private TextBlock deviceNameText;
        private TextBlock osNameText;
        private TextBlock currentEventText;

        public DeviceUIManager(Action<Action> enqueueUi, TextBlock statusText, TextBlock deviceNameText, TextBlock osNameText, TextBlock currentEventText)
        {
            LogUtils.I(TAG, "Create DeviceUIManager");
            this.enqueueUi = enqueueUi;
            this.statusText = statusText;
            this.deviceNameText = deviceNameText;
            this.osNameText = osNameText;
            this.currentEventText = currentEventText;
            Reset();
        }


        public void Reset()
        {
            LogUtils.I(TAG, "DeviceUIManager Reset");
            enqueueUi(() =>
            {
                statusText.Text = "状态: Disconnected";
                deviceNameText.Text = "设备名称: N/A";
                osNameText.Text = "系统版本: N/A";
                currentEventText.Text = "当前事件: N/A";
            });
        }

        public void SetStatus(string status)
        {
            LogUtils.I(TAG, $"DeviceUIManager SetStatus status = [{status}]");
            enqueueUi(() =>
            {
                statusText.Text = $"状态: {status}";
            });
        }
        public void SetDeviceName(string deviceName)
        {
            LogUtils.I(TAG, $"DeviceUIManager SetDeviceName deviceName = [{deviceName}]");
            enqueueUi(() =>
            {
                deviceNameText.Text = $"设备名称: {deviceName}";
            });
        }

        public void SetOsName(string osName)
        {
            LogUtils.I(TAG, $"DeviceUIManager SetOsName osName = [{osName}]");
            enqueueUi(() =>
            {
                osNameText.Text = $"系统版本: {osName}";
            });
        }

        public void SetCurrentEvent(string currentEvent)
        {
            LogUtils.I(TAG, $"DeviceUIManager SetCurrentEvent currentEvent = [{currentEvent}]");
            enqueueUi(() =>
            {
                currentEventText.Text = $"当前事件: {currentEvent}";
            });
        }

    }
}
