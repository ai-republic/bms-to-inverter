#!/bin/bash
#
# This script is run by a new systemd service bms.service.
# Place this script into your bms-to-inverter folder under /home/<your installer username>
# Make this script executable with the command:
# chmod +x bmsservice.sh
#
# Create a new file bms.service in /etc/systemd/system.  You can use the command:
# sudo nano /etc/systemd/system/bms.service
# Copy the lines at the bottom of this file between the comment ends below, starting with [Unit] and ending 
# with WantedByWantedBy=multi-user.target into the new file, and replacing <your installer username> with the correct value.
#
# Execute below command to enable the service; it will automatically start on reboot, and restart if it crashes.  It does not start the service now however.
# sudo systemctl enable bms.service
#
# Execute command to start the service manually:
# sudo systemctl start bms.service
#
# Execute command to see the status of the service:
# sudo systemctl status bms.service
#
# Execute command to stop the service manually:
# sudo systemctl stop bms.service
#
# Execute command to disable the service from automatically running:
# sudo systemctl disable bms.service
#
# Note the script logs actions into ./logs/service.log
# Note also it is preferred to use systemctl command to stop and start bms-to-inverter rather than calling the script directly.
# Calling the script directly will put the actual state of running in a different condition than the system service controller thinks.

while getopts "rkc" opt; do
  case $opt in
    r)
		echo Option r: Run BMS to Inverter
		cd ~/bms-to-inverter
		echo $(date +"%Y-%m-%d %H:%M:%S") Starting BMS to Inverter Service>> logs/service.log

		./bmsservice.sh -c

		stopfile="stop"

		if [ -f "$stopfile" ]; then
			rm "$stopfile"
		fi

		echo $(date +"%Y-%m-%d %H:%M:%S") Running BMS to Inverter >> logs/service.log
		./start.sh

		exit 0
		;;
    k)
		echo Option k: Stop BMS to Inverter
		echo stop > ~/bms-to-inverter/stop
		echo $(date +"%Y-%m-%d %H:%M:%S") Stopping BMS to Inverter Service >> logs/service.log
		exit 0
		;;
    c)
		echo Option c: Setup can0 Port
		echo $(date +"%Y-%m-%d %H:%M:%S") Setup can0 Port >> logs/service.log
		sudo ip link set down can0
		sudo ip link set can0 type can bitrate 500000 restart-ms 100 fd off
		sudo ifconfig can0 txqueuelen 65536
		sudo ip link set up can0
		exit 0
		;;
  esac
done

echo Invalid option.  Available options:
echo "-r Start (run) the service"
echo "-k Sopt (kill) the service"
echo "-c Setup can0 Port"

exit 1

<<commment
[Unit]
Description=BMS to Inverter
After=multi-user.target

[Service]
Type=simple
Restart=always
RestartSec=3
ExecStop=runuser -l <your installer username> -c "echo stop > ~/bms-to-inverter/stop"
ExecStart=runuser -l <your installer username> -c "~/bms-to-inverter/bmsservice.sh -r"

[Install]
WantedBy=multi-user.target
commment