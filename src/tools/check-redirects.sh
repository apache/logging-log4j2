#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to you under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
##
#
# Checks the permanent redirects for a given site.
# Usage:
#
# check-redirects logging.staged.apache.org
#

BASE_DIR=$(dirname $0)
HOST=$1

if [ "$HOST" == "" ]; then
  HOST=logging.apache.org
fi

while read -r src dst; do
  case $src in
    \#*)
      ;;
    *)
      actual_dst=$(curl -s -I "https://$HOST$src" | grep --color=never -Po '(?<=Location: ).*' | tr -d [[:cntrl:]])
      if [ "$actual_dst" != "https://$HOST$dst" ]; then
        echo "Expecting '$src' to redirect to"
        echo -e "\t'https://$HOST$dst'"
        echo "but was"
        echo -e "\t'$actual_dst'";
      fi
      ;;
  esac
done < $BASE_DIR/redirects.txt

