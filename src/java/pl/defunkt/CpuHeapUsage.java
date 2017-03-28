/* CpuMemoryUsage
 *
 * Calculate JVM CPU and heap memory usage using MXBeans.
 *
 * CPU usage based on JVMCPUUsage class by Lahiru Pathirage:
 * https://gist.github.com/lpsandaruwan/f2cef0aa91ae68cb041c7ecda04a0724
 */

package pl.defunkt;

import java.lang.management.MemoryMXBean;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;


public class CpuHeapUsage {
    private com.sun.management.OperatingSystemMXBean peOperatingSystemMXBean;
    private OperatingSystemMXBean operatingSystemMXBean;
    private RuntimeMXBean runtimeMXBean;
    private MemoryMXBean memoryMXBean;

    // keeping previous timestamps
    private long previousJvmProcessCpuTime = 0;
    private long previousJvmUptime = 0;

    public CpuHeapUsage(MBeanServerConnection mBeanServerConnection) throws IOException {
        setupProxies(mBeanServerConnection);
    }

    // initiate and prepare MXBean interfaces proxy connections
    public void setupProxies(MBeanServerConnection mBeanServerConnection) throws IOException {
        peOperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mBeanServerConnection,
                ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                com.sun.management.OperatingSystemMXBean.class
        );
        operatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mBeanServerConnection,
                ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                OperatingSystemMXBean.class
        );
        runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy(
                mBeanServerConnection,
                ManagementFactory.RUNTIME_MXBEAN_NAME,
                RuntimeMXBean.class
        );
        memoryMXBean = ManagementFactory.newPlatformMXBeanProxy(
            mBeanServerConnection,
            ManagementFactory.MEMORY_MXBEAN_NAME,
            MemoryMXBean.class
        );
    }

    public float getCpuUsed() {
        // elapsed process time is in nanoseconds
        long elapsedProcessCpuTime = peOperatingSystemMXBean.getProcessCpuTime() - previousJvmProcessCpuTime;
        // elapsed uptime is in milliseconds
        long elapsedJvmUptime = runtimeMXBean.getUptime() - previousJvmUptime;

        // total jvm uptime on all the available processors
        long totalElapsedJvmUptime = elapsedJvmUptime * operatingSystemMXBean.getAvailableProcessors();

        // calculate cpu usage as a percentage value
        // to convert nanoseconds to milliseconds divide it by 1000000 and to get a percentage multiply it by 100
        float cpuUsage = elapsedProcessCpuTime / (totalElapsedJvmUptime * 10000F);

        // set old timestamp values
        previousJvmProcessCpuTime = peOperatingSystemMXBean.getProcessCpuTime();
        previousJvmUptime = runtimeMXBean.getUptime();

        return cpuUsage;
    }

    public long getHeapInit() {
        return memoryMXBean.getHeapMemoryUsage().getInit();
    }

    public long getHeapCommitted() {
        return memoryMXBean.getHeapMemoryUsage().getCommitted();
    }

    public long getHeapUsed() {
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    public long getHeapMax() {
        return memoryMXBean.getHeapMemoryUsage().getMax();
    }
}
