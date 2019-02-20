/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.horcrux.svg;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.annotations.ReactProp;

@SuppressLint("ViewConstructor")
class UseView extends RenderableView {
    private String mHref;
    private SVGLength mX;
    private SVGLength mY;
    private SVGLength mW;
    private SVGLength mH;

    public UseView(ReactContext reactContext) {
        super(reactContext);
    }

    @ReactProp(name = "href")
    public void setHref(String href) {
        mHref = href;
        invalidate();
    }

    @ReactProp(name = "x")
    public void setX(Dynamic x) {
        mX = SVGLength.from(x);
        invalidate();
    }

    @ReactProp(name = "y")
    public void setY(Dynamic y) {
        mY = SVGLength.from(y);
        invalidate();
    }

    @ReactProp(name = "width")
    public void setWidth(Dynamic width) {
        mW = SVGLength.from(width);
        invalidate();
    }

    @ReactProp(name = "height")
    public void setHeight(Dynamic height) {
        mH = SVGLength.from(height);
        invalidate();
    }

    @Override
    void draw(final Canvas canvas, final GlyphContext glyphContext, final Paint paint, final float opacity) {
        VirtualView template = getSvgView().getDefinedTemplate(mHref);

        if (template != null) {
            canvas.translate((float) relativeOnWidth(glyphContext, mX), (float) relativeOnHeight(glyphContext, mY));
            if (template instanceof RenderableView) {
                ((RenderableView)template).mergeProperties(this);
            }

            int count = template.saveAndSetupCanvas(canvas);
            clip(canvas, glyphContext, paint);

            if (template instanceof SymbolView) {
                SymbolView symbol = (SymbolView)template;
                symbol.drawSymbol(canvas, glyphContext, paint, opacity, (float) relativeOnWidth(glyphContext, mW), (float) relativeOnHeight(glyphContext, mH));
            } else {
                template.draw(canvas, glyphContext, paint, opacity * mOpacity);
            }

            this.setClientRect(template.getClientRect());

            template.restoreCanvas(canvas, count);
            if (template instanceof RenderableView) {
                ((RenderableView)template).resetProperties();
            }
        } else {
            FLog.w(ReactConstants.TAG, "`Use` element expected a pre-defined svg template as `href` prop, " +
                "template named: " + mHref + " is not defined.");
        }
    }

    @Override
    int hitTest(float[] src) {
        if (!mInvertible || !mTransformInvertible) {
            return -1;
        }

        float[] dst = new float[2];
        mInvMatrix.mapPoints(dst, src);
        mInvTransform.mapPoints(dst);

        VirtualView template = getSvgView().getDefinedTemplate(mHref);
        int hitChild = template.hitTest(dst);
        if (hitChild != -1) {
            return (template.isResponsible() || hitChild != template.getId()) ? hitChild : getId();
        }

        return -1;
    }

    @Override
    Path getPath(final Canvas canvas, final GlyphContext glyphContext, final Paint paint) {
        // todo:
        return new Path();
    }
}
