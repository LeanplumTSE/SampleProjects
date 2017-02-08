//
//  AppDelegate.m
//  Leanplum_mParticle_CocoaPods
//
//  Created by Sophie Saouma on 9/24/16.
//  Copyright © 2016 Sophie Saouma. All rights reserved.
//

#import "AppDelegate.h"
//#import "ViewControllerSecond.h"
#import <Leanplum/Leanplum.h>


@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    // Override point for customization after application launch.
    self.viewController = [[ViewControllerSecond alloc] initWithNibName:@"ViewControllerSecond" bundle:nil];
    self.window.rootViewController = self.viewController;
    [self.window makeKeyAndVisible];
    
    
    // Override point for customization after application launch.
    [[MParticle sharedInstance] startWithKey:@"04780a3595efa74fbe58dca6f7161f96" secret:@"lAYFYw6LEsj7ZJIizaxfvrLvHIOo9w-0NxopiUXIqc4Gh8J2b27ffzXDeRAAVi1T"];
    
    [Leanplum trackAllAppScreens]
    
    [Leanplum onStartResponse:^(bool success) {
        [Leanplum track:@"LeanplumLoaded"]; 
    }];
    
    return YES;
}


- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
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
