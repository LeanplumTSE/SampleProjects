//
//  MPKitLeanplum.m
//
//  Copyright 2016 mParticle, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#import "MPKitLeanplum.h"
#import "mParticle.h"
#import "MPKitRegister.h"
#import "MPEnums.h"
#import "MPIConstants.h"

#import "Leanplum.h"

@implementation MPKitLeanplum

+ (NSNumber *)kitCode {
    return @98;
}

+ (void)load {
    MPKitRegister *kitRegister = [[MPKitRegister alloc] initWithName:@"Leanplum" className:@"MPKitLeanplum" startImmediately:NO];
    [MParticle registerExtension:kitRegister];
}

#pragma mark - MPKitInstanceProtocol methods

#pragma mark Kit instance and lifecycle
- (nonnull instancetype)initWithConfiguration:(nonnull NSDictionary *)configuration startImmediately:(BOOL)startImmediately {
    self = [super init];
    NSString *appId = configuration[@"appId"];
    NSString *clientKey = configuration[@"clientKey"];
    NSString *userIdField = configuration[@"userIdField"];
    if (!self || !appId || !clientKey || !userIdField) {
        return nil;
    }

    _configuration = configuration;

    if (startImmediately) {
        [self start];
    }

    return self;
}

- (MPUserIdentity)preferredIdentityType {
    NSString *userIdField = self.configuration[@"userIdField"];
    if ([userIdField isEqual:@"customerId"]) {
        return MPUserIdentityCustomerId;
    }
    else {
        return MPUserIdentityEmail;
    }
}

- (void)start {
    static dispatch_once_t kitPredicate;

    dispatch_once(&kitPredicate, ^{

        if ([MParticle sharedInstance].environment == MPEnvironmentDevelopment) {
            LEANPLUM_USE_ADVERTISING_ID;
            [Leanplum setAppId:self.configuration[@"appId"] withDevelopmentKey:self.configuration[@"clientKey"]];
        }
        else {
            [Leanplum setAppId:self.configuration[@"appId"] withProductionKey:self.configuration[@"clientKey"]];
        }

        NSString *userId = nil;
        for (NSDictionary<NSString *, id> *userIdentity in self.userIdentities) {
            MPUserIdentity identityType = (MPUserIdentity)[userIdentity[kMPUserIdentityTypeKey] integerValue];
            NSString *identityString = userIdentity[kMPUserIdentityIdKey];

            if (identityType == [self preferredIdentityType]) {
                userId = identityString;
                break;
            }
        }

        NSDictionary<NSString *, id> *attributes = self.userAttributes;

        if (userId && attributes) {
            [Leanplum startWithUserId:userId userAttributes:attributes];
        }
        else if (attributes) {
            [Leanplum startWithUserAttributes:attributes];
        }
        else if (userId) {
            [Leanplum startWithUserId:userId];
        }
        else {
            [Leanplum start];
        }

        _started = YES;

        dispatch_async(dispatch_get_main_queue(), ^{
            NSDictionary *userInfo = @{mParticleKitInstanceKey:[[self class] kitCode]};

            [[NSNotificationCenter defaultCenter] postNotificationName:mParticleKitDidBecomeActiveNotification
                                                                object:nil
                                                              userInfo:userInfo];
        });
    });
}

- (id const)kitInstance {
    return nil;
}


#pragma mark Application
- (MPKitExecStatus *)handleActionWithIdentifier:(NSString *)identifier forRemoteNotification:(NSDictionary *)userInfo {
    [Leanplum handleActionWithIdentifier:identifier forRemoteNotification:userInfo completionHandler:^{
    }];

    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

#pragma mark User attributes and identities
- (MPKitExecStatus *)setUserAttribute:(NSString *)key value:(NSString *)value {
    NSDictionary *attributes = @{key: value};
    [Leanplum setUserAttributes:attributes];
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

- (MPKitExecStatus *)removeUserAttribute:(NSString *)key {
    [Leanplum startWithUserAttributes:@{key: [NSNull null]}];
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

- (MPKitExecStatus *)setUserIdentity:(NSString *)identityString identityType:(MPUserIdentity)identityType {
    MPKitExecStatus *execStatus;
    if (identityType == [self preferredIdentityType]) {
        [Leanplum setUserId:identityString];
        execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    }
    else {
        execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeUnavailable];
    }

    return execStatus;
}

#pragma mark e-Commerce
- (MPKitExecStatus *)logCommerceEvent:(MPCommerceEvent *)commerceEvent {
    
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess forwardCount:0];
    
    if (commerceEvent.type == MPEventTypePurchase) {
        NSMutableDictionary *baseProductAttributes = [[NSMutableDictionary alloc] init];
        NSDictionary *transactionAttributes = [commerceEvent.transactionAttributes beautifiedDictionaryRepresentation];
        
        if (transactionAttributes) {
            [baseProductAttributes addEntriesFromDictionary:transactionAttributes];
        }
        
        NSDictionary *commerceEventAttributes = [commerceEvent beautifiedAttributes];
        NSArray *keys = @[kMPExpCECheckoutOptions, kMPExpCECheckoutStep, kMPExpCEProductListName, kMPExpCEProductListSource];
        
        for (NSString *key in keys) {
            if (commerceEventAttributes[key]) {
                baseProductAttributes[key] = commerceEventAttributes[key];
            }
        }
        
        NSArray *products = commerceEvent.products;
        NSMutableDictionary *properties;
        
        for (MPProduct *product in products) {
            // Add relevant attributes from the commerce event
            properties = [[NSMutableDictionary alloc] init];
            if (baseProductAttributes.count > 0) {
                [properties addEntriesFromDictionary:baseProductAttributes];
            }
            
            // Add attributes from the product itself
            NSDictionary *productDictionary = [product beautifiedDictionaryRepresentation];
            if (productDictionary) {
                [properties addEntriesFromDictionary:productDictionary];
            }
            
            // Strips key/values already being passed, plus key/values initialized to default values
            keys = @[kMPProductAffiliation, kMPExpProductCategory, kMPExpProductName];
            [properties removeObjectsForKeys:keys];
            
            double value = [product.price doubleValue] * [product.quantity doubleValue];
            
            [Leanplum track:LP_PURCHASE_EVENT withValue:value andInfo:product.name andParameters:properties];
            [execStatus incrementForwardCount];
        }
    } else {
        NSArray *expandedInstructions = [commerceEvent expandedInstructions];
        
        for (MPCommerceEventInstruction *commerceEventInstruction in expandedInstructions) {
            [self logEvent:commerceEventInstruction.event];
            [execStatus incrementForwardCount];
        }
    }

    return execStatus;
}

- (MPKitExecStatus *)logLTVIncrease:(double)increaseAmount event:(MPEvent *)event {
    [Leanplum track:event.name withValue:increaseAmount andParameters:event.info];
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

- (MPKitExecStatus *)receivedUserNotification:(NSDictionary *)userInfo {
    [Leanplum handleActionWithIdentifier:nil forRemoteNotification:userInfo completionHandler:^{
    }];

    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

#pragma mark Events

- (MPKitExecStatus *)logEvent:(MPEvent *)event {
    [Leanplum track:event.name withParameters:event.info];
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

- (nonnull MPKitExecStatus *)logScreen:(nonnull MPEvent *)event {
    [Leanplum advanceTo:event.name withParameters:event.info];
    MPKitExecStatus *execStatus = [[MPKitExecStatus alloc] initWithSDKCode:@(MPKitInstanceLeanplum) returnCode:MPKitReturnCodeSuccess];
    return execStatus;
}

@end
