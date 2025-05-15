# import numpy as np
#import argparse
# import time
#import cv2
import torch
#import torch.backends.cudnn as cudnn
from tracking.yolov8.load_model import LoadYolov8nModel

class DetectYolov8n(object):
    def __init__(self, model):
        self.load_yolo_obj = LoadYolov8nModel()
        self.model = self.load_yolo_obj.load_model()
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

    def detect(self, batch_frame):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'

        #for frame in batch_frame:
            # frame = np.reshape(frame, (3, 640, 480))
        # frame = torch.from_numpy(batch_frame[0]).to(self.device)
        # frame = frame.float()
            # frame = frame.unsqueeze(0)
        #start_time = time.time()
            
        bboxes_cls_list =[]
        b_xywh_list = []

        for frame in batch_frame:
            output = self.model.predict(frame, conf = 0.5)

            if len(output) == 0:
                return []

            bboxes = output[0].boxes
            for box in bboxes:
                b_xywh_list.append(box.xywh.squeeze().tolist())
                bboxes_cls_list.append([box.cls.squeeze().tolist(), box.xyxy.squeeze().tolist()])

        result = bboxes_cls_list
        # return result


            #b = torch.tensor([0, 0, 0, 0])
            #b_y = torch.tensor([0, 0, 0, 0])
            #cls = boxes.cls.to('cpu').numpy()
        
            #j = 0
            #for box in boxes:
            #    b_y = box.xyxy[0]
            #    outs = [cls[j], b_y[0].item(), b_y[1].item(), b_y[2].item(), b_y[3].item()]
            #    j = j + 1

        #end_inf = time.time()
        #inf_time = end_inf - start_time
        #print("inference time for yolov8 nano:", inf_time)

        print("detect.py result  = ", result)

        return result   #, inf_time
