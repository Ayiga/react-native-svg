/**
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import <React/RCTBridge.h>
#import <React/RCTUIManager.h>
#import "RNSVGSvgViewManager.h"
#import "RNSVGSvgView.h"

@implementation RNSVGSvgViewManager

RCT_EXPORT_MODULE()

- (UIView *)view
{
    return [RNSVGSvgView new];
}

RCT_EXPORT_VIEW_PROPERTY(bbWidth, RNSVGLength*)
RCT_EXPORT_VIEW_PROPERTY(bbHeight, RNSVGLength*)
RCT_EXPORT_VIEW_PROPERTY(minX, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(minY, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(vbWidth, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(vbHeight, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(align, NSString)
RCT_EXPORT_VIEW_PROPERTY(meetOrSlice, RNSVGVBMOS)
RCT_EXPORT_VIEW_PROPERTY(tintColor, UIColor)

RCT_EXPORT_METHOD(toDataURL:(nonnull NSNumber *)reactTag callback:(RCTResponseSenderBlock)callback)
{
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        __kindof UIView *view = viewRegistry[reactTag];
        if ([view isKindOfClass:[RNSVGSvgView class]]) {
            RNSVGSvgView *svg = view;
            callback(@[[svg getDataURL]]);
        } else {
            RCTLogError(@"Invalid svg returned frin registry, expecting RNSVGSvgView, got: %@", view);
        }
    }];
}

RCT_EXPORT_METHOD(toDataURLWithOptions:(nonnull NSNumber *)reactTag options:(nonnull NSDictionary *)options callback:(RCTResponseSenderBlock)callback)
{
    CGFloat width = [options[@"width"] floatValue];
    CGFloat height = [options[@"height"] floatValue];
    CGFloat scale = [options[@"scale"] floatValue];
    
    if (scale < 0) {
        scale = 1;
    }
    
    [self.bridge.uiManager addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *,UIView *> *viewRegistry) {
        __kindof UIView *view = viewRegistry[reactTag];
        if ([view isKindOfClass:[RNSVGSvgView class]]) {
            RNSVGSvgView *svg = view;
            callback(@[[svg getDataURLWithRect:CGRectMake(0, 0, width, height) withScale:scale]]);
        } else {
            RCTLogError(@"Invalid svg returned frin registry, expecting RNSVGSvgView, got: %@", view);
        }
    }];
}

@end
