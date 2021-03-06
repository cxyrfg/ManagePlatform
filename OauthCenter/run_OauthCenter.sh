#!/bin/bash
#Date: 2019-08-21
#Author: MuggleLee
#Version: v1.0
docker ps -a | sed '/^CONTAINER/d' | grep "oauth" | gawk '{cmd="docker rm -f "$1; system(cmd)}'
docker images | sed '/^IMAGE/d' | grep "oauth" | gawk '{cmd="docker rmi "$3; system(cmd)}'
mvn clean package docker:build -Dmaven.test.skip;
#docker run -d -p 8082:8082 -e JAVA_OPTS='-Xmx256m' --restart=always -m 700m --memory-swap -1 --name=OauthCenter com.hao/oauth-center:1.0;
