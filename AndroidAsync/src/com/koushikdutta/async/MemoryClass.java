package com.koushikdutta.async;

public class MemoryClass {

     long usedMemInMB = 0;
     long maxHeapSizeInMB = 0;
     long availHeapSizeInMB = 0;


    public static long getAvailableHeapSize()
    {
        final Runtime runtime = Runtime.getRuntime();
        long usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        long maxHeapSizeInMB = runtime.maxMemory() / 1048576L;
        long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        return availHeapSizeInMB;
    }

    public static long getMaxHeapSize()
    {
        final Runtime runtime = Runtime.getRuntime();

        long maxHeapSizeInMB = runtime.maxMemory() / 1048576L;

        return maxHeapSizeInMB;
    }

}
