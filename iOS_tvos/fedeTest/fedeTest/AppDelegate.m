//
//  AppDelegate.m
//  fedeTest
//
//  Created by Federico Casali on 1/31/17.
//  Copyright © 2017 Leanplum. All rights reserved.
//

#import "AppDelegate.h"
#import <Leanplum/Leanplum.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
#ifdef DEBUG
    LEANPLUM_USE_ADVERTISING_ID;
    [Leanplum setAppId:@"app_lahFY0OlDNM0KMjZyWEYjs6Zm9DiAEPObc3viGhhw5g"
    withDevelopmentKey:@"dev_M4NzDeT6O7qOkde50lWGUO9U1UeAo4xKgqBjrrWbxrs"];
#else
    [Leanplum setAppId:@"app_lahFY0OlDNM0KMjZyWEYjs6Zm9DiAEPObc3viGhhw5g"
     withProductionKey:@"prod_dN3Qi0qdqHPzvwuAtFw3ntASS5SBgvpxDZ73Fqa0OEw"];
#endif
    
    [Leanplum start];
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
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


@end
