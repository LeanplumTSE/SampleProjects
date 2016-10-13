//
//  MPEvent+Internal.h
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

#import "MPEvent.h"
#import "MPIConstants.h"

@interface MPEvent(Internal)

@property (nonatomic, strong, nullable) NSDate *timestamp;

- (void)beginTiming;
- (nullable NSDictionary *)breadcrumbDictionaryRepresentation;
- (nullable NSDictionary<NSString *, id> *)dictionaryRepresentation;
- (void)endTiming;
- (nullable NSDictionary *)screenDictionaryRepresentation;

@end
