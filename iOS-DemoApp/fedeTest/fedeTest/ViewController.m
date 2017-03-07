//
//  ViewController.m
//  fedeTest
//
//  Created by Federico Casali on 3/7/16.
//  Copyright © 2016 Federico Casali. All rights reserved.
//

#import "ViewController.h"
#import <Leanplum/Leanplum.h>

@interface ViewController ()

@property (strong, nonatomic) IBOutlet UITextField *loginField;

@end


@implementation ViewController

//DEFINE_VAR_FILE(bass, @"darthbass.jpg");
DEFINE_VAR_STRING(sampleText, @"This is some text we can change from the Dashboard!");

// Defining prices for the Purchase event
double priceDouble = 1.999;
float priceFloat = 1.01;
NSString *value = @"6.99";



UITextField *loginField;


- (IBAction)buttonAction:(id)sender {
    [Leanplum setUserId:self.loginField.text];
    NSLog(@"Setting UserID");
    
    [Leanplum track:@"loggedin"];
    
    [self presentViewController:secondViewController animated:YES completion:nil];
}



-(void) pushButtonClicked:(UIButton*)sender
{
    NSLog(@"you clicked on Push Notification registration button");
    
    // Otherwise use boilerplate code from docs.
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
}


- (void)trackEventButtonClicked:(UIButton*)sender
{
    NSLog(@"you clicked on track event button");
    [Leanplum track:@"Event test!"];
    
    // Sample custom purchase event
    //   [Leanplum track:@"logPurchase" withValue:value.doubleValue andParameters:@{@"items-count": @234.5, @"promo-code" : @"stringaTest"}];

    }

- (void) userAttributeButton:(UIButton*)sender
{
    NSLog(@"you clicked on the set user Attributes button");
    [Leanplum setUserAttributes:@{@"email": @"federico@leanplum.com", @"ZIPcode": @94107}];
    
//    [Leanplum forceContentUpdate:^{
//        NSLog(@"force content update DONE!");
//    }];

//    NSArray* array = [NSArray arrayWithObjects: @"indian", @"italian", nil];
//    [Leanplum setUserAttributes:@{@"favorite foods": array}];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
    secondViewController = [[SecondViewController alloc] initWithNibName:@"SecondViewController" bundle:nil];
    
    UIButton *pushBut = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [pushBut addTarget:self action:@selector(pushButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [pushBut setFrame:CGRectMake(30, 290, 150, 30)];
    [pushBut setTitle:@"Register for Push" forState:UIControlStateNormal];
    [pushBut setExclusiveTouch:YES];
    [self.view addSubview:pushBut];
    
    UIButton *trackBut = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [trackBut addTarget:self action:@selector(trackEventButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [trackBut setFrame:CGRectMake(30, 340, 150, 30)];
    [trackBut setTitle:@"Track event" forState:UIControlStateNormal];
    [trackBut setExclusiveTouch:YES];
    [self.view addSubview:trackBut];
    
    UIButton *setUserAttributesBut = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [setUserAttributesBut addTarget:self action:@selector(userAttributeButton:) forControlEvents:UIControlEventTouchUpInside];
    [setUserAttributesBut setFrame:CGRectMake(30, 390, 150, 30)];
    [setUserAttributesBut setTitle:@"SetUserAttributes" forState:UIControlStateNormal];
    [setUserAttributesBut setExclusiveTouch:YES];
    [self.view addSubview:setUserAttributesBut];
    
    UITextView *myTextView = [[UITextView alloc]initWithFrame: CGRectMake(30, 460, 400, 400)];
    myTextView.editable = NO;
    [self.view addSubview:myTextView];
    
   
//    UIImageView *imageView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"darthbass.jpg"]];
//    [self.view addSubview:imageView];
    
    
    [Leanplum onVariablesChanged:^{
        [myTextView setText:sampleText.stringValue];
    }];
    
    [Leanplum setUserAttributes:@{@"isLoggedIn": @"False"}];
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
