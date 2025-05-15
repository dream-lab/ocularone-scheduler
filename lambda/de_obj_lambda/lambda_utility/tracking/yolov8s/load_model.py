#import sys
#sys.path.append('tracking/yolov8s')
import torch
# import torch.backends.cudnn as cudnn

from ultralytics import YOLO

class LoadYolov8sModel(object):
    def __init__(self):
        pass
    
    def load_model(self, save_img = False):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        
        #weights = 'yolov8s.pt'
        model = YOLO('./tracking/yolov8s/weights/yolov8s.pt')
        
        return model 
