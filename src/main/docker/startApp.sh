#!/bin/bash

java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT} -Xms100m -Xmx280m -jar /app.jar
