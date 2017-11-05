#!/bin/bash

rm -f /var/lib/mysql/CAN_USE

# Change root password if needed
LAST_PASS=$(cat /volumes/config/lastPass)

set -e
NEW_PASS=$(cat /newPass)

# Initializing
if [ ! -f /var/lib/mysql/READY ]; then
	echo Initializing the DB
	/usr/bin/mysql_install_db
	
fi

# Start
echo Starting
/usr/sbin/mysqld &
APP_PID=$!
echo Started

if [ "$LAST_PASS" != "$NEW_PASS" ]; then
	sleep 5
	echo Update the password
	
	if [ "$LAST_PASS" == "" ]; then
		echo Had no password 
		mysql -u root << _EOF
DROP USER IF EXISTS root@localhost;
DROP USER IF EXISTS root@'127.0.0.1';
DROP USER IF EXISTS root@'::1';
GRANT ALL PRIVILEGES ON *.* TO root@'%' IDENTIFIED BY '$NEW_PASS' WITH GRANT OPTION;
FLUSH PRIVILEGES;
_EOF
	else
		echo Had a password
		mysql --defaults-file=/volumes/config/lastPass.cnf -u root << _EOF
DROP USER IF EXISTS root@localhost;
DROP USER IF EXISTS root@'127.0.0.1';
DROP USER IF EXISTS root@'::1';
GRANT ALL ON *.* TO root@'%' IDENTIFIED BY '$NEW_PASS';
FLUSH PRIVILEGES;
_EOF
	fi
	echo $NEW_PASS > /volumes/config/lastPass
	cp /newPass.cnf /volumes/config/lastPass.cnf
fi

touch /var/lib/mysql/READY
touch /var/lib/mysql/CAN_USE

wait $APP_PID
