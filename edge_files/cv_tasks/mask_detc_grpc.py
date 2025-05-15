import json
import logging
import os
import sys
import uuid
from concurrent import futures
from time import sleep
import time
import cv2,statistics,signal
import yaml, grpc,torch,tempfile
sys.path.append('proto')
import proto.metadata_pb2 as metadata_pb2
import proto.metadata_pb2_grpc as metadata_pb2_grpc
logging.basicConfig(level=logging.INFO)
from collections import defaultdict
import ast

inf = []
ov_head = []
e2e = []
sum = 0
count = 0

# inf_dict = defaultdict(list)
e2e_dict = defaultdict(list)

class Inference(metadata_pb2_grpc.PostInferencerServicer):
    def __init__(self):
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

    def Submit(self, request, context):
        # print("submit function entered")
        start = time.time()
        if request is not None:
            task_id = request.task_id
            dnn_model = request.dnn_model
            dnn_model_output = request.result
            print('dnn_model_output', dnn_model_output)
            count_mask = 0

            dnn_model_output_list = ast.literal_eval(dnn_model_output)
            for res in dnn_model_output_list:
                if res[0] == 0:
                    count_mask+=1
            if(count_mask >= 1):
                print(f"Mask detected at {task_id}")
            else:
                print(f"No mask detected at {task_id}")

        end = time.time()
        e2e_dict[0].append(end - start)
       
        print(f"post_processing_time: ", end-start)
        return metadata_pb2.Ack(message = "")

def sigterm_handler():
    # with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/inf_result_tempfile_run.json", "w") as json_file:
        # json.dump(e2e_dict, json_file)
    with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/e2e_result_tempfile_run.json", "w") as json_file:
        json.dump(e2e_dict, json_file)
    print("Mean e2e %s, Median e2e %s" % (statistics.mean(e2e_dict[0]), statistics.median(e2e_dict[0])))

def start_server():
    global sum,count
    server = grpc.server(futures.ThreadPoolExecutor(max_workers = 1))
    metadata_pb2_grpc.add_PostInferencerServicer_to_server(Inference(), server)
    server.add_insecure_port('[::]:5004')  # 5004 for mask detection
    signal.signal(signal.SIGTERM, sigterm_handler)
    try:
        server.start()
        print("Inferencing Server started ... ")
        server.wait_for_termination()
    except Exception:
        print("Mean e2e %s, Median e2e %s" % (statistics.mean(e2e_dict[0]), statistics.median(e2e_dict[0])))
    finally:
        print("Mean e2e %s, Median e2e %s" % (statistics.mean(e2e_dict[0]), statistics.median(e2e_dict[0])))

if __name__ == "__main__":
    start_server()
