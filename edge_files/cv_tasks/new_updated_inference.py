#from pose_estimation.trt_pose_hand.handpose import HandPoseModel
import json
from PIL import Image
import logging
import os
import sys
import uuid
import pandas as pd
from concurrent import futures
from time import sleep
import time
import cv2,statistics,signal
import yaml, grpc,torch,tempfile
sys.path.append('proto')
#sys.path.append(os.getcwd())
#sys.path.append('detection/yolov5')
import proto.metadata_pb2 as metadata_pb2
import proto.metadata_pb2_grpc as metadata_pb2_grpc
logging.basicConfig(level=logging.INFO)
#from tracking.yolov4.load_model import Loadv4Model
#from tracking.yolov4.detect import Detect
#sys.path.append('detection/yolov5_hub')
#from detection.yolov5_hub.load_model import Loadv5Model
#from detection.yolov5_hub.detect import Detectv5
from FaceMaskDetection.pytorch_infer import LoadMaskModel,DetectMask
from pose_estimation.trt_pose.tasks.human_pose.bodypose import BodyPoseModel
from collections import defaultdict
from bp_test import get_pose
from FaceMaskDetection import pytorch_infer_og
from FaceMaskDetection.load_model.pytorch_loader import load_pytorch_model, pytorch_inference

from ultralytics import YOLO

from detection.yolov8n.load_model import LoadYolov8nModel
from detection.yolov8n.detect import DetectYolov8n

from detection.yolov8m.load_model import LoadYolov8mModel
from detection.yolov8m.detect import DetectYolov8m

from detection.yolov8s.load_model import LoadYolov8sModel
from detection.yolov8s.detect import DetectYolov8s

from detection.monodepth.load_model import LoadMonodepthModel
from detection.monodepth.inference import MonoLTInferencing

import numpy as np
# from hvt_detection.load_model import HvtYolov8Model

inf = []
ov_head = []
e2e = []
sum = 0
count = 0

inf_dict = defaultdict(list)
e2e_dict = defaultdict(list)

class Inference(metadata_pb2_grpc.InferencerServicer):
    def __init__(self):
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.bodypose = BodyPoseModel()
        self.mask_model = LoadMaskModel().load_model()
        self.detect_mask = DetectMask(self.mask_model)

        self.yolov8n_model = LoadYolov8nModel().load_model()
        self.yolov8m_model = LoadYolov8mModel().load_model()
        self.yolov8s_model = LoadYolov8sModel().load_model()

        self.mono_model = LoadMonodepthModel().load_model()
        # load monodepth model in memory 
        # self.monodepth = ExecuteMonodepth().monodepth()

    def Submit(self, request, context):
        print("connected...")
        print(request)
        start_s = time.time()
        dnn_model = request.dnn_model
        task_id = request.task_id
        batch_id = request.batch_id
        file_nm = request.file_path

        if request is not None:
            frames = []
            cap = cv2.VideoCapture(file_nm)

        while True:
            ret, frame = cap.read()
            if not ret:
                break
            frames.append(frame)

        print(f'Length of frames {len(frames)}')
        # print(f'read time {time.time() - r_s}')
        """
        Inferencing code goes here
        """
        #do some warmup

        # yolov8n = DetectYolov8n()
        # yolov8m = DetectYolov8m()
        # mono = ExecuteMonodepth()

        start = time.time()
        if dnn_model == 'HAZARD_VEST':
            yolov8n = DetectYolov8n()
            res = yolov8n.detect(self.yolov8n_model, frames, dnn_model)            
            print("Output of hazard vest detection = " , res)

        elif dnn_model == 'CROWD_DENSITY':
            yolov8m = DetectYolov8m()
            res = yolov8m.detect(self.yolov8m_model, frames, dnn_model)
            print("Output of crowd density = ", res)

        elif dnn_model == 'MASK_DETECTION':
            res = self.detect_mask.inference(frames, draw_result = False, show_result = False, target_shape = (360, 360))
            print('mask detection res = ', res)

        elif dnn_model == 'BODY_POSE_ESTIMATION':
            res = self.bodypose.detect_pose(frames) #bodypose estimation
            print('body pose res=', res)
            #get_pose(res)

        elif dnn_model == 'DISTANCE_ESTIMATION_VIP': # previously handpose, HP
            yolov8n = DetectYolov8n()
            res = yolov8n.detect(self.yolov8n_model, frames, dnn_model) 
            print('DE VIP res = ', res)

        elif dnn_model == 'DISTANCE_ESTIMATION_OBJECT': # previously DE
            #yolov8m = DetectYolov8m()
            yolov8s = DetectYolov8s()
            start_yolo = time.time()
            yolov8_result = yolov8s.detect(self.yolov8s_model, frames, dnn_model)
            time_yolo = time.time() - start_yolo
            #res1 = call monodepth and get the depth for entire image
            #res2 = use yolov8_results and res1 and get the LT% for all objects 

            mono = MonoLTInferencing() 

            start_mono = time.time()
            monodepth_output = mono.monodepth(self.mono_model, file_nm)
            time_mono = time.time() - start_mono

            start_avg = time.time()
            res = mono.LT_avg(yolov8_result, monodepth_output)
            time_avg = time.time() - start_avg
                
            #res = [yolov8_result, monodepth_output.tolist()]
            print (f"de_obj_steps_times     time_yolo  {time_yolo}  time_mono  {time_mono}  time_avg  {time_avg}")
            print('output of distance estimation objects = ', res)
        # inf_dict[0].append(end_i - start)

        # end_i = time.time()
        # print("task_id %s model %s  inf time: %s" % (task_id, dnn_model,end_i-start))
        end = time.time()
        print("task_id %s   model %s   e2e_time_taken: %s   inf_time_taken: %s" % (task_id, dnn_model, end-start_s, end-start))

        inf_dict[0].append(end-start)
        e2e_dict[0].append(end-start_s)

        return metadata_pb2.Ack(message="Processed the chunk", task_id=task_id, batch_id=batch_id, result= str(res))

def sigterm_handler():
    with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/inf_result_tempfile_run.json", "w") as json_file:
        json.dump(inf_dict, json_file)
    with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/e2e_result_tempfile_run.json", "w") as json_file:
        json.dump(e2e_dict, json_file)
    print("Mean inf %s, Median Inf %s" % (statistics.mean(inf_dict[0]), statistics.median(inf_dict[0])))
def start_server():
    global sum,count
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    metadata_pb2_grpc.add_InferencerServicer_to_server(Inference(),
                                                       server)
    server.add_insecure_port('[::]:5001')
    dnn_model = os.environ['dnn_model']
    signal.signal(signal.SIGTERM, sigterm_handler)
    try:
        server.start()
        print("Inferencing Server started... ")
        server.wait_for_termination()
    except Exception:
        print(statistics.mean(inf),statistics.mean(ov_head))
        with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/inf_result_tempfile_run.json", "w") as json_file:
            json.dump(inf_dict, json_file)
        with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/e2e_result_tempfile_run.json", "w") as json_file:
            json.dump(e2e_dict, json_file)
        print("Mean inf %s, Median Inf %s" % (statistics.mean(inf_dict[0]), statistics.median(inf_dict[0])))
    finally:
        with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/inf_result_tempfile_run.json", "w") as json_file:
            json.dump(inf_dict, json_file)
        with open(f"/home/ultraviolet/cpu_container_benchmark/{dnn_model}/e2e_result_tempfile_run.json", "w") as json_file:
            json.dump(e2e_dict, json_file)
        print("Mean inf %s, Median Inf %s" % (statistics.mean(inf_dict[0]), statistics.median(inf_dict[0])))

if __name__ == "__main__":
    start_server()
