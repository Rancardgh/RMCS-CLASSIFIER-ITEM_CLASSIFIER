#!/bin/sh
#### Start Rancard Item Classifier Application #######
# chkconfig: 345 99 01
# description: RICA startup script

. /etc/init.d/functions

if [ -f /etc/sysconfig/ric-instance-1 ]; then
. /etc/sysconfig/ric-sandbox
fi

##### below is the script for controlling classification service #########

LOG="/var/log/ric/ric-sandbox.log"
PID_FOLDER="/var/run"
pid_file="$PID_FOLDER/ric-sandbox.pid"
LOCK="/var/lock/subsys/ric-sandbox"
PROJECT_NAME="Rancard Item Classifier Application - Sandbox"
PROJECT_DIR="/var/www/sandbox/Rancard-Item-Classifier-Application/target/"
PROJECT_LIB_DIR="/var/www/sandbox/Rancard-Item-Classifier-Application/target/lib/*.jar"
JAVA_HOME="/opt/jdk1.7.0_79/bin/java"
PROJECT_JAR="content.classification-1.0-SNAPSHOT-sandbox.jar"

pid_of_spring_boot() {
    pgrep -f "java.*$PROJECT_JAR"
}

RETVAL=0
case "$1" in
start)
cd "$PROJECT_DIR"
su root -c "nohup \"$JAVA_HOME\" -Drun.jvmArguments=-Xms256m -Xmx512m -XX:+UseParallelOldGC -XX:ParallelGCThreads=2 -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError -Dsun.misc.URLClassPath.disableJarChecking=true -jar content.classification-1.0-SNAPSHOT-sandbox.jar --spring.profiles.active=sandbox --run.profiles=sandbox >> \"$LOG\" 2>&1 &"
pid=`pid_of_spring_boot`
echo -n "Started $PROJECT_NAME: "$pid
echo
;;
stop)
pid=`pid_of_spring_boot`
echo -n "Stopping $PROJECT_NAME: "$pid
[ -n "$pid" ] && kill $pid

RETVAL=$?
cnt=10
while [ $RETVAL = 0 -a $cnt -gt 0 ] &&
{ pid_of_spring_boot > /dev/null ; } ; do
    sleep 1
    ((cnt--))
done
echo
;;
#[ $RETVAL = 0 ] && rm -f "$LOCK"
#[ $RETVAL = 0 ] && success $"$STRING" || failure $"$STRING"
restart)
;;
*)
echo "Usage: $0 {start|stop|restart}"
exit 1
;;
esac