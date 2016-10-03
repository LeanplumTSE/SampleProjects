//
//  AppDelegate.h
//  Leanplum_mParticle_CocoaPods
//
//  Created by Sophie Saouma on 9/24/16.
//  Copyright © 2016 Sophie Saouma. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#if defined(__has_include) && __has_include(<mParticle_Apple_SDK/mParticle.h>)
#import <mParticle_Apple_SDK/mParticle.h>
#else
#import "mParticle.h"
#endif
#import "ViewControllerSecond.h"

@class ViewControllerSecond; 

@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) UIViewController *viewController;

@end

