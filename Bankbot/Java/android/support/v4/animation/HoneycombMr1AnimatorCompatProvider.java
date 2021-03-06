package android.support.v4.animation;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;

class HoneycombMr1AnimatorCompatProvider implements AnimatorProvider {
    private TimeInterpolator mDefaultInterpolator;

    class AnimatorListenerCompatWrapper implements AnimatorListener {
        final ValueAnimatorCompat mValueAnimatorCompat;
        final AnimatorListenerCompat mWrapped;

        public AnimatorListenerCompatWrapper(AnimatorListenerCompat animatorListenerCompat, ValueAnimatorCompat valueAnimatorCompat) {
            this.mWrapped = animatorListenerCompat;
            this.mValueAnimatorCompat = valueAnimatorCompat;
        }

        public void onAnimationCancel(Animator animator) {
            this.mWrapped.onAnimationCancel(this.mValueAnimatorCompat);
        }

        public void onAnimationEnd(Animator animator) {
            this.mWrapped.onAnimationEnd(this.mValueAnimatorCompat);
        }

        public void onAnimationRepeat(Animator animator) {
            this.mWrapped.onAnimationRepeat(this.mValueAnimatorCompat);
        }

        public void onAnimationStart(Animator animator) {
            this.mWrapped.onAnimationStart(this.mValueAnimatorCompat);
        }
    }

    class HoneycombValueAnimatorCompat implements ValueAnimatorCompat {
        final Animator mWrapped;

        public HoneycombValueAnimatorCompat(Animator animator) {
            this.mWrapped = animator;
        }

        public void addListener(AnimatorListenerCompat animatorListenerCompat) {
            this.mWrapped.addListener(new AnimatorListenerCompatWrapper(animatorListenerCompat, this));
        }

        public void addUpdateListener(final AnimatorUpdateListenerCompat animatorUpdateListenerCompat) {
            if (this.mWrapped instanceof ValueAnimator) {
                ((ValueAnimator) this.mWrapped).addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        animatorUpdateListenerCompat.onAnimationUpdate(HoneycombValueAnimatorCompat.this);
                    }
                });
            }
        }

        public void cancel() {
            this.mWrapped.cancel();
        }

        public float getAnimatedFraction() {
            return ((ValueAnimator) this.mWrapped).getAnimatedFraction();
        }

        public void setDuration(long j) {
            this.mWrapped.setDuration(j);
        }

        public void setTarget(View view) {
            this.mWrapped.setTarget(view);
        }

        public void start() {
            this.mWrapped.start();
        }
    }

    HoneycombMr1AnimatorCompatProvider() {
    }

    public void clearInterpolator(View view) {
        if (this.mDefaultInterpolator == null) {
            this.mDefaultInterpolator = new ValueAnimator().getInterpolator();
        }
        view.animate().setInterpolator(this.mDefaultInterpolator);
    }

    public ValueAnimatorCompat emptyValueAnimator() {
        return new HoneycombValueAnimatorCompat(ValueAnimator.ofFloat(new float[]{0.0f, 1.0f}));
    }
}
