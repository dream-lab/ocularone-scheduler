#!/bin/bash

mkdir /home/ultraviolet/experiments/$edge
bash monitor.sh &
bw_pid=$!
python3 mock_edge.py GRPC_PING 1 1 &>/dev/null &
cd cv_tasks

top -b > /home/ultraviolet/experiments/$edge/"$edge"_top_logs &
top_pid=$!

python3  -u new_updated_inference.py  >  /home/ultraviolet/experiments/$edge/"$edge"_new_server_logs 2>&1  &
PID2=$!

python3  -u mask_detc_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_mask_detection_server_logs 2>&1  &
PID3=$!

python3  -u bp_est_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_bp_detection_server_logs 2>&1  &
PID5=$!

python3  -u crowd_detc_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_crowd_detection_server_logs 2>&1  &
PID6=$!

python3  -u hvt_detc_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_hvt_detection_server_logs 2>&1  &
PID7=$!

python3  -u dist_est_vip_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_dist_est_vip_server_logs 2>&1  &
PID8=$!

python3  -u dist_est_obj_grpc.py  >  /home/ultraviolet/experiments/$edge/"$edge"_postprocessing_dist_est_obj_server_logs 2>&1  &
PID9=$!

cd ..


sleep 10

cd ocularone

java -cp target/ocularone-1.0-SNAPSHOT-jar-with-dependencies.jar org.dreamlab.ApplicationDetails.Application $algo $load > /home/ultraviolet/experiments/$edge/"$edge"_algo_"$algo"_load_"$load"_java_client.log 2>&1;

sleep 5 

kill -2 $PID $top_pid $bw_pid $PID2 $PID3 $PID6 $PID7 $PID8 $PID9 $PID5
