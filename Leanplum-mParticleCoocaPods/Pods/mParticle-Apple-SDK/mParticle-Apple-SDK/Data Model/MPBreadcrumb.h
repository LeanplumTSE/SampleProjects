//
//  MPBreadcrumb.h
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

#import "MPDataModelAbstract.h"
#import "MPDataModelProtocol.h"

@interface MPBreadcrumb : MPDataModelAbstract <NSCopying, NSCoding, MPDataModelProtocol>

@property (nonatomic, strong, nonnull) NSString *sessionUUID;
@property (nonatomic, strong, nonnull) NSData *breadcrumbData;
@property (nonatomic, strong, nonnull) NSNumber *sessionNumber;
@property (nonatomic, unsafe_unretained) NSTimeInterval timestamp;
@property (nonatomic, unsafe_unretained) int64_t breadcrumbId;

- (nonnull instancetype)initWithSessionUUID:(nonnull NSString *)sessionUUID
                               breadcrumbId:(int64_t)breadcrumbId
                                       UUID:(nonnull NSString *)uuid
                             breadcrumbData:(nonnull NSData *)breadcrumbData
                              sessionNumber:(nonnull NSNumber *)sessionNumber
                                  timestamp:(NSTimeInterval)timestamp;

@end
