//
//  AppDelegate.h
//  fedeTest
//
//  Created by Federico Casali on 7/31/17.
//  Copyright © 2017 com.leanplum. All rights reserved.
//

#import <UIKit/UIKit.h>

//Add Framework in your project "UserNotifications"
#import <UserNotifications/UserNotifications.h>
@interface AppDelegate : UIResponder <UIApplicationDelegate,UNUserNotificationCenterDelegate>

@property (strong, nonatomic) UIWindow *window;


@end

