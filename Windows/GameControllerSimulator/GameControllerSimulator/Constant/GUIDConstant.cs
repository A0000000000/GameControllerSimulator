using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameControllerSimulator.Constant
{
    public static class GUIDConstant
    {
        public static Guid DEFAULT_RFCOMM_GUID = Guid.Parse("0000180D-0000-1000-8000-00805f9b34fb");
        public static Guid GATT_FUN_GUID = Guid.Parse("09251205-1113-1998-2026-000000000000");
        public static Guid GATT_DATA_RFCOMM_GUID = Guid.Parse("09251205-1113-1998-2026-000000000001");
        public static Guid HOST_GUID = Guid.Parse("09251205-1113-1998-2026-000000000002");
        public static Guid TCP_INFO_GUID = Guid.Parse("09251205-1113-1998-2026-000000000003");
        public static Guid UDP_INFO_GUID = Guid.Parse("09251205-1113-1998-2026-000000000004");
    }
}
