// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
//  Field.m
//  JreEmulation
//
//  Created by Tom Ball on 06/18/2012.
//

#import "IOSClass.h"
#import "IOSObjectArray.h"
#import "java/lang/AssertionError.h"
#import "java/lang/Boolean.h"
#import "java/lang/Byte.h"
#import "java/lang/Character.h"
#import "java/lang/Double.h"
#import "java/lang/Float.h"
#import "java/lang/Integer.h"
#import "java/lang/Long.h"
#import "java/lang/NullPointerException.h"
#import "java/lang/Short.h"
#import "java/lang/Void.h"
#import "java/lang/reflect/Field.h"
#import "java/lang/reflect/Modifier.h"

@implementation JavaLangReflectField

typedef union {
  void *asId;
  char asChar;
  unichar asUnichar;
  short asShort;
  int asInt;
  long long asLong;
  float asFloat;
  double asDouble;
  BOOL asBOOL;
} JavaResult;

- (id)initWithName:(NSString *)name withClass:(IOSClass *)aClass {
  if ((self = [super init])) {
    const char* cname =
        [name cStringUsingEncoding:[NSString defaultCStringEncoding]];
    ivar_ = class_getInstanceVariable(aClass.objcClass, cname);
    declaringClass_ = aClass;
  }
  return self;
}

- (id)initWithIvar:(Ivar)ivar withClass:(IOSClass *)aClass {
  if ((self = [super init])) {
    ivar_ = ivar;
    declaringClass_ = aClass;
  }
  return self;
}

+ (id)fieldWithName:(NSString *)name withClass:(IOSClass *)aClass {
  JavaLangReflectField *field =
      [[JavaLangReflectField alloc]
       initWithName:name withClass:aClass];
#if ! __has_feature(objc_arc)
  [field autorelease];
#endif
  return field;
}

+ (id)fieldWithIvar:(Ivar)ivar withClass:(IOSClass *)aClass {
  JavaLangReflectField *field =
      [[JavaLangReflectField alloc] initWithIvar:ivar withClass:aClass];
#if ! __has_feature(objc_arc)
  [field autorelease];
#endif
  return field;
}

- (NSString *)getName {
  return [NSString stringWithCString:ivar_getName(ivar_)
                            encoding:[NSString defaultCStringEncoding]];
}

- (NSString *)description {
    return [self getName];
}

- (id)getWithId:(id)object {
  return object_getIvar(object, ivar_);
}

- (BOOL)getBooleanWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  BOOL *field = ((BOOL *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (char)getByteWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  char *field = ((char *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (unichar)getCharWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  unichar *field = ((unichar *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (double)getDoubleWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  double *field = ((double *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (float)getFloatWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  float *field = ((float *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (int)getIntWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  int *field = ((int *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (long long)getLongWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  long long *field = ((long long *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (short)getShortWithId:(id)object {
  void *p = (ARCBRIDGE void *) object;
  short *field = ((short *) p) + ivar_getOffset(ivar_);
  return *field;
}

- (void)setAndRetain:(id)object withId:(id) ARC_CONSUME_PARAMETER value {
  object_setIvar(object, ivar_, value);
}

- (void)setWithId:(id)object withId:(id) value {
  // Test for nil, since calling a method that consumes its parameters
  // with nil causes a leak.
  // http://clang.llvm.org/docs/AutomaticReferenceCounting.html#retain-count-semantics
  if (value) {
    [self setAndRetain:object withId:value];
  } else {
    object_setIvar(object, ivar_, value);
  }
}

- (void)setBooleanWithId:(id)object withBOOL:(BOOL)value {
  void *p = (ARCBRIDGE void *) object;
  BOOL *field = ((BOOL *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setByteWithId:(id)object withChar:(char)value {
  void *p = (ARCBRIDGE void *) object;
  char *field = ((char *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setCharWithId:(id)object withUnichar:(unichar)value {
  void *p = (ARCBRIDGE void *) object;
  unichar *field = ((unichar *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setDoubleWithId:(id)object withDouble:(double)value {
  void *p = (ARCBRIDGE void *) object;
  double *field = ((double *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setFloatWithId:(id)object withFloat:(float)value {
  void *p = (ARCBRIDGE void *) object;
  float *field = ((float *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setIntWithId:(id)object withInt:(int)value {
  void *p = (ARCBRIDGE void *) object;
  int *field = ((int *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setLongWithId:(id)object withLongInt:(long long)value {
  void *p = (ARCBRIDGE void *) object;
  long long *field = ((long long *) p) + ivar_getOffset(ivar_);
  *field = value;
}

- (void)setShortWithId:(id)object withShortInt:(short)value {
  void *p = (ARCBRIDGE void *) object;
  short *field = ((short *) p) + ivar_getOffset(ivar_);
  *field = value;
}


- (IOSClass *)getType {
  const char *argType = ivar_getTypeEncoding(ivar_);
  if (strlen(argType) != 1) {
    NSString *errorMsg =
    [NSString stringWithFormat:@"unexpected type: %s", argType];
    id exception = [[JavaLangAssertionError alloc] initWithNSString:errorMsg];
#if ! __has_feature(objc_arc)
    [exception autorelease];
#endif
    @throw exception;
  }
  return decodeTypeEncoding(*argType);
}

- (int)getModifiers {
  // All Objective-C fields and methods are public at runtime.
  return JavaLangReflectModifier_PUBLIC;
}

- (IOSClass *)getDeclaringClass {
  return declaringClass_;
}

@end
