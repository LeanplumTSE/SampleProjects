//
//  AppDelegate.m
//  fedeTest
//
//  Created by Federico Casali on 3/7/16.
//  Copyright © 2016 Federico Casali. All rights reserved.
//

#import "AppDelegate.h"
#import <Leanplum/Leanplum.h>
#import "ViewController.h"


@interface AppDelegate ()

@end

@implementation AppDelegate


DEFINE_VAR_STRING(welcomeMessage, @"Welcome to Leanplum!");
DEFINE_VAR_FLOAT(floatvar, 1.5);
DEFINE_VAR_INT(intvalue, 20);

DEFINE_VAR_BOOL(boolCheck, true);

DEFINE_VAR_DICTIONARY_WITH_OBJECTS_AND_KEYS(
                                            powerup,
                                            @"Turbo Boost", @"name",
                                            @150, @"price",
                                            @1.5, @"speedMultiplier",
                                            @15, @"timeout",
                                            nil);


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {


//
//     IOS_app
//#ifdef DEBUG
//    LEANPLUM_USE_ADVERTISING_ID;
//    [Leanplum setAppId:@"app_lahFY0OlDNM0KMjZyWEYjs6Zm9DiAEPObc3viGhhw5g" withDevelopmentKey:@"dev_M4NzDeT6O7qOkde50lWGUO9U1UeAo4xKgqBjrrWbxrs"];
//#else
    [Leanplum setAppId:@"app_lahFY0OlDNM0KMjZyWEYjs6Zm9DiAEPObc3viGhhw5g" withProductionKey:@"prod_dN3Qi0qdqHPzvwuAtFw3ntASS5SBgvpxDZ73Fqa0OEw"];
//#endif
    
        
    
    [Leanplum onVariablesChanged:^() {
        NSLog(@"### Callback triggered");
        NSLog(@"### %@", welcomeMessage.stringValue);
        NSLog(@"### %0.1f", floatvar.floatValue);
        NSLog(@"### %d", intvalue.intValue);
        NSLog(@"### %2f", [[powerup objectForKey:@"speedMultiplier"] floatValue]);
        NSLog(@"### %d", boolCheck.intValue);
    }];
    
    
// [Leanplum allowInterfaceEditing];
// [Leanplum setVerboseLoggingInDevelopmentMode:true];
    
//    [Leanplum trackAllAppScreens];
    
    
    [Leanplum onStartResponse:^(bool success) {
        NSLog(@"### Leanplum has started!");
        NSLog(@"### onStartResponse callback triggered");
        NSLog(@"%@", [Leanplum variants]);
    }];
    
    
//    [Leanplum setDeviceId:@"12345-678910"];
//    [Leanplum setDeviceId:@"1098765-4321-9999-7-151"];

    
    
    [Leanplum start];
    
//    [Leanplum trackInAppPurchase:(SKPaymentTransaction *)];
    
    
    // Override point for customization after application launch.
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

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
{
    [Leanplum handleNotification:userInfo fetchCompletionHandler:completionHandler];
}

@end
