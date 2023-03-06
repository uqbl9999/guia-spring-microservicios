@echo off
set RABBIT_ADDRESSES=localhost:5672
set STORAGE_TYPE=mysql
set MYSQL_USER=zipkin
SET MYSQL_PASS=zipkin
java -jar ./zipkin-server-2.24.0-exec.jar