#!/bin/bash
BOOT_DIR=$(dirname "`readlink -f $0`")
cd $BOOT_DIR
SpringBoot=`ls *.jar | grep -v grep | tr -s " "|cut -d" " -f2`
Args=$2
port=`cat application.yml|grep -o -e '\sport: \([0-9]\{4\}\)'|awk '{print $2}'`
if [ "$Args" = "" ]
then
    Args="-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -Xms512m -Xmx4g -Xmn256m -Xss384k -XX:SurvivorRatio=8 -XX:+UseConcMarkSweepGC -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
fi

if [ "$1" = "" ];
then
    echo -e "\033[0;31m 未输入操作名 \033[0m  \033[0;34m {start|stop|restart|status} \033[0m"
    exit 1
fi

if [ "$SpringBoot" = "" ];
then
    echo -e "\033[0;31m 未输入应用名 \033[0m"
    exit 1
fi

function start()
{
    nohup java -javaagent:$BOOT_DIR/agent/transmittable-thread-local-2.11.4.jar $Args -jar $BOOT_DIR/$SpringBoot > /dev/null 2>&1 &
    echo "$!" > boot.pid
    echo "Start $SpringBoot success..."
}

function stop()
{
    boot_id=`netstat -anp | grep :$port.*java | grep -v grep | tr -s " "|cut -d" " -f7 | cut -d "/" -f1`
    if [ -n "$boot_id" ];then
        kill -9 $boot_id
    else
        boot_id=`ps -ef | grep $BOOT_DIR/ | grep -v grep | tr -s " "|cut -d" " -f2`
        if [ -n "$boot_id" ];then
            kill -9 $boot_id
        fi
    fi
    echo "Stop $SpringBoot success..."
}

function restart()
{
    stop
    sleep 5
    start
}

function status()
{
    boot_id=`ps -ef | grep $BOOT_DIR/ | grep -v grep | tr -s " "|cut -d" " -f2`
    if [ -n "$boot_id" ];then
        echo "$SpringBoot is running..."
    else
        echo "$SpringBoot is not running..."
    fi
}

case $1 in
    start)
    start;;
    stop)
    stop;;
    restart)
    restart;;
    status)
    status;;
    *)

    echo -e "\033[0;31m Usage: \033[0m  \033[0;34m sh  $0  {start|stop|restart|status}  {SpringBootJarName} \033[0m
\033[0;31m Example: \033[0m
      \033[0;33m sh  $0  start esmart-test.jar \033[0m"
esac
