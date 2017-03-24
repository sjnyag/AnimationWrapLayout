# AnimationWrapLayout
AnimationWrapLayout is an Android library used to create a layout which aligns child views dynamically (with a animation) on a new line.

Screenshots
-----------

![Demo Screenshot 1][1]
![Demo Screenshot 2][2]

Usage
-----

1. Add ``AnimationWrapLayout`` widget to your layout.

```xml
    <com.github.sjnyag.AnimationWrapLayout
        android:id="@+id/list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        animation_wrap_layout:each_margin_height="4dp"
        animation_wrap_layout:each_margin_width="4dp" />
```

2. Call ``addViewWithAnimation(View view, int position)`` or ``removeViewWithAnimation(View view)`` instead of ``addView`` or ``removeView`` to feel the same way as ViewGroup. 

```java
        AnimationWrapLayout list = (AnimationWrapLayout) findViewById(R.id.list);
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mList.addViewWithAnimation(yourView, position);
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mList.removeViewWithAnimation(yourView)
            }
        });
```

Import AnimationWrapLayout dependency
--------------------------------

your build.gradle
```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```


```groovy
dependencies {
        compile 'com.github.sjnyag:AnimationWrapLayout:0.1.0'
}
```

License
-------
```
   Copyright 2017 sjnyag

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

[1]: ./same_size.gif
[2]: ./random_size.gif
