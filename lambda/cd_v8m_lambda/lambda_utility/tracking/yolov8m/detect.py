#import os
#import sys
import torch
#import torch.backends.cudnn as cudnn
from tracking.yolov8m.load_model import LoadYolov8mModel

class DetectYolov8m(object):
    def __init__(self, model):
        self.load_yolo_obj = LoadYolov8mModel()
        self.model = self.load_yolo_obj.load_model()
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'

    def detect(self, batch_frame):    
        device = 'cuda' if torch.cuda.is_available() else 'cpu'

#        for frame in batch_frame:
        #frame = torch.from_numpy(batch_frame[0]).to(self.device)
        #frame = frame.float()
#            results = self.model.predict(frame, conf = 0.5)
#            boxes = results[0].boxes
#
        #    outs = []
        #    b_y = torch.tensor([0, 0, 0, 0])
        #    j = #0
        #    for box in boxes:
        #        b_y = box.xyxy[0]
        #        outs.append([cls[j], b_y[0].item(), b_y[1].item(), b_y[2].item(), b_y[3].item()])
        #        j = j + 1

        result =[]

        bboxes_class_lefttop_rightdown_list = []
        for frame in batch_frame:
            output = self.model.predict(frame, classes=[0], conf = 0.5)
        
            if len(output) == 0:
                return []
        
            bboxes = output[0].boxes                                                        
            for box in bboxes:
                bboxes_class_lefttop_rightdown_list.append([box.cls.squeeze().tolist(), box.xyxy.squeeze().tolist()])
            result = bboxes_class_lefttop_rightdown_list


        return result
