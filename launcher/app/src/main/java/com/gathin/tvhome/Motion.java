package com.gathin.tvhome;

import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/** Shared timing for focus and titles: opacity and transform, short, no bounce. */
final class Motion {

    /** A focus change. Long enough to read, short enough not to lag the remote. */
    static final int FAST_MS = 180;
    /** A title or panel arriving. */
    static final int NORMAL_MS = 320;
    /** The longest any reduced-motion animation runs, in milliseconds. */
    static final int REDUCED_MS = 90;
    /** How far a focused tile lifts, in dp. */
    static final float LIFT_DP = 3f;
    /** Ease-out that settles without a bounce: cubic-bezier(0.22, 1, 0.36, 1). */
    static final Interpolator SMOOTH = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
    /** Critically damped (no bounce) and stiff enough that any move, even across the screen, settles in 5 frames. */
    static final float REDUCED_STIFFNESS = 15000f;
    /** The spring stops once it is within this many pixels of its target. */
    static final float REDUCED_MIN_CHANGE = 2f;

    private Motion() {
    }

    static void reduce(SpringAnimation spring) {
        spring.setSpring(new SpringForce().setStiffness(REDUCED_STIFFNESS).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY));
        spring.setMinimumVisibleChange(REDUCED_MIN_CHANGE);
    }
}
