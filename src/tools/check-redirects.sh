#!/bin/bash
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

