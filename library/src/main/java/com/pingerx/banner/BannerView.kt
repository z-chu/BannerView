package com.pingerx.banner

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Scroller
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.pingerx.banner.holder.BannerHolderCreator
import com.pingerx.banner.holder.BaseBannerHolder
import com.pingerx.banner.pager.BounceBackViewPager
import com.pingerx.banner.transformer.CoverModeTransformer
import com.pingerx.banner.transformer.ScaleAlphaTransformer
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.*

/**
 *
 * @author Pinger
 * @since 18-7-18 下午13:21
 *
 * 封装ViewPager,实现了多种功能。包括延时无限循环，自动滑动慢速切换，手动快速自然切换，
 * 滑动缩放特效，指示器定制，Holder基类封装处理，页面点击事件，滑动监听事件等。
 * 基本满足了项目中Banner的需求，使用也十分简单。
 *
 * 本仓库基于项目[MZBannerView]进行优化改造，因此代码会有多处雷同，
 * 但是本仓库只是用于开源，并未商用，如有侵权违法请及时通知。
 *
 * 参考：[https://github.com/pinguo-zhouwei/MZBannerView]
 *
 */
class BannerView<T> @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int =0
) : RelativeLayout(context, attrs, defStyleAttr) {

    // 适配器
    private var mAdapter: BannerPagerAdapter<T>? = null

    // ViewPager当前位置
    private var mCurrentItem = 0

    // Banner自动轮播的切换时间，默认是4秒
    private var mDuration = 4000

    // 是否支持滑动动画
    private var isSlideAnim = true

    // 是否自动轮播图片
    private var isAutoLoop = true

    // 是否处于自动播放中
    private var isAutoLooping = false

    // 滑动模式，默认是远离模式
    private var mPageMode = PageMode.FAR

    // 覆盖模式下，覆盖的宽度,默认10dp
    private var mCoverMargin = dpToPx(10)

    // 远离模式下，页面的边距，默认10dp
    private var mFarMargin = dpToPx(10)

    // 覆盖模式下中间ViewPager左右的padding,默认20dp
    private var mPagePadding = dpToPx(20)

    // 左右两边页面的缩放比例
    private var mPageScale = 0.9f

    // 左右两边页面的透明度
    private var mPageAlpha = 0.8f

    // Banner手动滑动时的滑动时长
    private var mSlideDuration = 800

    // 第一次手动触摸Banner的时候
    private var mFirstTouchTime = 0L

    // 距离左边的距离
    private var mIndicatorPaddingLeft = dpToPx(12)
    // 距离右边的距离
    private var mIndicatorPaddingRight = dpToPx(12)
    // 距离上边的距离
    private var mIndicatorPaddingTop = dpToPx(12)
    // 距离下边的距离
    private var mIndicatorPaddingBottom = dpToPx(12)
    // 指示器的位置
    private var mIndicatorAlign = IndicatorAlign.CENTER
    // 指示器是否可见，默认不可见
    private var isIndicatorVisible = false

    // 页面改变的监听事件
    private var mOnPageChangeListener: ViewPager.OnPageChangeListener? = null

    // ViewPager
    private val mViewPager: BounceBackViewPager by lazy {
        findViewById<BounceBackViewPager>(R.id.viewPager)
    }

    // IndicatorContainer
    private val mIndicatorContainer: LinearLayout  by lazy {
        findViewById<LinearLayout>(R.id.indicatorContainer)
    }

    // 指示器的ImageView集合
    private val mIndicators = ArrayList<ImageView>()

    // 指示器的图片资源 mIndicatorRes[0] 为为选中，mIndicatorRes[1]为选中
    private var mIndicatorRes = emptyArray<Int>()

    init{
        readAttrs(context, attrs)
        initView()
    }

    private fun readAttrs(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerView)
        isAutoLoop = typedArray.getBoolean(R.styleable.BannerView_bannerAutoLoop, isAutoLoop)
        isSlideAnim = typedArray.getBoolean(R.styleable.BannerView_bannerSlideAnim, isSlideAnim)
        isIndicatorVisible = typedArray.getBoolean(R.styleable.BannerView_indicatorVisible, isIndicatorVisible)

        val pageMode = typedArray.getInt(R.styleable.BannerView_bannerPageMode, mPageMode.ordinal)
        mCoverMargin = typedArray.getDimensionPixelSize(R.styleable.BannerView_bannerCoverMargin, mCoverMargin)
        mFarMargin = typedArray.getDimensionPixelSize(R.styleable.BannerView_bannerFarMargin, mFarMargin)
        mPageScale = typedArray.getFloat(R.styleable.BannerView_bannerPageScale, mPageScale)
        mPageAlpha = typedArray.getFloat(R.styleable.BannerView_bannerPageAlpha, mPageAlpha)

        mDuration = typedArray.getInteger(R.styleable.BannerView_bannerDuration, mDuration)
        mSlideDuration = typedArray.getInteger(R.styleable.BannerView_bannerSlideDuration, mSlideDuration)

        val defaultPadding = if (pageMode == PageMode.NORMAL.ordinal) 0 else mPagePadding
        mPagePadding = typedArray.getDimensionPixelSize(R.styleable.BannerView_bannerPagePadding, defaultPadding)

        val indicatorAlign = typedArray.getInt(R.styleable.BannerView_indicatorAlign, mIndicatorAlign.ordinal)
        mIndicatorPaddingLeft = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingLeft, mIndicatorPaddingLeft)
        mIndicatorPaddingRight = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingRight, mIndicatorPaddingRight)
        mIndicatorPaddingTop = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingTop, mIndicatorPaddingTop)
        mIndicatorPaddingBottom = typedArray.getDimensionPixelSize(R.styleable.BannerView_indicatorPaddingBottom, mIndicatorPaddingBottom)

        val indicatorSelectRes = typedArray.getResourceId(R.styleable.BannerView_indicatorSelectRes, R.drawable.banner_indicator_selected)
        val indicatorUnSelectRes = typedArray.getResourceId(R.styleable.BannerView_indicatorSelectRes, R.drawable.banner_indicator_normal)

        typedArray.recycle()

        mIndicatorRes = arrayOf(indicatorSelectRes, indicatorUnSelectRes)

        mPageMode = when (pageMode) {
            PageMode.COVER.ordinal -> PageMode.COVER
            PageMode.FAR.ordinal -> PageMode.FAR
            else -> PageMode.NORMAL
        }

        mIndicatorAlign = when (indicatorAlign) {
            IndicatorAlign.LEFT.ordinal -> IndicatorAlign.LEFT
            IndicatorAlign.RIGHT.ordinal -> IndicatorAlign.RIGHT
            else -> IndicatorAlign.CENTER
        }
    }


    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.layout_banner, this, true)

        // 初始化Scroller
        initViewPagerScroll()
    }

    /**
     * 指示器对齐方式
     */
    enum class IndicatorAlign {
        LEFT,     // 做对齐
        CENTER,   // 居中对齐
        RIGHT     // 右对齐
    }


    /**
     * Banner滑动的模式
     */
    enum class PageMode {
        NORMAL,    // 普通
        COVER,     // 覆盖
        FAR        // 远离
    }


    /**
     * 无限循环的Runnable，自动进行页面切换
     */
    private val mHandler = Handler()
    private val mLoopRunnable = object : Runnable {
        override fun run() {
            if (mAdapter == null) return
            if (isAutoLooping) {
                mCurrentItem = mViewPager.currentItem
                mCurrentItem++
                if (mCurrentItem == mAdapter!!.count - 1) {
                    mCurrentItem = 0
                    mViewPager.setCurrentItem(mCurrentItem, false)
                    mHandler.postDelayed(this, mDuration.toLong())
                } else {
                    mViewPager.currentItem = mCurrentItem
                    mHandler.postDelayed(this, mDuration.toLong())
                }
            } else {
                mHandler.postDelayed(this, mDuration.toLong())
            }
        }
    }

    /**
     * 设置ViewPager的滑动速度
     */
    private fun initViewPagerScroll() {
        if (isSlideAnim) {
            try {
                val mScroller: Field = ViewPager::class.java.getDeclaredField("mScroller")
                mScroller.isAccessible = true
                mScroller.set(mViewPager, ViewPagerScroller(mViewPager.context))
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }


    /**
     * 初始化指示器Indicator
     */
    private fun initIndicator(datas: List<T>) {
        // 如果数据大小不够就隐藏
        if (datas.size < 2) {
            setIndicatorVisible(false)
        } else {
            setIndicatorVisible(isIndicatorVisible)
        }

        mIndicatorContainer.removeAllViews()
        mIndicators.clear()

        // 设置指示器的位置
        setIndicatorAlign(mIndicatorAlign)

        // 填充指示器
        for (i in datas.indices) {
            val imageView = ImageView(context)
            imageView.setPadding(6, 0, 6, 0)
            if (i == mCurrentItem % datas.size) {
                imageView.setImageResource(mIndicatorRes[1])
            } else {
                imageView.setImageResource(mIndicatorRes[0])
            }
            mIndicators.add(imageView)
            mIndicatorContainer.addView(imageView)
        }
    }

    /**
     * 设置页面的轮播样式
     */
    private fun initPageMode(pageMode: PageMode) {
        when (pageMode) {
            PageMode.COVER -> {
                mViewPager.pageMargin = 0
                mViewPager.setPadding(mPagePadding, 0, mPagePadding, 0)
                mViewPager.setPageTransformer(false, CoverModeTransformer(mViewPager, mCoverMargin, mPagePadding, mPageScale, mPageAlpha))
            }
            PageMode.FAR -> {
                mViewPager.pageMargin = mFarMargin
                mViewPager.setPadding(mPagePadding, 0, mPagePadding, 0)
                mViewPager.setPageTransformer(false, ScaleAlphaTransformer(mPageScale, mPageAlpha))
            }
            else -> {
                // 普通模式，设置默认的transformer
                mViewPager.setPadding(0, 0, 0, 0)
                mViewPager.pageMargin = 0
                mViewPager.setPageTransformer(false) { _, _ -> }
            }
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isAutoLoop) {
            return super.dispatchTouchEvent(ev)
        }
        when (ev.action) {
            // TODO 触摸边缘不滑动
            // 按住Banner的时候，停止自动轮播
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_OUTSIDE, MotionEvent.ACTION_DOWN -> {
                mFirstTouchTime = System.currentTimeMillis()
                // 按下或者移动的时候，暂停轮播
                pause()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mFirstTouchTime = System.currentTimeMillis()
                start()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * BannerView的适配器，设置无限轮播的方式是让适配器以为数据有无限条，然后让第一条为中间位置
     */
    inner class BannerPagerAdapter<T>(
            private val datas: List<T>,
            private val holderCreator: BannerHolderCreator<T, *>,
            private val loopEnable: Boolean) : PagerAdapter() {

        private var pageReference: WeakReference<ViewPager>? = null
        private val looperCountFactor = 500

        /**
         * 我们设置当前选中的位置为Integer.MAX_VALUE / 2,这样开始就能往左滑动
         * 但是要保证这个值与getRealPosition 的 余数为0，因为要从第一页开始显示
         * 直到找到从0开始的位置
         */
        private val startSelectItem: Int
            get() {
                var currentItem = realCount * looperCountFactor / 2
                if (currentItem % realCount == 0) {
                    return currentItem
                }
                while (currentItem % realCount != 0) {
                    currentItem++
                }
                return currentItem
            }

        /**
         * 获取真实的Count
         */
        private val realCount: Int
            get() = datas.size

        /**
         * 初始化Adapter和设置当前选中的Item
         */
        fun setUpViewPager(viewPager: ViewPager) {
            pageReference = WeakReference(viewPager)

            viewPager.adapter = this
            notifyDataSetChanged()
            val currentItem = if (loopEnable) startSelectItem else 0
            // 设置当前选中的Item
            viewPager.currentItem = currentItem
        }

        /**
         * 如果getCount 的返回值为Integer.MAX_VALUE 的话，
         * 那么在setCurrentItem的时候会ANR(除了在onCreate 调用之外)
         */
        override fun getCount(): Int {
            return if (loopEnable) realCount * looperCountFactor else realCount
        }

        override fun isViewFromObject(view: View, any: Any): Boolean {
            return view === any
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = getView(position, container)
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            container.removeView(any as View)
        }

        override fun finishUpdate(container: ViewGroup) {
            // 轮播模式才执行
            if (loopEnable) {
                var position = pageReference?.get()?.currentItem
                if (position == count - 1) {
                    position = 0
                    setCurrentItem(position)
                }
            }
        }

        private fun setCurrentItem(position: Int) {
            try {
                pageReference?.get()?.setCurrentItem(position, false)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        /**
         * 初始化Holder的视图和数据，并且回调方法给子类处理
         * @param position 视图的位置
         * @param container 视图容器
         * @return　Holder生成的视图
         */
        private fun getView(position: Int, container: ViewGroup): View {

            val realPosition = position % realCount
            val holder: BaseBannerHolder<T> = holderCreator.onCreateBannerHolder() as BaseBannerHolder<T>

            // create View
            val view = LayoutInflater.from(container.context).inflate(holder.getHolderResId(), container, false)

            // bind data
            if (datas.isNotEmpty()) {
                holder.onBindData(view, datas[realPosition])
            }

            // set listener
            view.setOnClickListener { v ->
                holder.onPageClick(v, realPosition, datas[realPosition])
            }
            return view
        }
    }

    /**
     * 由于ViewPager 默认的切换速度有点快，因此用一个Scroller 来控制切换的速度
     * 而实际上ViewPager 切换本来就是用的Scroller来做的，因此我们可以通过反射来
     * 获取取到ViewPager 的 mScroller 属性，然后替换成我们自己的Scroller
     */
    inner class ViewPagerScroller(context: Context) : Scroller(context) {

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
            super.startScroll(startX, startY, dx, dy, mSlideDuration)
        }

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            var durationX = duration
            // 如果是自动轮播，就使用动画模式，如果不是就还是用原来的模式
            if (System.currentTimeMillis() - mFirstTouchTime >= mDuration) {
                durationX = mSlideDuration
            }
            super.startScroll(startX, startY, dx, dy, durationX)
        }
    }


    /**
     * Banner的Holder点击回调
     */
    interface BannerPageClickListener<T> {
        fun onPageClick(view: View, position: Int, data: T)
    }

    /**
     * 单位转化
     */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics).toInt()
    }


    /**
     * 添加指示器的位置规则
     * 由于RelativeLayout的布局规则，不能添加冲突的规则，所以这里需要
     * 先将之前可能添加的规则移除掉
     * @param params 布局参数
     * @param verb 规则
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun addIndicatorRule(params: RelativeLayout.LayoutParams, verb: Int) {
        when (verb) {
            RelativeLayout.ALIGN_PARENT_LEFT -> {
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            RelativeLayout.ALIGN_PARENT_RIGHT -> {
                params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
                params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
            }

            RelativeLayout.CENTER_HORIZONTAL -> {
                params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
                params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }
        }
        params.addRule(verb)
    }


    // －－－－－－－－－－－相关常用API－－－－－－－－－－－－－－
    // －－－－－－－－－－－相关常用API－－－－－－－－－－－－－－
    // －－－－－－－－－－－相关常用API－－－－－－－－－－－－－－
    /**
     * 设置视图和数据，这是最重要的一个方法
     * 其他的配置应该在这个方法之前调用
     *
     * @param datas  Banner展示的数据集合
     * @param holderCreator ViewHolder生成器 [BannerHolderCreator] And [BaseBannerHolder]
     */
    fun <BH : BaseBannerHolder<T>> setPages(datas: List<T>?, holderCreator: BannerHolderCreator<T, BH>) {
        if (datas == null) {
            return
        }

        // 如果在播放，就先让播放停止
        pause()

        // 如果Banner数据不够，就去处特效
        if (datas.size < 3) {
            val layoutParams = mViewPager.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 0, 0)
            mViewPager.layoutParams = layoutParams
            clipChildren = true
            mViewPager.clipChildren = true
        }

        // 设置ViewPager适配器
        mAdapter = BannerPagerAdapter(datas, holderCreator, isAutoLoop)
        mAdapter!!.setUpViewPager(mViewPager)
        mViewPager.offscreenPageLimit = datas.size

        // 添加滑动监听
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                val realPosition = position % mIndicators.size
                mOnPageChangeListener?.onPageScrolled(realPosition, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                mCurrentItem = position

                // 如果数据不对等就return
                if (mIndicators.size != datas.size) return

                // 切换indicator
                val realSelectPosition = mCurrentItem % mIndicators.size
                for (i in datas.indices) {
                    if (i == realSelectPosition) {
                        mIndicators[i].setImageResource(mIndicatorRes[1])
                    } else {
                        mIndicators[i].setImageResource(mIndicatorRes[0])
                    }
                }
                // 不能直接将mOnPageChangeListener 设置给ViewPager ,否则拿到的position 是原始的positon
                mOnPageChangeListener?.onPageSelected(realSelectPosition)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager.SCROLL_STATE_DRAGGING -> isAutoLooping = false
                    ViewPager.SCROLL_STATE_SETTLING -> isAutoLooping = true
                }
                mOnPageChangeListener?.onPageScrollStateChanged(state)
            }
        })

        // 根据数据的大小，初始化Indicator
        initIndicator(datas)

        // 设置页面轮播模式
        initPageMode(mPageMode)

        // 如果设置了可以轮播，那就设置自动轮播
        if (isAutoLoop) {
            start()
        }
    }


    /**
     * 开始轮播
     * 应该确保在调用用了[setPages] 之后调用这个方法开始轮播
     * 如果设置了自动轮播
     */
    fun start() {
        // 如果Adapter为null, 说明还没有设置数据，这个时候不应该轮播Banner
        if (isAutoLoop && !isAutoLooping && mAdapter?.count ?: 0 > 2) {
            isAutoLooping = true
            this.mHandler.postDelayed(mLoopRunnable, mDuration.toLong())
        }
    }

    /**
     * BannerView停止轮播
     * Banner将会停止自动滑动，一般当BannerView不可见或者正在触摸时需要停止滑动
     */
    fun pause() {
        isAutoLooping = false
        this.mHandler.removeCallbacks(mLoopRunnable)
    }


    /**
     * 获取ViewPager对象
     * @return ViewPager
     */
    fun getViewPager(): ViewPager? {
        return this.mViewPager
    }


    /**
     * 设置BannerView页面的切换时间间隔
     * 开启自动轮播之后，过了duration时间Page就会自动切换到下一页
     * @param duration 切换时长
     */
    fun setDuration(duration: Int) {
        this.mDuration = duration
    }

    /**
     * 设置滑动BannerView时，滑动过程中的时长
     *
     * @param slideDuration 滑动时长
     */
    fun setSlideDuration(slideDuration: Int) {
        mSlideDuration = slideDuration
    }

    /**
     * 添加Banner滑动事件
     * @param listener 滑动回调
     */
    fun addOnPageChangeListener(listener: ViewPager.OnPageChangeListener) {
        this.mOnPageChangeListener = listener
    }

    /**
     * 是否显示Indicator
     * @param visible 是否可见
     */
    fun setIndicatorVisible(visible: Boolean) {
        isIndicatorVisible = visible
        if (visible) {
            this.mIndicatorContainer.visibility = View.VISIBLE
        } else {
            this.mIndicatorContainer.visibility = View.GONE
        }
    }

    /**
     * 设置Indicator 的对齐方式
     * 需要在setPages之前设置，如果要设置indicator的边距，也要在setIndicatorAlign方法前设置
     * @param indicatorAlign 包括三个方向
     * 中间：[IndicatorAlign.CENTER]　
     * 左边：[IndicatorAlign.LEFT]
     * 右边：[IndicatorAlign.RIGHT]
     */
    fun setIndicatorAlign(indicatorAlign: IndicatorAlign) {
        // 如果数据不够2条就不展示指示器
        if (mAdapter?.count ?: 0 < 2) {
            setIndicatorVisible(false)
        }

        // 设置Indicator展示的位置
        mIndicatorAlign = indicatorAlign

        val params = mIndicatorContainer.layoutParams as RelativeLayout.LayoutParams

        when (indicatorAlign) {
            IndicatorAlign.LEFT -> addIndicatorRule(params, RelativeLayout.ALIGN_PARENT_LEFT)
            IndicatorAlign.RIGHT -> addIndicatorRule(params, RelativeLayout.ALIGN_PARENT_RIGHT)
            else -> addIndicatorRule(params, RelativeLayout.CENTER_HORIZONTAL)
        }

        // 设置Indicator 的边距
        params.setMargins(mIndicatorPaddingLeft + mPagePadding, mIndicatorPaddingTop, mIndicatorPaddingRight + mPagePadding, mIndicatorPaddingBottom)
        mIndicatorContainer.layoutParams = params
    }


    /**
     * 设置indicator 图片资源
     *
     * @param selectRes   选中状态资源图片
     * @param unSelectRes 未选中状态资源图片
     */
    fun setIndicatorRes(@DrawableRes selectRes: Int, @DrawableRes unSelectRes: Int) {
        this.mIndicatorRes[0] = unSelectRes
        this.mIndicatorRes[1] = selectRes
    }

    /**
     * 设置指示器的边距
     * @param left 左边padding值
     * @param top 顶部padding值
     * @param right 右边padding值
     * @param bottom 底部padding值
     */
    fun setIndicatorPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mIndicatorPaddingLeft = left
        mIndicatorPaddingTop = top
        mIndicatorPaddingRight = right
        mIndicatorPaddingBottom = bottom
    }

    /**
     * 设置自动轮播
     * @param isAutoLoop true 设置数据后，就会自动轮播 false　不会自动轮播
     */
    fun setAutoLoop(isAutoLoop: Boolean) {
        this.isAutoLoop = isAutoLoop
    }


    /**
     * 设置覆盖模式下，左右页面覆盖中间页面的边距
     * @param coverMargin 覆盖距离
     */
    fun setCoverMargin(coverMargin: Int) {
        this.mCoverMargin = coverMargin
    }


    /**
     * 设置远离模式下，左右页面距离中间页面的距离
     * @param farMargin　页面边距
     */
    fun setFarMargin(farMargin: Int) {
        this.mFarMargin = farMargin
    }


    /**
     * 设置中间页面距离左右两边的距离
     * @param pagePadding 页面padding值
     */
    fun setPagePadding(pagePadding: Int) {
        this.mPagePadding = pagePadding
    }

    /**
     * 设置左右页面的高度缩放比例
     * @param pageScale 缩放比例
     */
    fun setPageScale(pageScale: Float) {
        this.mPageScale = pageScale
    }

    /**
     * 设置左右页面的透明度
     * @param pageAlpha　透明度
     */
    fun setPageAlpha(pageAlpha: Float) {
        this.mPageAlpha = pageAlpha
    }

    /**
     * 页面轮播的模式
     * 设置页面模式之前，如果要对padding和margin这些属性设置
     * 一定要在setPages或者setPageMode之前设置，不然不会生效
     * @param pageMode　轮播模式
     */
    fun setPageMode(pageMode: PageMode) {
        mPageMode = pageMode
    }

    /**
     * 获取当前的页面模式
     * @return 页面模式
     */
    fun getPageMode(): PageMode {
        return mPageMode
    }

    /**
     * 设置ViewPager越界回弹的宽度
     */
    fun setOverscrollTranslation(overscrollTranslation: Float) {
        mViewPager.setOverscrollTranslation(overscrollTranslation)
    }

    /**
     * 设置越界回弹返回的时长
     */
    fun setOverscrollAnimationDuration(overscrollAnimationDuration: Long) {
        mViewPager.setOverscrollAnimationDuration(overscrollAnimationDuration)
    }
}
