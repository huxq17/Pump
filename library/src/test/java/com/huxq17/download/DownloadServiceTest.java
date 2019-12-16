//package com.huxq17.download;
//
//import android.content.Context;
//
//import com.huxq17.download.core.DownloadRequest;
//import com.huxq17.download.core.DownloadDispatcher;
//import com.huxq17.download.core.DownLoadLifeCycleCallback;
//import com.huxq17.download.provider.Provider;
//import com.huxq17.download.core.task.DownloadTask;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.robolectric.RobolectricTestRunner;
//
//import java.util.concurrent.ExecutorService;
//
//import static org.junit.Assert.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@RunWith(RobolectricTestRunner.class)
//public class DownloadServiceTest {
//    @Mock
//    private DownLoadLifeCycleCallback lifeCycleObserver;
//    @Mock
//    private Context context;
//    @Mock
//    private ExecutorService customThreadPool;
//    private DownloadDispatcher downloadService;
//
//    @Before
//    public void setup() {
//        MockitoAnnotations.initMocks(this);
//        Provider.init(context);
//        downloadService = spy(new DownloadDispatcher(lifeCycleObserver));
//        TaskManager.setThreadPool(customThreadPool);
//    }
//
//    @Test
//    public void start() {
//        doNothing().when(downloadService).signalConsumer();
//        doNothing().when(customThreadPool).execute(eq(downloadService));
//        downloadService.start();
//        assertTrue(downloadService.isRunning());
//        verify(customThreadPool).execute(eq(downloadService));
//    }
//
//    @Test
//    public void enqueueRequest() {
//        downloadService.setIsRunning(false);
//        doNothing().when(downloadService).signalConsumer();
//        DownloadRequest downloadRequest = spy(new DownloadRequest("", ""));
//        downloadService.enqueueRequest(downloadRequest);
//        verify(downloadService).start();
//        assertTrue(downloadService.isRunning());
//        verify(downloadService).signalConsumer();
//
//        //Enqueue a exist request
//        downloadService.setIsRunning(true);
//        downloadService.enqueueRequest(downloadRequest);
//        verify(downloadService).start();
//        verify(downloadService).signalConsumer();
//        verify(downloadService).printExistRequestWarning(downloadRequest);
//
//        //Enqueue a new request
//        downloadService.enqueueRequest(spy(new DownloadRequest("123", "")));
//        verify(downloadService, times(2)).signalConsumer();
//    }
//
//    @Test
//    public void consumeRequest() {
//        doNothing().when(downloadService).signalConsumer();
//        doNothing().when(customThreadPool).execute(eq(downloadService));
//        DownloadRequest downloadRequest = spy(new DownloadRequest("url", ""));
//        downloadService.enqueueRequest(downloadRequest);
//        doNothing().when(downloadService).waitForConsumer();
//        doReturn(spy(new DownloadDetailsInfo("", ""))).when(downloadService).createDownloadInfo(anyString(), anyString(), anyString(), anyString());
//        doReturn(true).when(downloadService).isUsableSpaceEnough(downloadRequest);
//        downloadService.consumeRequest();
//        verify(downloadService, never()).waitForConsumer();
//        verify(lifeCycleObserver).onDownloadStart(any(DownloadTask.class));
//    }
//
//    @Test
//    public void consumeRequest_block() {
//        start();
//        doNothing().when(downloadService).waitForConsumer();
//        downloadService.consumeRequest();
//        verify(downloadService).waitForConsumer();
//        assertTrue(downloadService.isBlockForConsumeRequest());
//        verify(lifeCycleObserver, never()).onDownloadStart(any(DownloadTask.class));
//    }
//
//    @Test
//    public void consumeRequest_usableSpaceNotEnough() {
//        doNothing().when(downloadService).signalConsumer();
//        doNothing().when(customThreadPool).execute(eq(downloadService));
//        DownloadRequest downloadRequest = spy(new DownloadRequest("url", ""));
//        downloadService.enqueueRequest(downloadRequest);
//        doNothing().when(downloadService).waitForConsumer();
//        doReturn(spy(new DownloadDetailsInfo("", ""))).when(downloadService).createDownloadInfo(anyString(), anyString(), anyString(), anyString());
//        doReturn(false).when(downloadService).isUsableSpaceEnough(downloadRequest);
//        downloadService.consumeRequest();
//        verify(downloadService, never()).waitForConsumer();
//        verify(lifeCycleObserver, never()).onDownloadStart(any(DownloadTask.class));
//    }
//
//    @Test
//    public void consumeRequest_waitRequest() {
//        start();
//        doNothing().when(downloadService).waitForConsumer();
//        downloadService.consumeRequest();
//        verify(downloadService).waitForConsumer();
//        verify(lifeCycleObserver, never()).onDownloadStart(any(DownloadTask.class));
//    }
//
//    @Test
//    public void consumeTask() {
//        consumeRequest();
//        doNothing().when(customThreadPool).execute(any(DownloadTask.class));
//        downloadService.consumeTask();
//        verify(downloadService, never()).waitForConsumer();
//        verify(customThreadPool).execute(any(DownloadTask.class));
//    }
//
//    @Test
//    public void consumeTask_block() {
//        start();
//        doNothing().when(downloadService).waitForConsumer();
//        doNothing().when(customThreadPool).execute(any(DownloadTask.class));
//        when(downloadService.isBlockForConsumeTask()).thenReturn(true).thenReturn(false);
//        downloadService.consumeTask();
//        verify(downloadService).waitForConsumer();
//    }
//
//    @Test
//    public void run() {
//        start();
//        when(downloadService.isRunnable()).thenReturn(true).thenReturn(false);
//        doNothing().when(downloadService).consumeRequest();
//        doNothing().when(downloadService).consumeTask();
//        downloadService.run();
//        verify(downloadService).consumeRequest();
//        verify(downloadService).consumeTask();
//        assertFalse(downloadService.isRunning());
//    }
//
//    @Test
//    public void isRunning() {
//        assertFalse(downloadService.isRunning());
//        start();
//        assertTrue(downloadService.isRunning());
//    }
//
//    @Test
//    public void cancel() {
//        doNothing().when(downloadService).signalConsumer();
//        start();
//        assertTrue(downloadService.isRunnable());
//        downloadService.cancel();
//        verify(downloadService).signalConsumer();
//        assertFalse(downloadService.isRunnable());
//    }
//
//    @Test
//    public void onDownloadEnd() {
//        doAnswer(new Answer() {
//            @Override
//            public Object answer(InvocationOnMock invocation) {
//                DownloadTask downloadTask = invocation.getArgument(0);
//                downloadService.onDownloadEnd(downloadTask);
//                verify(lifeCycleObserver).onDownloadEnd(downloadTask);
//                return null;
//            }
//        }).when(downloadService).executeDownloadTask(any(DownloadTask.class));
//        consumeRequest();
//        doNothing().when(customThreadPool).execute(any(DownloadTask.class));
//        downloadService.consumeTask();
//        verify(downloadService, never()).waitForConsumer();
//    }
//}