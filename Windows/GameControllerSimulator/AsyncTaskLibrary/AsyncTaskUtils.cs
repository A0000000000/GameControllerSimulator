using System;
using System.Threading;
using System.Threading.Tasks;

namespace AsyncTaskLibrary
{
    public static class AsyncTaskUtils
    {
        public const string TAG = "AsyncTaskUtils";
        public static Task Post(Action action, Action<Exception> onException = null, string tag = TAG)
        {
            return Task.Run(() =>
            {
                try
                {
                    action();
                }
                catch(Exception ex) 
                {
                    onException?.Invoke(ex);
                }
            });
        }

        public static Task Post(Func<Task> task, Action<Exception> onException = null, string tag = TAG)
        {
            return Task.Run(async () =>
            {
                try
                {
                    await task();
                }
                catch (Exception ex)
                {
                    onException?.Invoke(ex);
                }
            });
        }

    }
}
