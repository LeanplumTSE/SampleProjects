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

UITextField *loginField;


- (IBAction)buttonAction:(id)sender {
    [Leanplum setUserId:self.loginField.text];
    NSLog(@"Setting UserID");
    
//    [self presentViewController:secondViewController animated:YES completion:nil];
}



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


- (void)trackEventButtonClicked:(UIButton*)sender
{
    NSLog(@"you clicked on track event button");
    [Leanplum track:@"Button Click Event" withParameters:@{@"sampleParameter": @"1234" }];
    }

- (void) userAttributeButton:(UIButton*)sender
{
    NSLog(@"you clicked on the set user Attributes button");
    [Leanplum setUserAttributes:@{@"gender": @"Female", @"age": @36}];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    
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
    
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
