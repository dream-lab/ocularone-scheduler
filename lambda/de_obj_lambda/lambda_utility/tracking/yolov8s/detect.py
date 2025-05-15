#import numpy as np
#import argparse
#import time
#import cv2
import torch
#import torch.backends.cudnn as cudnn
from tracking.yolov8s.load_model import LoadYolov8sModel

class DetectYolov8s(object):
    def __init__(self, model):
        self.yolo_load_obj = LoadYolov8sModel()
        self.model = self.yolo_load_obj.load_model()
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

    def detect(self, batch_frame, dist = False):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'

        #frame = torch.from_numpy(batch_frame[0]).to(self.device)
        #frame = frame.float()
            
        bboxes_class_lefttop_rightdown_list = []
        
        for frame in batch_frame:
            output = self.model.predict(frame, conf = 0.5)
            if len(output) == 0:
                return []
            bboxes = output[0].boxes
            for box in bboxes:
                bboxes_class_lefttop_rightdown_list.append([box.cls.squeeze().tolist(), box.xyxy.squeeze().tolist()])
        
        result = bboxes_class_lefttop_rightdown_list
        return result
