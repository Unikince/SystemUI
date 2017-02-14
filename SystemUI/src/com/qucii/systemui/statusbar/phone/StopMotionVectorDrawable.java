
package com.qucii.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;

/***
 * @Description:
 * @csdnblog   
 * @author  mare
 * @date 2016-11-1
 * @time  pm 12:48:39
 */
public class StopMotionVectorDrawable extends DrawableWrapper {
    private static final String TAG = StopMotionVectorDrawable.class.getSimpleName();

    private AnimatedVectorDrawable mDrawable;
    private AnimatorSet mAnimatorSet;

    /**
     * Creates a new wrapper around the specified drawable.
     *
     * @param dr The drawable to wrap.  Must be an {@link AnimatedVectorDrawable}
     */
    public StopMotionVectorDrawable(Drawable dr) {
        super(dr);
        setDrawable(dr);
    }

    /**
     * {@see DrawableWrapper$setDrawable}
     * @param dr the wrapped drawable
     * @throws IllegalArgumentException IF drawable is not an {@link AnimatedVectorDrawable}
     */
    @Override
    public void setDrawable(Drawable dr) {
        if (dr != null && !(dr instanceof AnimatedVectorDrawable)) {
            throw new IllegalArgumentException("Drawable must be an AnimatedVectorDrawable");
        }

        super.setDrawable(dr);
        mDrawable = (AnimatedVectorDrawable) dr;
        if (mDrawable != null) {
            mDrawable.reset();
            getAnimatorSetViaReflection();
        }
    }

    /**
     * {@see android.animation.ValueAnimator#setCurrentFraction}
     * @param fraction The fraction to which the animation is advanced or rewound. Values outside
     *                 the range of 0 to the maximum fraction for the animator will be clamped to
     *                 the correct range.
     */
    public void setCurrentFraction(float fraction) {
        if (mDrawable == null || mAnimatorSet == null) return;

        ArrayList<Animator> animators = mAnimatorSet.getChildAnimations();
        for (Animator animator : animators) {
            if (animator instanceof ValueAnimator) {
                ((ValueAnimator) animator).setCurrentFraction(fraction);
            }
        }

        mDrawable.invalidateSelf();
    }

    private void getAnimatorSetViaReflection() {
        try {
            Field _mAnimatorSet = AnimatedVectorDrawable.class.getDeclaredField("mAnimatorSet");
            _mAnimatorSet.setAccessible(true);
            mAnimatorSet = (AnimatorSet) _mAnimatorSet.get(mDrawable);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Could not get mAnimatorSet via reflection", e);
        }
    }
}
