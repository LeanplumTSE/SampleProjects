//
//  AppDelegate.swift
//  swiftExample
//
//  Created by Kyle Cote on 12/8/16.
//  Copyright © 2016 Leanplum. All rights reserved.
//

import UIKit
#if DEBUG
    import AdSupport
#endif
import Leanplum
import UserNotifications


var m_User: UserModel?


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    /**
     * These keys can be found by doing the following.
     * Go to -> Leanplum.com
     * Login
     * Click the dropdown for your name in the top right corner.
     * Select "App Settings"
     * Find your application on the page and on the right side selct "Keys and Settings"
     * The first tab will show the content for these keys
     **/
    private static var appID = "YOUR_APPID"
    private static var devKey = "YOUR_DEVKEY"
    private static var prodKey = "YOUR_PRODKEY"

    
    /**
    * Example variable synce with the Leanplum --> Variables Page
    **/
    static var altDisplayTextIsEnabled = LPVar.define("altDisplayTextIsEnabled", with: false)

    var window: UIWindow?


    /**
     * Things that important &  crucial for inside didFinishLaunchingWithOptions/application is launched
     * - Setup LP based on #debug or production as shown in LeanplumManager.setup()
     * - Call Leanplum.start()
     **/
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        
        //Setup your keys for LP
        #if DEBUG
            Leanplum.setDeviceId(ASIdentifierManager.shared().advertisingIdentifier.uuidString)
            Leanplum.setAppId(AppDelegate.appID,
                              withDevelopmentKey:AppDelegate.devKey)
        #else
            Leanplum.setAppId(AppDelegate.appID,
                              withProductionKey: AppDelegate.prodKey)
        #endif
        
        //Call Start
        
        Leanplum.setDeviceId("fedeDeviceID_location_001")
        
        Leanplum.start()

        /**
         * ---Push Registration ---
         * This method will simulate registering for push based on OS version
         * Things to think about when registering for push:
         * 1) Make sure you have called Leanplum.start()
         **/
        //iOS-10
        if #available(iOS 10.0, *){
            let userNotifCenter = UNUserNotificationCenter.current()
            
            userNotifCenter.requestAuthorization(options: [.badge,.alert,.sound]){ (granted,error) in
                //Handle individual parts of the granting here
            }
            UIApplication.shared.registerForRemoteNotifications()
        }
        //iOS 8-10
        else if #available(iOS 8.0, *){
            let settings = UIUserNotificationSettings.init(types: [UIUserNotificationType.alert,UIUserNotificationType.badge,UIUserNotificationType.sound],
                                                           categories: nil)
            UIApplication.shared.registerUserNotificationSettings(settings)
            UIApplication.shared.registerForRemoteNotifications()
        }
        //iOS below 8
        else{
            UIApplication.shared.registerForRemoteNotifications(matching:
                [UIRemoteNotificationType.alert,
                 UIRemoteNotificationType.badge,
                 UIRemoteNotificationType.sound])
        }        

        let frame = UIScreen.main.bounds
        window = UIWindow(frame:frame)
        
        let homeVC : UIViewController = HomeVC()
        
        if let window = self.window{
            window.rootViewController = homeVC
            window.makeKeyAndVisible()
        }
        
        
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }
    

    //Code for handling DeepLink opens
    func application(_ application: UIApplication, handleOpen url: URL) -> Bool {
        print("Handling deepLink")
        if window == Optional.none{
            let frame = UIScreen.main.bounds
            window = UIWindow(frame:frame)
        }
        
        if url.absoluteString.contains("deepLink"){
            let deepLinkVC : UIViewController = DeepLinkVC()
            if let window = self.window{
                window.rootViewController = deepLinkVC
                window.makeKeyAndVisible()
                return true
            }
        }
        return false
    }

}

