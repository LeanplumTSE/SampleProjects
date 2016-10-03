//
//  ViewController.m
//  Leanplum_mParticle_CocoaPods
//
//  Created by Sophie Saouma on 9/24/16.
//  Copyright © 2016 Sophie Saouma. All rights reserved.
//

#import "ViewController.h"
#import <Leanplum/Leanplum.h>


@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    NSLog(@"GluedToTheTv"); 
    [self.setUser setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
    //[Leanplum setUserId:@"ExampleUser"];
    // Do any additional setup after loading the view, typically from a nib.
}

-(IBAction)setUserIdDidPress:(id)sender {
    
    //[Leanplum track:@"SettingUserIdAction"];
    NSLog(@"WHAT");
    
    //[Leanplum setUserId:@"ExampleUserId"];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


@end
