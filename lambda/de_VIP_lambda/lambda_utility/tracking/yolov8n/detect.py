#import numpy as np
#import argparse
# import time
#import cv2
import torch
#import torch.backends.cudnn as cudnn
from tracking.yolov8n.load_model import LoadYolov8nModel

class DetectYolov8n(object):
    def __init__(self, model):
        self.load_yolo_obj = LoadYolov8nModel()
        self.model = self.load_yolo_obj.load_model()
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

    def detect(self, batch_frame, dist = False):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'

        #start = time.time()

        result = 0.0
        for frame in batch_frame:
            output = self.model.predict(frame, conf = 0.5)
            bboxes = output[0].boxes
        
            bboxes_cls_list = []
            b_xywh_list =[]

            for box in bboxes:
                b_xywh_list.append(box.xywh.squeeze().tolist())
                bboxes_cls_list.append([box.cls.squeeze().tolist(), box.xyxy.squeeze().tolist()])

            for item in b_xywh_list:
                bbox_w = item[2]
                bbox_h = item[3]
                bbox_area = bbox_w * bbox_h
                result = bbox_w * -2.144 + bbox_h * -1.767 + bbox_area * 0.0050
    
        #inf_time = time.time() - start
        
        return result   #, inf_time
