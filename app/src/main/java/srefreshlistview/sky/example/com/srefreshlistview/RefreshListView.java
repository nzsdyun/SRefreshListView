package srefreshlistview.sky.example.com.srefreshlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sky on 4/10/2017.
 */

public class RefreshListView extends ListView implements AbsListView.OnScrollListener {
     /** header and foot view */
    private View mHeadView, mFootView;
    /** header arrow */
    private ImageView mHeadArrow;
    /** header refresh progress */
    private ProgressBar mProgressBar;
    /** refresh state */
    private TextView mRefreshState;
    /** last refresh time */
    private TextView mLastUpdateTime;
    /** header and footer's height */
    private int mHeadViewHeight, mFootViewHeight;
    /** refresh state, initial is  REFRESH_STATE_IDLE */
    private PullRefreshState mCurrentState = PullRefreshState.REFRESH_STATE_IDLE;
    /** header arrow animation */
    private Animation mDownAnimation, mUpAnimation;
    /** whether item is first */
    private int mFirstItem = 0;
    /** whether the last one */
    private boolean mScrollToBottom = false;
    /** whether loading more */
    private boolean mLoadMore = false;
    /** callback listener */
    private OnRefreshListener mOnRefreshListener;

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeadViews();
        initFootViews();
        this.setOnScrollListener(this);
    }

    private void initFootViews() {
        mFootView = View.inflate(getContext(), R.layout.listview_footer, null);
        mFootView.measure(0, 0);
        mFootViewHeight = mFootView.getMeasuredHeight();
        // hide footer view
        mFootView.setPadding(0, -mFootViewHeight, 0, 0);
        this.addFooterView(mFootView);
    }

    private void initHeadViews() {
        mHeadView = View.inflate(getContext(), R.layout.listview_header, null);
        mHeadArrow = (ImageView) mHeadView.findViewById(R.id.header_arrow);
        mProgressBar = (ProgressBar) mHeadView.findViewById(R.id.refresh_progress);
        mRefreshState = (TextView) mHeadView.findViewById(R.id.refresh_state);
        mLastUpdateTime = (TextView) mHeadView.findViewById(R.id.last_update_time);
        mLastUpdateTime.setText(getLastUpdateTime());
        mHeadView.measure(0, 0);
        mHeadViewHeight = mHeadView.getMeasuredHeight();
        // hide header view
        mHeadView.setPadding(0, -mHeadViewHeight, 0, 0);
        this.addHeaderView(mHeadView);
        initAnimation();
    }

    private void initAnimation() {
        mDownAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mDownAnimation.setDuration(500);
        mDownAnimation.setFillAfter(true);
        mUpAnimation = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mUpAnimation.setDuration(500);
        mUpAnimation.setFillAfter(true);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE ||
                scrollState == SCROLL_STATE_FLING) {
            if (mScrollToBottom && !mLoadMore) {
                mLoadMore = true;
                mFootView.setPadding(0, 0, 0, 0);
                setSelection(getCount());
                // call back interface
                if (mOnRefreshListener != null) {
                    mOnRefreshListener.loadMore();
                }
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mFirstItem = firstVisibleItem;
        mScrollToBottom = getLastVisiblePosition() == (totalItemCount - 1);
    }

    public String getLastUpdateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return getResources().getString(R.string.last_refresh_time, sdf.format(new Date()));
    }

    int startY = 0, moveY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = (int) ev.getY();
                if (mCurrentState == PullRefreshState.REFRESH_STATE_REFRESHING) {
                    // when the refresh does not consume mouse events
                    return false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = (int) ev.getY();
                // down pull distance
                int offsetDistance = moveY - startY;
                // down pull
                if (offsetDistance > 0 && mFirstItem == 0) {
                    if (mCurrentState == PullRefreshState.REFRESH_STATE_IDLE
                            && offsetDistance < mHeadViewHeight) {
                        // first case: the distance down is less than the height of the head
                        mCurrentState = PullRefreshState.REFRESH_STATE_DOWN_PULL_REFRESH;
                        updateHeadViews(mCurrentState);
                    } else if (mCurrentState == PullRefreshState.REFRESH_STATE_IDLE
                            && offsetDistance >= mHeadViewHeight) {
                        // second case: initial pull down quickly causes the pull-down distance to exceed the head height
                        mCurrentState = PullRefreshState.REFRESH_STATE_RELEASE_REFRESH;
                        updateHeadViews(mCurrentState);
                    } else if (mCurrentState == PullRefreshState.REFRESH_STATE_DOWN_PULL_REFRESH
                            && offsetDistance >= mHeadViewHeight) {
                        // third case: the pull-down height is higher than the head and the current state is the pull-down state
                        mCurrentState = PullRefreshState.REFRESH_STATE_RELEASE_REFRESH;
                        updateHeadViews(mCurrentState);
                    } else if (mCurrentState == PullRefreshState.REFRESH_STATE_RELEASE_REFRESH
                            && offsetDistance < mHeadViewHeight) {
                        // four case: the pull-down height is lower than the head height and the current state is the release release state
                        mCurrentState = PullRefreshState.REFRESH_STATE_DOWN_PULL_REFRESH;
                        updateHeadViews(mCurrentState);
                    }
                    int paddingTop = -mHeadViewHeight + offsetDistance;
                    mHeadView.setPadding(0, paddingTop, 0, 0);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mCurrentState == PullRefreshState.REFRESH_STATE_RELEASE_REFRESH) {
                    mCurrentState = PullRefreshState.REFRESH_STATE_REFRESHING;
                    updateHeadViews(mCurrentState);
                    mHeadView.setPadding(0, 0, 0, 0);
                    if (mOnRefreshListener != null) {
                        mOnRefreshListener.refresh();
                    }
                } else if (mCurrentState == PullRefreshState.REFRESH_STATE_DOWN_PULL_REFRESH) {
                    mCurrentState = PullRefreshState.REFRESH_STATE_IDLE;
                    updateHeadViews(mCurrentState);
                    mHeadView.setPadding(0, -mHeadViewHeight, 0, 0);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void updateHeadViews(PullRefreshState refreshState) {
        switch (refreshState) {
            case REFRESH_STATE_IDLE:
                mRefreshState.setText(R.string.refresh_state_down_pull_state);
                mHeadArrow.setVisibility(View.VISIBLE);
                mLastUpdateTime.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                break;
            case REFRESH_STATE_DOWN_PULL_REFRESH:
                mRefreshState.setText(R.string.refresh_state_down_pull_state);
                mHeadArrow.startAnimation(mUpAnimation);
                break;
            case REFRESH_STATE_RELEASE_REFRESH:
                mRefreshState.setText(R.string.refresh_state_release_refresh);
                mHeadArrow.startAnimation(mDownAnimation);
                break;
            case REFRESH_STATE_REFRESHING:
                mRefreshState.setText(R.string.refresh_state_refreshing);
                mHeadArrow.clearAnimation();
                mHeadArrow.setVisibility(View.GONE);
                mLastUpdateTime.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                break;
        }
    }

    public boolean isRefresh() {
        return mCurrentState == PullRefreshState.REFRESH_STATE_REFRESHING;
    }

    public void hiddenHeaderView() {
        mCurrentState = PullRefreshState.REFRESH_STATE_IDLE;
        updateHeadViews(mCurrentState);
        mHeadView.setPadding(0, -mHeadViewHeight, 0, 0);
        mLastUpdateTime.setText(getLastUpdateTime());
    }

    public void hiddenFooterView() {
        mLoadMore = false;
        mFootView.setPadding(0, -mFootViewHeight, 0, 0);
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    public interface OnRefreshListener {
        void loadMore();

        void refresh();
    }

    enum PullRefreshState {
        REFRESH_STATE_IDLE,
        REFRESH_STATE_DOWN_PULL_REFRESH,
        REFRESH_STATE_RELEASE_REFRESH,
        REFRESH_STATE_REFRESHING
    }
}
