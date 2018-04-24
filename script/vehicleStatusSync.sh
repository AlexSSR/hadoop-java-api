#!/bin/bash
export JAVA_HOME=/opt/jdk1.8.0_131
export CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/*.jar
export PATH=.:$JAVA_HOME/bin:$PATH


APP_NAME=kafka-es-1.0.0.jar

case $1 in
    start)
        nohup java -Dlog4j.fileName=service-vehicleStatus -cp ${APP_NAME} com.bitnei.sync.VehicleStatusSync >/dev/null 2>&1 &
        echo ${APP_NAME} start!
        ;;
    stop)
        ps -ef| grep ${APP_NAME} |grep -v grep |awk '{print $2}'  | sed -e "s/^/kill -9 /g" | sh -
        echo ${APP_NAME} stop!
        ;;
    restart)
        "$0" stop
        sleep 3
        "$0" start
        ;;
    status)  ps -aux | grep ${APP_NAME} | grep -v 'grep'
        ;;
    log)
    	case $2 in
	debug)
		tail -f -n ${3-400} logs/service.log
		;;
	error)
		tail -f -n ${3-400} logs/error.log
		;;
	*)
		echo "Example: services.sh log {debug|error}" ;;
	esac
        ;;
    *)
        echo "Example: services.sh [start|stop|restart|status]" ;;
esac

