package com.huxq17.download;

import com.huxq17.download.core.SpeedMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SpeedMonitorTest {
    private SpeedMonitor speedMonitor;
    private static final long SECOND = 1000000000;

    @Before
    public void setup() {
        speedMonitor = Mockito.spy(new SpeedMonitor());
    }

    @Test
    public void compute() {
        long currentTime = SECOND;
        Mockito.doReturn(currentTime).when(speedMonitor).currentTime();
        speedMonitor.download(1537);
        currentTime += SECOND;
        Mockito.doReturn(currentTime).when(speedMonitor).currentTime();
        assertEquals("1.5KB/s", speedMonitor.getSpeed());

        speedMonitor.download(1024 * 1024 + 1024 * 100);
        currentTime += SECOND;
        Mockito.doReturn(currentTime).when(speedMonitor).currentTime();
        assertEquals("1.1MB/s", speedMonitor.getSpeed());

        speedMonitor.download(1024 * 1024 * 1024 + 1024 * 1024 * 110);
        currentTime += SECOND;
        Mockito.doReturn(currentTime).when(speedMonitor).currentTime();
        assertEquals("1.11GB/s", speedMonitor.getSpeed());
    }
}