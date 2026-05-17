# GameControllerSimulator
* 用于将Android手机模拟成游戏手柄的Demo

## 开发环境
* Android Studio
* Codex

## app
* 此项目不依赖任何其它软件，只需要设备有蓝牙即可，将设备模拟成HID手柄，支持Windows、macOS
* 由于是通用手柄协议，用处有限，主要是各个操作系统对HID手柄支持有限

## app2
* 纯手柄UI的模拟，将产生的事件通过蓝牙(Todo: 后续支持TCP/UDP)发送给Windows端，由Windows端自己创建虚拟手柄，解析事件，发送到系统
* 仅支持Windows系统，并且Windows需要安装对应的软件
* 兼容性高，基本上支持所有支持Xbox手柄的游戏输入