#!/bin/sh
ps -ef |grep GuardMain  |awk {'print $2'} | sed -e "s/^/kill -9 /g" | sh -
