/*
 * Copyright (C) 2014. Rayman Zhang <raymanzhang@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//TO DO:
//
// - improve timer performance (especially on Eee Pad)
// - improve child rearranging

package cn.mdict.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;

import cn.mdict.R;

public class DraggableGridView extends FrameLayout implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {
    //layout vars
    public static float childRatio = .9f;
    protected int colCount, childSize, padding, dpi, scroll = 0;
    protected float lastDelta = 0;
    protected Handler handler = new Handler();
    //dragging vars
    protected int dragged = -1, lastX = -1, lastY = -1, lastTarget = -1, lastHoverTarget = -1;
    protected boolean enabled = true;
    protected boolean touching = false;
    private boolean groupingAllowed = false;
    private boolean touchAllowed = true;
    //anim vars
    public static int animT = 150;

    public static int defaultChildSize = 120;
    protected ArrayList<Integer> newPositions = new ArrayList<Integer>();
    //listeners
    protected OnRearrangeListener onRearrangeListener;
    protected OnGroupListener onGroupListener;
    protected OnClickListener secondaryOnClickListener;
    private OnItemClickListener onItemClickListener;
    private OnItemCheckedListener onItemCheckedListener;
    private OnDraggedOutOfContainerListener onDraggedOutOfContainerListener;

    //CONSTRUCTOR AND HELPERS
    public DraggableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setListeners();
        handler.removeCallbacks(updateTask);
        handler.postAtTime(updateTask, SystemClock.uptimeMillis() + 500);
        setChildrenDrawingOrderEnabled(true);

        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dpi = metrics.densityDpi;
    }

    protected void setListeners() {
        setOnTouchListener(this);
        super.setOnClickListener(this);
        setOnLongClickListener(this);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        secondaryOnClickListener = l;
    }

    protected Runnable updateTask = new Runnable() {
        public void run() {
            if (isTouchAllowed()) {
                if (dragged != -1) {
                    if (lastY < padding * 3 && scroll > 0)
                        scroll -= 20;
                    else if (lastY > getBottom() - getTop() - (padding * 3) && scroll < getMaxScroll())
                        scroll += 20;
                } else if (lastDelta != 0 && !touching) {
                    scroll += lastDelta;
                    lastDelta *= .9;
                    if (Math.abs(lastDelta) < .25)
                        lastDelta = 0;
                }
                clampScroll();
                onLayout(true, getLeft(), getTop(), getRight(), getBottom());
            }
            handler.postDelayed(this, 25);
        }
    };

    //OVERRIDES
    @Override
    public void addView(View child) {
        super.addView(child);
        newPositions.add(-1);
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        newPositions.remove(index);
    }

    //LAYOUT
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //compute width of view, in dp
        float w = (r - l) / (dpi / 160f);

        //determine number of columns, at least 2
        colCount = 2;
        int sub = 240;
        w -= 280;
        while (w > 0) {
            colCount++;
            w -= sub;
            sub += 40;
        }

        //determine childSize and padding, in px
        childSize = (r - l) / colCount;
        childSize = Math.round(childSize * childRatio);
        padding = ((r - l) - (childSize * colCount)) / (colCount + 1);

        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).findViewById(R.id.item_cb).setTag(i);
            if (onItemCheckedListener != null) {
                ((CheckBox) getChildAt(i).findViewById(R.id.item_cb)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        int position = Integer.valueOf(compoundButton.getTag().toString());// dgv.getFirstVisiblePosition();
                        onItemCheckedListener.OnItemChecked(position);
                    }
                });
            }
            if (i != dragged) {
                Point xy = getCoorFromIndex(i);
                getChildAt(i).layout(xy.x, xy.y, xy.x + childSize, xy.y + childSize);
            }

        }
        //if(dragged!=-1){
        this.setVisibility(GONE);
        this.setVisibility(VISIBLE);
        //}
        //this.setBackgroundColor(this.getDrawingCacheBackgroundColor());
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (dragged == -1)
            return i;
        else if (i == childCount - 1)
            return dragged;
        else if (i >= dragged)
            return i + 1;
        return i;
    }

    public int getIndexFromCoor(int x, int y) {
        int col = getColOrRowFromCoor(x), row = getColOrRowFromCoor(y + scroll);
        if (col == -1 || row == -1) //touch is between columns or rows
            return -1;
        int index = row * colCount + col;
        if (index >= getChildCount())
            return -1;
        return index;
    }

    protected int getColOrRowFromCoor(int coor) {
        coor -= padding;
        for (int i = 0; coor > 0; i++) {
            if (coor < childSize)
                return i;
            coor -= (childSize + padding);
        }
        return -1;
    }

    protected int getHoverTargetFromCoor(int x, int y) {
        if (getColOrRowFromCoor(y + scroll) == -1) //touch is between rows
            return -1;
        //if (getIndexFromCoor(x, y) != -1) //touch on top of another visual
        // return getIndexFromCoor(x, y);

        int leftPos = getIndexFromCoor(x - (childSize / 4), y);
        int rightPos = getIndexFromCoor(x + (childSize / 4), y);
        if (leftPos == -1 && rightPos == -1) //touch is in the middle of nowhere
            return -1;
        if (leftPos == rightPos) //touch is in the middle of a visual
            return leftPos;
        else
            return -1;
    }

    protected int getTargetFromCoor(int x, int y) {
        if (getColOrRowFromCoor(y + scroll) == -1) //touch is between rows
            return -1;
        //if (getIndexFromCoor(x, y) != -1) //touch on top of another visual
        //return -1;

        int leftPos = getIndexFromCoor(x - (childSize / 4), y);
        int rightPos = getIndexFromCoor(x + (childSize / 4), y);
        if (leftPos == -1 && rightPos == -1) //touch is in the middle of nowhere
            return -1;
        if (leftPos == rightPos) //touch is in the middle of a visual
            return -1;

        int target = -1;
        if (rightPos > -1)
            target = rightPos;
        else if (leftPos > -1)
            target = leftPos + 1;
        if (dragged < target)
            return target - 1;

        //Toast.makeText(getContext(), "Target: " + target + ".", Toast.LENGTH_SHORT).show();
        return target;
    }

    protected Point getCoorFromIndex(int index) {
        int col = index % colCount;
        int row = index / colCount;
        return new Point(padding + (childSize + padding) * col,
                padding + (childSize + padding) * row - scroll);
    }

    public int getIndexOf(View child) {
        for (int i = 0; i < getChildCount(); i++)
            if (getChildAt(i) == child)
                return i;
        return -1;
    }

    //EVENT HANDLERS
    public void onClick(View view) {
        if (enabled && isTouchAllowed()) {
            if (secondaryOnClickListener != null)
                secondaryOnClickListener.onClick(view);
            if (onItemClickListener != null && getLastIndex() != -1)
                onItemClickListener.onItemClick(null, getChildAt(getLastIndex()), getLastIndex(), getLastIndex() / colCount);
        }
    }

    public boolean onLongClick(View view) {
        if (!enabled || !isTouchAllowed())
            return false;
        int index = getLastIndex();
        if (index != -1) {
            dragged = index;
            animateDragged();
            return true;
        }
        return false;
    }

    public boolean onTouch(View view, MotionEvent event) {
        if (!this.isTouchAllowed()) {
            return true; //OnTouch event has been done
        }
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                enabled = true;
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                touching = true;
                break;
            case MotionEvent.ACTION_MOVE:
                int delta = lastY - (int) event.getY();
                if (dragged != -1) {
                    //change draw location of dragged visual
                    int x = (int) event.getX(), y = (int) event.getY();
                    int l = x - (3 * childSize / 4), t = y - (3 * childSize / 4);
                    //Hover Target for grouping
                    int hoverTarget = getHoverTargetFromCoor(x, y);
                    if (hoverTarget != -1 && isGroupingAllowed()) {
                        getChildAt(dragged).layout(l + (childSize * 1 / 10), t + (childSize * 1 / 10), l + (childSize * 9 / 10), t + (childSize * 9 / 10));
                        if (lastHoverTarget != hoverTarget
                                ) {
                            if (lastHoverTarget != -1) {
                                View lastHoverView = getChildAt(lastHoverTarget);
                                if(lastHoverView.getTag()!=null) {
                                    getChildAt(lastHoverTarget).setBackgroundResource(Integer.valueOf(lastHoverView.getTag().toString()));
                                }
                            }
                            if(hoverTarget != dragged) {
                                animateDraggedHover(hoverTarget);
                            }
                            lastHoverTarget = hoverTarget;
                        }
                    } else {
                        if (lastHoverTarget != -1) {
                            View lastHoverView = getChildAt(lastHoverTarget);
                            if(lastHoverView.getTag()!=null) {
                                getChildAt(lastHoverTarget).setBackgroundResource(Integer.valueOf(lastHoverView.getTag().toString()));
                            }
                        }
                        getChildAt(dragged).layout(l, t, l + (childSize * 3 / 2), t + (childSize * 3 / 2));
                    }
                    //check for new target hover
                    int target = getTargetFromCoor(x, y);
                    if (lastTarget != target) {
                        if (target != -1) {
                            animateGap(target);
                            lastTarget = target;
                        }

                    }
                } else {
                    //Toast.makeText(this.getContext(), "no target", Toast.LENGTH_SHORT).show();
                    scroll += delta;
                    clampScroll();
                    if (Math.abs(delta) > 2)
                        enabled = false;
                    onLayout(true, getLeft(), getTop(), getRight(), getBottom());
                }
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                lastDelta = delta;
                break;
            case MotionEvent.ACTION_UP:
                if (dragged != -1) {
                    View v = getChildAt(dragged);
                    Point xy = getCoorFromIndex(dragged);
                    if (lastTarget != -1)
                        reorderChildren();
                    else {

                        v.layout(xy.x, xy.y, xy.x + childSize, xy.y + childSize);
                    }
                    v.clearAnimation();
                    int[] locations = new int[2];
                    this.getLocationOnScreen(locations);

                    if (event.getRawY() < locations[1] || event.getRawY() > locations[1] + this.getHeight()) {
                        if (this.onDraggedOutOfContainerListener != null)
                            this.onDraggedOutOfContainerListener.onDraggedOutOFContainer(dragged);
                        //Toast.makeText(this.getContext(), "Out of order", Toast.LENGTH_SHORT).show();
                    }
                    if (v instanceof ImageView)
                        ((ImageView) v).setAlpha(255);

                    if (isGroupingAllowed()) {
                        if (lastHoverTarget != dragged && this.onGroupListener != null) {
                            this.onGroupListener.onGroup(dragged, lastHoverTarget);
                        }
                    }
                    lastTarget = -1;
                    dragged = -1;
                }
                touching = false;
                break;
        }
        if (dragged != -1)
            return true;
        return false;
    }

    //EVENT HELPERS
    protected void animateDragged() {
        View v = getChildAt(dragged);
        //v.bringToFront();
        int x = getCoorFromIndex(dragged).x + childSize / 2, y = getCoorFromIndex(dragged).y + childSize / 2;
        int l = x - (3 * childSize / 4), t = y - (3 * childSize / 4);
        v.layout(l, t, l + (childSize * 3 / 2), t + (childSize * 3 / 2));
        AnimationSet animSet = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(.667f, 1, .667f, 1, childSize * 3 / 4, childSize * 3 / 4);
        scale.setDuration(animT);
        AlphaAnimation alpha = new AlphaAnimation(1, .5f);
        alpha.setDuration(animT);

        animSet.addAnimation(scale);
        animSet.addAnimation(alpha);
        animSet.setFillEnabled(true);
        animSet.setFillAfter(true);

        v.clearAnimation();
        v.startAnimation(animSet);
    }


    protected void animateDraggedHover(int currentHoverTarget) {
        View v = getChildAt(currentHoverTarget);
        v.setBackgroundResource(R.drawable.bg_dictgroup_hover);

        //v.setTag(v.getBackground());
        // FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) v.getLayoutParams();
        // v.layout(v.getLeft()-childSize * 1/10,
        //         v.getTop()-childSize * 1/10,
        //         v.getRight() + childSize * 1/10,
        //         v.getBottom()+childSize * 1/10);
    }

    protected void animateGap(int target) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (i == dragged)
                continue;
            int newPos = i;
            if (dragged < target && i >= dragged + 1 && i <= target)
                newPos--;
            else if (target < dragged && i >= target && i < dragged)
                newPos++;

            //animate
            int oldPos = i;
            if (newPositions.get(i) != -1)
                oldPos = newPositions.get(i);
            if (oldPos == newPos)
                continue;

            Point oldXY = getCoorFromIndex(oldPos);
            Point newXY = getCoorFromIndex(newPos);
            Point oldOffset = new Point(oldXY.x - v.getLeft(), oldXY.y - v.getTop());
            Point newOffset = new Point(newXY.x - v.getLeft(), newXY.y - v.getTop());

            TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x,
                    Animation.ABSOLUTE, newOffset.x,
                    Animation.ABSOLUTE, oldOffset.y,
                    Animation.ABSOLUTE, newOffset.y);
            translate.setDuration(animT);
            translate.setFillEnabled(true);
            translate.setFillAfter(true);
            v.clearAnimation();
            v.startAnimation(translate);

            newPositions.set(i, newPos);
        }
    }

    protected void reorderChildren() {
        //FIGURE OUT HOW TO REORDER CHILDREN WITHOUT REMOVING THEM ALL AND RECONSTRUCTING THE LIST!!!
        if (onRearrangeListener != null)
            onRearrangeListener.onRearrange(dragged, lastTarget);
        ArrayList<View> children = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).clearAnimation();
            children.add(getChildAt(i));
        }
        removeAllViews();
        while (dragged != lastTarget)
            if (lastTarget == children.size()) // dragged and dropped to the right of the last element
            {
                children.add(children.remove(dragged));
                dragged = lastTarget;
            } else if (dragged < lastTarget) // shift to the right
            {
                Collections.swap(children, dragged, dragged + 1);
                dragged++;
            } else if (dragged > lastTarget) // shift to the left
            {
                Collections.swap(children, dragged, dragged - 1);
                dragged--;
            }
        for (int i = 0; i < children.size(); i++) {
            newPositions.set(i, -1);
            addView(children.get(i));
        }
        onLayout(true, getLeft(), getTop(), getRight(), getBottom());
    }

    public void scrollToTop() {
        scroll = 0;
    }

    public void scrollToBottom() {
        scroll = Integer.MAX_VALUE;
        clampScroll();
    }

    protected void clampScroll() {
        int stretch = 3, overreach = getHeight() / 2;
        int max = getMaxScroll();
        max = Math.max(max, 0);

        if (scroll < -overreach) {
            scroll = -overreach;
            lastDelta = 0;
        } else if (scroll > max + overreach) {
            scroll = max + overreach;
            lastDelta = 0;
        } else if (scroll < 0) {
            if (scroll >= -stretch)
                scroll = 0;
            else if (!touching)
                scroll -= scroll / stretch;
        } else if (scroll > max) {
            if (scroll <= max + stretch)
                scroll = max;
            else if (!touching)
                scroll += (max - scroll) / stretch;
        }
    }

    protected int getMaxScroll() {
        int rowCount = (int) Math.ceil((double) getChildCount() / colCount), max = rowCount * childSize + (rowCount + 1) * padding - getHeight();
        return max;
    }

    public int getLastIndex() {
        return getIndexFromCoor(lastX, lastY);
    }

    //OTHER METHODS
    public void setOnRearrangeListener(OnRearrangeListener l) {
        this.onRearrangeListener = l;
    }

    public void setOnItemCheckedListener(OnItemCheckedListener l) {
        this.onItemCheckedListener = l;
    }

    public void setOnDraggedOutOfContainerListener(OnDraggedOutOfContainerListener l) {
        this.onDraggedOutOfContainerListener = l;
    }

    public void setOnGroupListener(OnGroupListener l) {
        this.onGroupListener = l;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.onItemClickListener = l;
    }

    public boolean isGroupingAllowed() {
        return groupingAllowed;
    }

    public void setGroupingAllowed(boolean groupingAllowed) {
        this.groupingAllowed = groupingAllowed;
    }


    public boolean isTouchAllowed() {
        return touchAllowed;
    }

    public void setTouchAllowed(boolean value) {
        this.touchAllowed = value;
    }
}
