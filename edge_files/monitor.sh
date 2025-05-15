#!/bin/bash
#exec > ./output.txt 2>&1
while true
do
#        TIMESTAMP=`date "+%Y-%m-%d %H:%M:%S"`
        TIMESTAMP=`date +%s`
        echo "TS_start :: $TIMESTAMP" >> /home/ultraviolet/experiments/$edge/"$edge"_network_output.txt
        #echo "Timestamp $EPOCHSECONDS" >> output.txt
        #cmd="sudo iftop -B -b -t -o destination -s 2 -n"
        cmd="ifstat -b -i eth0  1 1"
        echo "`$cmd| tail -1`" >> /home/ultraviolet/experiments/$edge/"$edge"_network_output.txt
        TIMESTAMP=`date +%s`
        echo "TS_END :: $TIMESTAMP" >> /home/ultraviolet/experiments/$edge/"$edge"_network_output.txt

    done
