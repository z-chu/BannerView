# BannerView(轮播图控件)
 [ ![Download](https://api.bintray.com/packages/fungo/maven/bannerview/images/download.svg) ](https://bintray.com/fungo/maven/bannerview/_latestVersion)

轮播图控件，封装ViewPager，支持无限循环轮播，支持三种常用页面特效，支持设置指示器，支持自动切换手动滑动和自动滑动的滑动时长，封装Banner的Holder实现更加简单。使用Kotlin开发，在项目中使用，满足大部分Banner相关需求，可以直接使用。

本项目基于[MZBannerView](https://github.com/pinguo-zhouwei/MZBannerView)进行二次开发，只用于开源交流，如果侵权等问题请及时提醒。


## 使用方法
1. 在application的build.gradle文件中引入仓库依赖

        dependencies {
             implementation  'com.pingerx:bannerview:1.0.0'
        }

2. 在xml文件中引用BannerView控件

        <com.fungo.banner.BannerView
            android:id="@+id/bannerView"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            app:bannerAutoLoop="true"
            app:bannerPageMode="cover"
            app:bannerPageScale="0.9"
            app:bannerPageAlpha="0.6"
            app:bannerFarMargin="10dp"
            app:bannerCoverMargin="10dp"
            app:bannerPagePadding="20dp"
            app:indicatorVisible="true"
            app:indicatorAlign="right"
            app:indicatorPaddingLeft="12dp"
            app:indicatorPaddingBottom="12dp"
            app:indicatorPaddingRight="12dp"/>

3. 在代码中设置数据和适配器

        bannerView.setPages(data, object : BannerHolderCreator<BannerBean, BannerHolder> {
             override fun onCreateBannerHolder(): BannerHolder {
                 return BannerHolder()
             }
         })



## 常用属性
| Name | Format | Description |
| :- | :-| :- |
| bannerAutoLoop| Boolean | 是否开启自动轮播 |
| bannerPageMode| Int | 页面模式 |
| bannerPageScale| Float | 左右页面的缩放比例 |
| bannerPageAlpha| Float | 左右页面的透明度 |
| bannerFarMargin| Dimension | 远离模式下左右页面的外边距 |
| bannerCoverMargin| Dimension | 覆盖模式下左右页面的内边距 |
| bannerPagePadding| Dimension | 中间页面距离左右的距离 |
| indicatorVisible| Boolean | 指示器是否可见 |
| indicatorAlign| Int | 指示器的位置 |
| indicatorPaddingLeft| Int | 指示器距离左侧的距离 |
| indicatorPaddingRight| Int | 指示器距离右侧的边距 |
| indicatorPaddingTop| Int | 指示器距离顶部的边距 |
| indicatorPaddingBottom| Int | 指示器距离底部的边距 |


### 参考
* [仿魅族应用的广告BannerView](https://jianshu.com/p/653680cfe877)
* [巧用ViewPager 打造不一样的广告轮播切换效果](https://blog.csdn.net/lmj623565791/article/details/51339751)
