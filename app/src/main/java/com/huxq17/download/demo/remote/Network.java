package com.huxq17.download.demo.remote;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class Network {
    public static Observable<List<Music>> getMusicList() {
        return Observable.create(new ObservableOnSubscribe<List<Music>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Music>> e) {
                final List<Music> list = new ArrayList<>();
                list.add(new Music("http://fdfs.xmcdn.com/group14/M01/92/B4/wKgDY1dfi2XQQz6rAB_-0pTygW4326.mp3","music1"));
                list.add(new Music("http://sc.sycdn.kuwo.cn/9615b1de6fd8dfada85683377456a00a/5df608c0/resource/n1/91/12/3997614833.mp3","music2"));
                list.add(new Music("http://m10.music.126.net/20191215183725/ba6252fe123dfae67e6099937d7b08d5/ymusic/0bec/75a9/a09c/e37e93fe58eac7ae44df445d2f8874f0.mp3","music3"));
                list.add(new Music("http://m10.music.126.net/20191215184001/b1cc8a9236c1376ee1341e06444a7c2a/ymusic/0158/030b/0458/62b59ab6574ab87d462532e90a964d3c.mp3","music4"));
                e.onNext(list);
            }
        });
    }
}
