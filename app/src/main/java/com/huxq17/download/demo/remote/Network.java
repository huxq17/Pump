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
                list.add(new Music("https://raw.githubusercontent.com/huxq17/Pump/master/music/East%20Of%20Eden.mp3","East of Eden.mp3"));
                list.add(new Music("https://raw.githubusercontent.com/huxq17/Pump/master/music/Glad%20You%20Came.mp3","Glad You Came.mp3"));
                list.add(new Music("https://raw.githubusercontent.com/huxq17/Pump/master/music/Road%20Trip.mp3","Road Trip.mp3"));
                list.add(new Music("https://raw.githubusercontent.com/huxq17/Pump/master/music/Stuttering.mp3","Stuttering.mp3"));
                e.onNext(list);
            }
        });
    }
}
