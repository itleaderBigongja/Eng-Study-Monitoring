package com.eng.study.engstudy.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 성능 메트릭 수집 스케줄러
 *
 * 역할: 시스템/JVM 메트릭을 주기적으로 수집하여 performance.log에 기록
 * 사용처: StatisticsController의 /api/statistics/performance-metrics
 */
@Slf4j
@Component
public class PerformanceMetricsCollector {

    private static final Logger PERF_LOGGER = LoggerFactory.getLogger("PERFORMANCE_LOGGER");

    /**
     * 1분마다 시스템 메트릭 수집
     */
    @Scheduled(fixedRate = 60000)
    public void collectSystemMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // 시스템 메트릭
            Map<String, Object> systemMetrics = new HashMap<>();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;

                double cpuLoad = sunOsBean.getCpuLoad() * 100;
                if (cpuLoad < 0) cpuLoad = sunOsBean.getSystemCpuLoad() * 100;
                systemMetrics.put("cpu_usage", Math.max(0, Math.round(cpuLoad * 100.0) / 100.0));

                long totalMemory = sunOsBean.getTotalMemorySize();
                long freeMemory = sunOsBean.getFreeMemorySize();
                double memoryUsage = totalMemory > 0 ?
                        ((double) (totalMemory - freeMemory) / totalMemory) * 100 : 0;
                systemMetrics.put("memory_usage", Math.round(memoryUsage * 100.0) / 100.0);

                try {
                    File root = new File("/");
                    long totalDisk = root.getTotalSpace();
                    long freeDisk = root.getFreeSpace();
                    double diskUsage = totalDisk > 0 ?
                            ((double) (totalDisk - freeDisk) / totalDisk) * 100 : 0;
                    systemMetrics.put("disk_usage", Math.round(diskUsage * 100.0) / 100.0);
                } catch (Exception e) {
                    systemMetrics.put("disk_usage", 0.0);
                }
            } else {
                double loadAverage = osBean.getSystemLoadAverage();
                systemMetrics.put("cpu_usage", loadAverage > 0 ? loadAverage : 0.0);
                systemMetrics.put("memory_usage", 0.0);
                systemMetrics.put("disk_usage", 0.0);
            }

            metrics.put("system", systemMetrics);

            // JVM 메트릭
            Map<String, Object> jvmMetrics = new HashMap<>();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double heapUsedMB = heapUsage.getUsed() / (1024.0 * 1024.0);
            jvmMetrics.put("heap_used", Math.round(heapUsedMB * 100.0) / 100.0);

            long totalGcCount = 0;
            long totalGcTime = 0;
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = gc.getCollectionCount();
                long time = gc.getCollectionTime();
                if (count > 0) totalGcCount += count;
                if (time > 0) totalGcTime += time;
            }
            jvmMetrics.put("gc_count", totalGcCount);
            jvmMetrics.put("gc_time", totalGcTime);

            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            jvmMetrics.put("thread_count", threadBean.getThreadCount());

            metrics.put("jvm", jvmMetrics);
            metrics.put("metric_type", "system_snapshot");

            PERF_LOGGER.info(Markers.appendEntries(metrics), "System Performance Metrics");
        } catch (Exception e) {
            log.error("Failed to collect performance metrics", e);
        }
    }
}
