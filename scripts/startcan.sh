#!/usr/bin/env bash
sudo ip link set can0 type can bitrate 500000 restart-ms 100 fd off
sudo ip link set can1 type can bitrate 500000 restart-ms 100 fd off
sudo ifconfig can0 txqueuelen 65536
sudo ifconfig can1 txqueuelen 65536
sudo ip link set up can0
sudo ip link set up can1