//
//  AppDelegate.m
//  basicSetup
//
//  Created by Federico Casali on 4/12/16.
//  Copyright © 2016 Federico Casali. All rights reserved.
//

#import "AppDelegate.h"
#import <LeanplumSegment/SEGLeanplumIntegrationFactory.h>



@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
    

    NSString *const SEGMENT_WRITE_KEY = @"YOUR_SEGMENT_KEY";
    SEGAnalyticsConfiguration *config = [SEGAnalyticsConfiguration configurationWithWriteKey:SEGMENT_WRITE_KEY];
    [config use:[SEGLeanplumIntegrationFactory instance]];
    [SEGAnalytics setupWithConfiguration:config];
    
    
    //
   
    [Leanplum onStartResponse:^(bool success) {
//         Otherwise use boilerplate code from docs.
            id notificationCenterClass = NSClassFromString(@"UNUserNotificationCenter");
            if (notificationCenterClass) {
                // iOS 10.
                SEL selector = NSSelectorFromString(@"currentNotificationCenter");
                id notificationCenter =
                ((id (*)(id, SEL)) [notificationCenterClass methodForSelector:selector])
                (notificationCenterClass, selector);
                if (notificationCenter) {
                    selector = NSSelectorFromString(@"requestAuthorizationWithOptions:completionHandler:");
                    IMP method = [notificationCenter methodForSelector:selector];
                    void (*func)(id, SEL, unsigned long long, void (^)(BOOL, NSError *__nullable)) =
                    (void *) method;
                    func(notificationCenter, selector,
                         0b111, /* badges, sounds, alerts */
                         ^(BOOL granted, NSError *__nullable error) {
                             if (error) {
                                 NSLog(@"Leanplum: Failed to request authorization for user "
                                       "notifications: %@", error);
                             }
                         });
                }
                [[UIApplication sharedApplication] registerForRemoteNotifications];
            } else if ([[UIApplication sharedApplication] respondsToSelector:
                        @selector(registerUserNotificationSettings:)]) {
                // iOS 8-9.
                UIUserNotificationSettings *settings = [UIUserNotificationSettings
                                                        settingsForTypes:UIUserNotificationTypeAlert |
                                                        UIUserNotificationTypeBadge |
                                                        UIUserNotificationTypeSound categories:nil];
                [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
                [[UIApplication sharedApplication] registerForRemoteNotifications];
            } else {
                // iOS 7 and below.
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Wdeprecated-declarations"
                [[UIApplication sharedApplication] registerForRemoteNotificationTypes:
        #pragma clang diagnostic pop
                 UIUserNotificationTypeSound | UIUserNotificationTypeAlert | UIUserNotificationTypeBadge];
            }
    }];
    
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


//(Optional) You may choose to enable the Remote Notifications background mode on iOS 7+ to preload the notification action. This is configurable in your XCode project settings > Capabilities > Background Modes. If you have this enabled, you must tell Leanplum to explicitly handle notifications in your app delegate:

//- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
//{
//    [Leanplum handleNotification:userInfo fetchCompletionHandler:completionHandler];
//}

- (void) application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(nonnull NSData *)deviceToken {
    NSLog(@"### token callback");
}

@end
