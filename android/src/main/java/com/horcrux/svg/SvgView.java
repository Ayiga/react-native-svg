/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.horcrux.svg;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.View;
import android.view.ViewParent;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.ReactCompoundView;
import com.facebook.react.uimanager.ReactCompoundViewGroup;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.view.ReactViewGroup;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Custom {@link View} implementation that draws an RNSVGSvg React view and its children.
 */
@SuppressLint("ViewConstructor")
public class SvgView extends ReactViewGroup implements ReactCompoundView, ReactCompoundViewGroup {

    @Override
    public boolean interceptsTouchEvent(float touchX, float touchY) {
        return true;
    }

    @SuppressWarnings("unused")
    public enum Events {
        EVENT_DATA_URL("onDataURL");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        public String toString() {
            return mName;
        }
    }

    private @Nullable Bitmap mBitmap;

    public SvgView(ReactContext reactContext) {
        super(reactContext);
//        mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        SvgViewManager.setSvgView(id, this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        ViewParent parent = getParent();
        if (parent instanceof VirtualView) {
            if (!mRendered) {
                return;
            }
            mRendered = false;
            ((VirtualView) parent).getSvgView().invalidate();
            return;
        }
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getParent() instanceof VirtualView) {
            return;
        }
        super.onDraw(canvas);
        if (mBitmap == null) {
            mBitmap = drawOutput();
        }
        if (mBitmap != null)
            canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.invalidate();
    }

    @Override
    public int reactTagForTouch(float touchX, float touchY) {
        return hitTest(touchX, touchY);
    }

    private boolean mResponsible = false;

    private final Map<String, VirtualView> mDefinedClipPaths = new HashMap<>();
    private final Map<String, VirtualView> mDefinedTemplates = new HashMap<>();
    private final Map<String, VirtualView> mDefinedMasks = new HashMap<>();
    private final Map<String, Brush> mDefinedBrushes = new HashMap<>();
    private Canvas mCanvas;
//    private final float mScale;

    private float mMinX;
    private float mMinY;
    private float mVbWidth;
    private float mVbHeight;
    private SVGLength mbbWidth;
    private SVGLength mbbHeight;
    private String mAlign;
    private int mMeetOrSlice;
    private final Matrix mInvViewBoxMatrix = new Matrix();
    private boolean mInvertible = true;
    private boolean mRendered = false;
    int mTintColor = 0;

    private void clearChildCache() {
        if (!mRendered) {
            return;
        }
        mRendered = false;
        for (int i = 0; i < getChildCount(); i++) {
            View node = getChildAt(i);
            if (node instanceof VirtualView) {
                VirtualView n = ((VirtualView)node);
                n.clearChildCache();
            }
        }
    }

    @ReactProp(name = "tintColor", customType = "Color")
    public void setTintColor(@Nullable Integer tintColor) {
        if (tintColor == null) {
            mTintColor = 0;
        } else {
            mTintColor = tintColor;
        }
    }

    @ReactProp(name = "minX")
    public void setMinX(float minX) {
        mMinX = minX;
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "minY")
    public void setMinY(float minY) {
        mMinY = minY;
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "vbWidth")
    public void setVbWidth(float vbWidth) {
        mVbWidth = vbWidth;
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "vbHeight")
    public void setVbHeight(float vbHeight) {
        mVbHeight = vbHeight;
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "bbWidth")
    public void setBbWidth(Dynamic bbWidth) {
        mbbWidth = SVGLength.from(bbWidth);
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "bbHeight")
    public void setBbHeight(Dynamic bbHeight) {
        mbbHeight = SVGLength.from(bbHeight);
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "align")
    public void setAlign(String align) {
        mAlign = align;
        invalidate();
        clearChildCache();
    }

    @ReactProp(name = "meetOrSlice")
    public void setMeetOrSlice(int meetOrSlice) {
        mMeetOrSlice = meetOrSlice;
        invalidate();
        clearChildCache();
    }

    private Bitmap drawOutput() {
        mRendered = true;
        final float scale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
        final float width = getWidth() / scale;
        final float height = getHeight() / scale;
        boolean invalid = Float.isNaN(width) || Float.isNaN(height) || width < 1 || height < 1 || (Math.log10(width) + Math.log10(height) > 42);
        if (invalid) {
            return null;
        }

        final Matrix m = new Matrix();
        m.setScale(scale, scale);
//        final Matrix i = new Matrix();
//        m.invert(i);
        final RectF src = new RectF(0, 0, width, height);
        final RectF dst = new RectF(0, 0, 0, 0);

        m.mapRect(dst, src);


        final Bitmap bitmap = Bitmap.createBitmap(
                (int) dst.width(),
                (int) dst.height(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.concat(m);
        drawChildren(canvas, new GlyphContext(1f, width, height));
        return bitmap;
    }

    Rect getCanvasBounds() {
        return mCanvas.getClipBounds();
    }

    void drawChildren(final Canvas canvas, final GlyphContext glyphContext) {
        mRendered = true;
        mCanvas = canvas;
        if (mAlign != null) {
            RectF vbRect = getViewBox();
            float width = canvas.getWidth();
            float height = canvas.getHeight();
            boolean nested = getParent() instanceof VirtualView;
            if (nested) {
                width = (float) PropHelper.fromRelative(mbbWidth, width, 0f, 1f, 12);
                height = (float) PropHelper.fromRelative(mbbHeight, height, 0f, 1f, 12);
            }
            RectF eRect = new RectF(0,0, width, height);
            if (nested) {
                canvas.clipRect(eRect);
            }
            Matrix mViewBoxMatrix = ViewBox.getTransform(vbRect, eRect, mAlign, mMeetOrSlice);
            mInvertible = mViewBoxMatrix.invert(mInvViewBoxMatrix);
            canvas.concat(mViewBoxMatrix);
        }

        final Paint paint = new Paint();

        paint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

        paint.setTypeface(Typeface.DEFAULT);


        for (int i = 0; i < getChildCount(); i++) {
            View node = getChildAt(i);
            if (node instanceof VirtualView) {
                ((VirtualView)node).saveDefinition();
            }
        }

        for (int i = 0; i < getChildCount(); i++) {
            View lNode = getChildAt(i);
            if (lNode instanceof VirtualView) {
                VirtualView node = (VirtualView)lNode;
                int count = node.saveAndSetupCanvas(canvas);
                node.render(canvas, glyphContext, paint, 1f);
                node.restoreCanvas(canvas, count);

                if (node.isResponsible() && !mResponsible) {
                    mResponsible = true;
                }
            }
        }
    }

    private RectF getViewBox() {
        return new RectF(mMinX * 1f, mMinY * 1f, (mMinX + mVbWidth) * 1f, (mMinY + mVbHeight) * 1f);
    }

    String toDataURL() {
        final float scale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
        final int width = (int) ((float) getWidth() / scale);
        final int height = (int) ((float) getHeight() / scale);

        final Matrix m = new Matrix();
        m.setScale(scale, scale);
//        final Matrix i = new Matrix();
//        m.invert(i);
        final RectF src = new RectF(0, 0, width, height);
        final RectF dst = new RectF(0, 0, 0, 0);

        m.mapRect(dst, src);

        Bitmap bitmap = Bitmap.createBitmap(
                (int) dst.width(),
                (int) dst.height(),
                Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(bitmap);
        canvas.concat(m);

        final GlyphContext ctx = new GlyphContext(1f, width, height);

        drawChildren(canvas, ctx);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bitmap.recycle();
        byte[] bitmapBytes = stream.toByteArray();
        return Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
    }

    String toDataURL(final int width, final int height, final float scale) {
        final Matrix m = new Matrix();
        final float finalScale;
        if (scale < 0) {
            finalScale = 1;
        } else if (scale == 0) {
            finalScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
        } else {
            finalScale = scale;
        }
        m.setScale(finalScale, finalScale);
        final RectF src = new RectF(0, 0, width, height);
        final RectF dst = new RectF(0, 0, 0, 0);

        m.mapRect(dst, src);

        Bitmap bitmap = Bitmap.createBitmap(
                (int) dst.width(),
                (int) dst.height(),
                Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(bitmap);
        canvas.concat(m);

        final GlyphContext ctx = new GlyphContext(1f, width, height);

        drawChildren(canvas, ctx);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bitmap.recycle();
        byte[] bitmapBytes = stream.toByteArray();
        return Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
    }

    void enableTouchEvents() {
        if (!mResponsible) {
            mResponsible = true;
        }
    }

    boolean isResponsible() {
        return mResponsible;
    }

    private int hitTest(float touchX, float touchY) {
        if (!mResponsible || !mInvertible) {
            return getId();
        }

        float[] transformed = { touchX, touchY };
        mInvViewBoxMatrix.mapPoints(transformed);

        int count = getChildCount();
        int viewTag = -1;
        for (int i = count - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child instanceof VirtualView) {
                viewTag = ((VirtualView) child).hitTest(transformed);
            } else if (child instanceof SvgView) {
                viewTag = ((SvgView) child).hitTest(touchX, touchY);
            }
            if (viewTag != -1) {
                break;
            }
        }

        return viewTag == -1 ? getId() : viewTag;
    }

    void defineClipPath(VirtualView clipPath, String clipPathRef) {
        mDefinedClipPaths.put(clipPathRef, clipPath);
    }

    VirtualView getDefinedClipPath(String clipPathRef) {
        return mDefinedClipPaths.get(clipPathRef);
    }

    void defineTemplate(VirtualView template, String templateRef) {
        mDefinedTemplates.put(templateRef, template);
    }

    VirtualView getDefinedTemplate(String templateRef) {
        return mDefinedTemplates.get(templateRef);
    }

    void defineBrush(Brush brush, String brushRef) {
        mDefinedBrushes.put(brushRef, brush);
    }

    Brush getDefinedBrush(String brushRef) {
        return mDefinedBrushes.get(brushRef);
    }

    void defineMask(VirtualView mask, String maskRef) {
        mDefinedMasks.put(maskRef, mask);
    }

    VirtualView getDefinedMask(String maskRef) {
        return mDefinedMasks.get(maskRef);
    }
}
