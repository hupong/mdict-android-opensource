/*
 * Copyright (C) 2012. Rayman Zhang <raymanzhang@gmail.com>
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

package cn.mdict;

import android.text.format.Time;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


/**
 * User: Rayman
 * Date: 11-12-20
 * Time: 下午3:20
 */
public class WebViewGestureFilter implements View.OnTouchListener {
    private boolean flinged;

    private static final int SWIPE_MIN_DISTANCE = 200;
    private static final int SWIPE_MAX_OFF_PATH = 80;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private GestureListener listener = null;
    private GestureDetector gd;
    private long lastPointerCountChangeTime = 0;
    private int lastPointerCount = 0;
    private int currentPointerCount = 0;
    private View targetView;

    public interface GestureListener {
        static int swipeLeft = 0;
        static int swipeRight = 1;

        //The built-in GestureDetector has problem with multi-touch gestrue recognition problem
        //So actullay the touchPointCount will always be 1 here.
        public void onSwipe(View view, int direction, int touchPointCount, MotionEvent motionEvent);

        public void onDoubleTap(View view, int touchPointCount, MotionEvent motionEvent);
    }

    public WebViewGestureFilter(View view, GestureListener gestureListener) {
        //int swipeLeft = 0;
        //int swipeRight = 0;
        this.targetView = view;
        gd = new GestureDetector(view.getContext(), sogl);
        listener = gestureListener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //String msg=String.format("Action:%d, pointer count=%d", motionEvent.getAction()&MotionEvent.ACTION_MASK, motionEvent.getPointerCount());
        //Log.d("Touch", msg);

        if (motionEvent.getPointerCount() != currentPointerCount) {
            Time now = new Time();
            now.setToNow();
            lastPointerCountChangeTime = now.toMillis(true);
            lastPointerCount = currentPointerCount;
            currentPointerCount = motionEvent.getPointerCount();
        }

        gd.onTouchEvent(motionEvent);
        return false;
/*
         if (flinged) {
             flinged = false;
             return false;
         } else {
             return view.onTouchEvent(motionEvent);
         }
*/
    }

    private int calculateTouchPointCount(MotionEvent currentEvent, boolean reset) {
        int pointerCount;
        if (currentEvent.getEventTime() - lastPointerCountChangeTime < 300 && lastPointerCount != 0) {
            pointerCount = lastPointerCount;
        } else
            pointerCount = currentEvent.getPointerCount();
        if (reset) {
            lastPointerCountChangeTime = 0;
            lastPointerCount = 0;
            currentPointerCount = 0;
        }
        return pointerCount;
    }

    public boolean doNotifySwipe(final int direction, MotionEvent event) {
        if (listener != null) {
            listener.onSwipe(targetView, direction, calculateTouchPointCount(event, true), event);
/*
            Handler handler=new Handler();
            handler.post(new Runnable(){
                public void run(){
                    listener.onSwipe(direction, touchPointCount);
                }
            });
*/
            return true;
        }
        return false;
    }

    public boolean doNotifyDoubleTap(MotionEvent event) {
        if (listener != null) {
            listener.onDoubleTap(targetView, calculateTouchPointCount(event, true), event);
            return true;
        }
        return false;
    }

    GestureDetector.SimpleOnGestureListener sogl = new GestureDetector.SimpleOnGestureListener() {
        // your fling code here
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
//            if (event1.getX() < 1200 && event1.getX() > 80) {
//                return false;
//            }
            if (Math.abs(event1.getY() - event1.getY()) < SWIPE_MAX_OFF_PATH) {
                if (event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return doNotifySwipe(GestureListener.swipeLeft, event2);
                } else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return doNotifySwipe(GestureListener.swipeRight, event2);
                }
            }
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(android.view.MotionEvent event) {
            return (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP && doNotifyDoubleTap(event);
        }

/*
        @Override
        public boolean onDown(android.view.MotionEvent event){
            return false;
        }
*/
    };
}