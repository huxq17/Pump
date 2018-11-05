# Pump
Pump is a fast, easy-to-use android download library that supports multitask, multithread, and breakpoint downloads. For more information, see  [the wiki](https://github.com/huxq17/Pump/wiki/%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E).

## Download
Root project build.gradle：
```
buildscript {
   allprojects {
       repositories {
           jcenter()
       }
   }
    dependencies {
        classpath 'com.buyi.huxq17:agencyplugin:1.1.0'
    }
}
```
App module build.gradle：

```
apply plugin: 'service_agency'

dependencies {
   implementation'com.huxq17.pump:download:1.0.0'
}

```
### Gifs

|Single Task|Multiple Task|
|:-----|:-----|
| <img src="art/download_file.gif" width="280" height="475" /> | <img src="art/download_files.gif" width="280" height="475" /> |

### About me
    Email：huxq17@gmail.com

### License

    Copyright (C) 2018 huxq17

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License
