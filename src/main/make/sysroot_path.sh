#!/bin/bash
# Copyright 2012 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Returns the location of the latest installed Xcode Mac OS X, iPhoneOS,
# or iPhoneSimulator SDK root directory.
#
# Usage: sysroot_path [--iphoneos | --iphonesimulator]

SDK_TYPE=MacOSX
if [ $# -gt 0 ]; then
  case $1 in
    --iphoneos ) SDK_TYPE=iPhoneOS ;;
    --iphonesimulator ) SDK_TYPE=iPhoneSimulator ;;
    * ) echo "usage: $0 [--iphoneos | --iphonesimulator]" && exit 1 ;;
  esac
fi

if [ ! -d /Applications/Xcode.app ]; then
  echo "Xcode is not installed."
  exit 1
fi

PLATFORM_ROOT=/Applications/Xcode.app/Contents/Developer/Platforms/${SDK_TYPE}.platform
if [ ! -d ${PLATFORM_ROOT} ]; then
  echo "There are no ${SDK_TYPE} SDKs installed."
  exit 1
fi

SDKS_ROOT=${PLATFORM_ROOT}/Developer/SDKs
if [ ! -d ${SDKS_ROOT} ]; then
  echo "There are no ${SDK_TYPE} SDKs installed."
  exit 1
fi

# Return the alphabetically last SDK in the directory, which should be the
# latest version.  This will need to be improved if iOS 10 ever releases.
ls -rd ${SDKS_ROOT}/${SDK_TYPE}* | head -1
exit $?
