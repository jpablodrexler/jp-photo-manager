using JPPhotoManager.Domain;
using System.Diagnostics;

namespace JPPhotoManager.Infrastructure
{
    public class ProcessService : IProcessService
    {
        public bool IsAlreadyRunning()
        {
            bool result = false;

            int currentProcessId = Environment.ProcessId;
            Process currentProcess = Process.GetProcessById(currentProcessId);
            Process[] processes = Process.GetProcessesByName(currentProcess.ProcessName);

            foreach (Process process in processes)
            {
                result = process.Id != currentProcessId;
                
                if (result)
                {
                    break;
                }
            }

            return result;
        }
    }
}
