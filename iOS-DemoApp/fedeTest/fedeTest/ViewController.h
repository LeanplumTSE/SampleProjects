//
//  ViewController.h
//  fedeTest
//
//  Created by Federico Casali on 3/7/16.
//  Copyright © 2016 Federico Casali. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SecondViewController.h"

@interface ViewController : UIViewController
{
    SecondViewController *secondViewController;
}

//@property (strong, nonatomic) UIWindow *window2;

- (IBAction)buttonAction:(id)sender;

@end

