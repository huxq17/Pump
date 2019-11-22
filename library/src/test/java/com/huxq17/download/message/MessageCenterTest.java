package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.provider.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.Iterator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MessageCenterTest {
    @Mock
    private Context context;
    MessageCenter messageCenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Provider.init(context);
        messageCenter = Mockito.spy(new MessageCenter());
    }

    @Test
    public void start() {
        messageCenter.start(context);
        Assert.assertEquals(context, messageCenter.getContext());
    }

    @Test
    public void notifyProgressChanged() {
        final DownloadDetailsInfo downloadDetailsInfo = Mockito.spy(new DownloadDetailsInfo(null, null, null, null));
        Mockito.when(messageCenter.isShutdown()).thenReturn(false);
        Handler handler = mock(Handler.class);
        when(handler.sendMessage(any(Message.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Message message = invocation.getArgument(0);
//                message.getCallback().run();
                DownloadInfoSnapshot downloadInfoSnapshot = (DownloadInfoSnapshot) message.obj;
                Assert.assertEquals(downloadInfoSnapshot.downloadInfo.getCompletedSize(), downloadDetailsInfo.getCompletedSize());
                Assert.assertEquals(downloadInfoSnapshot.downloadInfo.getStatus(), downloadDetailsInfo.getStatus());
                return null;
            }
        });
        messageCenter.setHandler(handler);
        messageCenter.notifyProgressChanged(downloadDetailsInfo);
    }

    @Test
    public void notifyProgressChangedWhenShutdown() {
        final DownloadDetailsInfo downloadDetailsInfo = Mockito.spy(new DownloadDetailsInfo(null, null, null, null));
        Mockito.when(messageCenter.isShutdown()).thenReturn(true);
        Handler handler = mock(Handler.class);
        messageCenter.setHandler(handler);
        messageCenter.notifyProgressChanged(downloadDetailsInfo);
        verify(handler, times(0)).sendMessage(any(Message.class));
    }

    @Test
    public void handleDownloadInfoSnapshot() {
        DownloadListener downloadListener = new DownloadListener();
        downloadListener.setId("url1");
        messageCenter.register(downloadListener);
        messageCenter.register(new DownloadListener());
        messageCenter.register(new DownloadListener());
        Iterator<DownloadListener> iterator = messageCenter.getObserverIterator();
        messageCenter.unRegister(downloadListener);
        DownloadDetailsInfo downloadDetailsInfo = new DownloadDetailsInfo(null, null, null, null);
        DownloadInfoSnapshot snapshot = DownloadInfoSnapshot.obtain();
        snapshot.downloadInfo = downloadDetailsInfo;
        messageCenter.handleDownloadInfoSnapshot(iterator, snapshot);
    }

    @Test
    public void register() {
        DownloadListener listener1 = spy(DownloadListener.class);
        listener1.setId("id1");
        DownloadListener listener2 = spy(DownloadListener.class);
        listener2.setId("id2");
        DownloadListener listener3 = spy(DownloadListener.class);
        listener3.setId("id1");
        int observerSize = messageCenter.getObserverSize();
        messageCenter.register(listener1);
        messageCenter.register(listener2);
        messageCenter.register(listener3);
        Assert.assertEquals(messageCenter.getObserverSize(), observerSize + 3);
        Assert.assertTrue(listener1.isEnable());
        Assert.assertTrue(listener2.isEnable());
        Assert.assertTrue(listener3.isEnable());


    }

    @Test
    public void unRegister() {
        register();
        int observerSize = messageCenter.getObserverSize();
        messageCenter.unRegister("id1");
        Assert.assertEquals(observerSize - 2, messageCenter.getObserverSize());
    }

    @Test
    public void unRegister1() {
        register();
        int observerSize = messageCenter.getObserverSize();
        DownloadListener downloadListener = new DownloadListener();
        downloadListener.setEnable(true);
        downloadListener.setId("id2");
        messageCenter.unRegister(downloadListener);
        Assert.assertEquals(observerSize - 1, messageCenter.getObserverSize());
        Assert.assertFalse(downloadListener.isEnable());
    }
}