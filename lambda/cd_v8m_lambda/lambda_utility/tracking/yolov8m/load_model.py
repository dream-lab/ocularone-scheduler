#import sys
#sys.path.append('../../detection/yolov5')
import torch
import torch.backends.cudnn as cudnn

from ultralytics import YOLO

class LoadYolov8mModel(object):
    def __init__(self):
        pass

    def load_model(self):
        device = 'cuda' if torch.cuda.is_available() else 'cpu'
        
        #weights = 'yolov8m.pt'
        model = YOLO('./tracking/yolov8m/weights/yolov8m.pt')

        return model
