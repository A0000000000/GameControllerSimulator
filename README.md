# GameControllerSimulator

* 将设备模拟成一个游戏手柄的Demo

## Client端

### Android
* 提供两种方案
    * 不借助任何软件，仅使用蓝牙和HID协议来模拟通用手柄，详见Android/GameControllerSimulator/app项目
    * 仅模拟UI和监听事件，不处理任何手柄相关逻辑，详见Android/GameControllerSimulator/app2项目，依赖Host端的App


### iOS
* WIP，未开始，未调研


## Host端
### Windows
* 借助开源驱动ViGEmBus，创建虚拟的Xbox手柄，兼容性好
* 处理来自Client端的连接、事件