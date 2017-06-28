//
//  ViewController.m
//  basicSetup
//
//  Created by Federico Casali on 4/12/16.
//  Copyright © 2016 Federico Casali. All rights reserved.
//

#import "ViewController.h"
#import "AppDelegate.h"
#import <Leanplum/Leanplum.h>


@interface ViewController ()

@end

@implementation ViewController

-(void) pushButtonClicked:(UIButton*)sender
{
    NSLog(@"you clicked on Push Notification registration button");
    
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 80000
    if ([[UIApplication sharedApplication] respondsToSelector:@selector(registerUserNotificationSettings:)]) {
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound categories:nil];
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        [[UIApplication sharedApplication] registerForRemoteNotifications];
    } else {
        [[UIApplication sharedApplication] registerForRemoteNotificationTypes: UIUserNotificationTypeSound | UIUserNotificationTypeAlert | UIUserNotificationTypeBadge];
    }
#else
    [[UIApplication sharedApplication] registerForRemoteNotificationTypes: UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert | UIRemoteNotificationTypeBadge];
#endif
}



- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
 
    [Leanplum onVariablesChangedAndNoDownloadsPending:^() {
        NSLog(@"#### Leanplum callback ");
//        [self.view addSubview:imageView];
    }];
    
    
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
