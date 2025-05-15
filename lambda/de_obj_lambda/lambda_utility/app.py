import os
import sys
import time
import uuid
# sys.path.append("../tracking/")
import json,logging,torch

from tracking.yolov8s.load_model import LoadYolov8sModel
from tracking.yolov8s.detect import DetectYolov8s

from tracking.monodepth.load_model import LoadMonodepthModel
from tracking.monodepth.inference import MonoLTInferencing

import cv2
import base64

# import requests
logger_inst = {}
def create_logger(logger_name, filename='default'):
    if logger_name in logger_inst:
        return logger_inst[logger_name]
    LOG = logging.getLogger(f"{logger_name}")

    # file_fmt = logging.Formatter(fmt='%(message)s')
    console_fmt = logging.Formatter(fmt='%(message)s')

    # file_handlr = logging.FileHandler(f"{filename}.log")
    console_handlr = logging.StreamHandler(stream=sys.stdout)

    # file_handlr.setFormatter(file_fmt)
    console_handlr.setFormatter(console_fmt)

    # LOG.addHandler(file_handlr)
    LOG.addHandler(console_handlr)

    LOG.setLevel("INFO")
    logger_inst[logger_name] = LOG
    return LOG

class Inference:
    def __init__(self) -> None:
        # start = time.time()
        self.yolov8s_model = LoadYolov8sModel().load_model()
        self.yolo_detect = DetectYolov8s(self.yolov8s_model)

        self.mono_model = LoadMonodepthModel().load_model()

        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.logger = create_logger('DEOBJ','DEOBJ')
        self.logger.info('All models are loaded')
        # print(f"HVT loaded in {time.time() - start}")
        #self.logger.info(f"HVT loaded in {time.time() - start}")

inference_obj = Inference()


def lambda_handler(event, context):
    """Sample pure Lambda function

    Parameters
    ----------
    event: dict, required
        API Gateway Lambda Proxy Input Format

        Event doc: https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html#api-gateway-simple-proxy-for-lambda-input-format

    context: object, required
        Lambda Context runtime methods and attributes

        Context doc: https://docs.aws.amazon.com/lambda/latest/dg/python-context-object.html

    Returns
    ------
    API Gateway Lambda Proxy Output Format: dict

        Return doc: https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html
    """

    # try:
    #     ip = requests.get("http://checkip.amazonaws.com/")
    # except requests.RequestException as e:
    #     # Send some context about this error to Lambda Logs
    #     print(e)

    #     raise e

    # inference_obj.logger.info("Entered lambda handler")

    start = time.time()
    # print("Inside app.py")
    # print(event)

    try:
        body = json.loads(event["body"])
        global inference_obj
        inference_obj.logger.info("Entered lambda handler")
        print("lambda_handler")
        if inference_obj == None:
            inference_obj = Inference()
        if body.get("is_cloud_exec"):
            #print("Grpc: ",start_s - float(request.batch_id))
            file_nm = f'/tmp/{uuid.uuid4()}.mp4'
            with open(file_nm, 'wb') as fp:
                fp.write(base64.b64decode(body.get("frame")))
            model= body.get("dnn_model")
            task_id = body.get("task_id")
        elif body.get("warmup"):
            load_time = body.get("load_time")
            time.sleep(load_time)
            return {
            "statusCode": 200,
            "body": json.dumps({
                "message": f"Warmup done. Waited {load_time}"
            }),
        }
        print("frame_Extraction")
        frames = []
        cap = cv2.VideoCapture(file_nm)
        r_s = time.time()
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            frames.append(frame)
        inference_obj.logger.info(f'read time {time.time() - r_s}. Frames {len(frames)}')
        
        # start = time.time()

        yolov8_result = inference_obj.yolo_detect.detect(frames,dist=True)
        #outs = yolov8_result
        
        mono = MonoLTInferencing()
        monodepth_output = mono.monodepth(inference_obj.mono_model, file_nm)

        outs = mono.LT_avg(yolov8_result, monodepth_output)
    
        inf_time = time.time() - start

        # inference_obj.logger.info("Inferencing output for DE-OBJ = %s" % (outs))
        # print(type(outs))
        
        inference_obj.logger.info(f"output of DE-OBJ = {outs}")
        inference_obj.logger.info("task_id %s model %s  inf_time: %s" % (task_id,model, inf_time))
        # inference_obj.logger.info("task_id %s model %s time_taken: %s" % (task_id,model ,end_i-start_s))

        return {
            "statusCode": 200,
            "body": json.dumps({
                "message": "Processed the chunk",
                # "location": ip.text.replace("\n", "")
                "task_id" : task_id,
                "output" : str(outs),
                "inf_time" : inf_time
            }),
        }
    except Exception as e:
        inference_obj.logger.exception(f"Exception occurred {e}")
        return {
            "statusCode": 400,
            "body": json.dumps(
                {
                    "message" : str(e),
                    "task_id" : task_id
                }
            )
        }
