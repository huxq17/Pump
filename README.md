# Pump
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-Pump-green.svg?style=flat)]( https://android-arsenal.com/details/1/7293 )

Pump is a fast, easy-to-use android download library that supports multitask, multithread, and breakpoint download. Use HTTP caching mechanism to avoid unnecessary download and make download smarter. For more information, see  [the wiki](https://github.com/huxq17/Pump/wiki/Usage).

See the [中文文档](https://github.com/huxq17/Pump/blob/master/README_cn.md) for Chinese readme.

## Download
App module build.gradle：

```
dependencies {
   implementation'com.huxq17.pump:download:1.3.10'
   implementation 'com.squareup.okhttp3:okhttp:lastversion'
}

```
### Gifs

|Single Task|Multiple Task|
|:-----|:-----|
| <img src="art/download_file.gif" width="280" height="475" /> | <img src="art/download_files.gif" width="280" height="475" /> |

### Proguard Rules

```
-keep class com.huxq17.download.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
```


### About me
    Email：huxq17@gmail.com

### License

    Copyright (C) 2020 huxq17

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License
