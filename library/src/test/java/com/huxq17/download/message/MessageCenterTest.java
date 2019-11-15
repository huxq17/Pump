package com.huxq17.download.message;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MessageCenterTest {
    @Mock
    private Context context;
    MessageCenter messageCenter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        messageCenter = Mockito.spy(new MessageCenter());
    }

    @Test
    public void start() {
        messageCenter.start(context);
        Assert.assertEquals(context,messageCenter.getContext());
    }

    @Test
    public void notifyProgressChanged() {
    }

    @Test
    public void register() {
    }

    @Test
    public void unRegister() {
    }

    @Test
    public void unRegister1() {
    }
}