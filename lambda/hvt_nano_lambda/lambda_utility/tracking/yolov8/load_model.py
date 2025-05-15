#import sys
#sys.path.append('tracking/yolov8')
import torch
#import torch.backends.cudnn as cudnn

import os
from ultralytics import YOLO

class LoadYolov8nModel(object):
    def __init__(self):
        pass
    
    def load_model(self, save_img = False):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        
        # pwd = os.cwd()
        # weights = 'best.pt'
        # tracking/yolov8/weights.best.pt
        model = YOLO('/tracking/yolov8/weights/best.pt')
        
        return model 
