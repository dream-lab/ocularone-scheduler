import requests
import json
import os
import sys
import time
import statistics
import logging
import uuid
from multiprocessing import Process

"""
sample metadata file
{
drone_id:<>,
batch_size:<>,
batch_duration:<>,
batch_details:
{<video_id>:{
    start_time:<>,
    end_time:<>,}
}
}
"""
# metadata_json = json.load(open('video_chunks/metadata.json'))
# drone_id = metadata_json["drone_id"]
# batch_size = metadata_json["batch_size"]
# batch_duration = metadata_json["batch_duration"]


def create_logger(logger_name, filename='default'):
    LOG = logging.getLogger(f"{logger_name}")

    file_fmt = logging.Formatter(fmt='%(created)f %(message)s')
    console_fmt = logging.Formatter(fmt='%(name)s %(levelname)-4s %(message)s')

    file_handlr = logging.FileHandler(f"/home/ultraviolet/experiments/{edge}/{filename}.log")
    console_handlr = logging.StreamHandler(stream=sys.stdout)

    file_handlr.setFormatter(file_fmt)
    console_handlr.setFormatter(console_fmt)

    LOG.addHandler(file_handlr)
    #LOG.addHandler(console_handlr)

    LOG.setLevel("INFO")
    return LOG

#FOG_IP = "54.180.106.19"

model_map = {
    "HAZARD_VEST" : "hvt_detect"
}
model = sys.argv[1]
thread=sys.argv[2]
edge=os.getenv("edge")
FUNCTION_URL = f"https:xyz.aws/"
if model == "HAZARD_VEST":
    FUNCTION_URL =  "hhttps:xyz.aws"
if model == "BODY_POSE_ESTIMATION":
    FUNCTION_URL =  "https:xyz.aws"
if model == "DISTANCE_ESTIMATION_OBJECT":
    FUNCTION_URL =  "https:xyz.aws"
if model == "MASK_DETECTION":
    FUNCTION_URL =  "https:xyz.aws"
if model == "CROWD_DENSITY":
    FUNCTION_URL =  "https:xyz.aws"
if model == "DISTANCE_ESTIMATION_VIP":
    FUNCTION_URL =  "https:xyz.aws"


def send_batch_details(logger,client_id):
    path_modifier = "/home/ultraviolet/Desktop/ocularone_stuff/video_dataset/1frame"
    e2e = []
    #payload = dict(warmup=True,load_time=5)
    #resp = requests.post(data=json.dumps(payload),url=FUNCTION_URL,verify=False,headers={'Content-Type': 'application/json'})
    #print(resp.text,resp.status_code)
    while True:
        try:
            task_id = str(uuid.uuid4())
            s = time.time()
            #resp = stub.Submit(m_messages.JobDetails(frame=file.read(),batch_id=os.path.join(path_modifier,batch_id.strip()), is_cloud_exec=True,task_id=task_id,dnn_model=model))
            payload = dict(frame='', is_cloud_exec=True,task_id=task_id,dnn_model=model)
            resp =requests.post(data=json.dumps(payload),url=FUNCTION_URL,verify=False,headers={'Content-Type': 'application/json'})
            e = time.time()
            logger.info("%s:%s:%s:%s",client_id,task_id,e-s,s)
            e2e.append(e-s)
            #print(e-s)
            time.sleep(1-(e-s))
        except Exception as e:
            print(e)
    metadata.close()
    print("Mean: %s, Median: %s" % (statistics.mean(e2e),statistics.median(e2e)))

#clients= [1,2,5,10,20,30]
clients= [int(sys.argv[3])]
for client in clients:
    p_list = []
    print("Starting clients",client)
    LOG = create_logger(f'{model}-Thread-{thread}-Clients-{client}',f'{model}-Thread-{thread}-Clients-{client}')
    for i in range(client):
        p_list.append(Process(target=send_batch_details,args=(LOG,i,)))
    for p in p_list:
        p.start()
    for p in p_list:
        p.join()
