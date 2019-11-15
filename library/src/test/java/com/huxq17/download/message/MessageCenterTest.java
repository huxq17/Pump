package com.huxq17.download.message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MessageCenterTest {
    MessageCenter messageCenter;

    @Before
    public void setup() {
        messageCenter = Mockito.spy(new MessageCenter());
    }

    @Test
    public void start() {
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