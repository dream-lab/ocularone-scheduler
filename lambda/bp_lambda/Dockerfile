#FROM public.ecr.aws/docker/library/python:buster
FROM public.ecr.aws/docker/library/python:3.8-buster

RUN apt update

# RUN apt-get upgrade -y gcc

RUN apt install -y ffmpeg

COPY lambda_utility/requirements.txt ./

#RUN python -m pip3 install --upgrade pip3
RUN pip3 install -r requirements.txt

RUN pip3 install torch==1.10.0+cu102 torchvision==0.11.0+cu102 torchaudio==0.10.0 -f https://download.pytorch.org/whl/torch_stable.html
# RUN pip3 install torch==1.10.0 torchvision==0.11.0 torchaudio==0.10.0 --extra-index-url https://download.pytorch.org/whl/cpu
RUN pip3 install awslambdaric

# RUN apt install -y python3-matplotlib

COPY lambda_utility/pose_estimation ./pose_estimation
COPY lambda_utility/app.py ./
ENV base_path=./
ENV TORCH_HOME=/tmp/

# ENTRYPOINT [ "/usr/local/bin/python3", "-m", "awslambdaric"]
# CMD ["app.lambda_handler"]

COPY ./entry_script.sh /entry_script.sh

RUN chmod 777 /entry_script.sh

RUN chmod -R 777 ./pose_estimation

RUN chmod 777 ./app.py

RUN chmod -R 777 /home

ADD aws-lambda-rie /usr/local/bin/aws-lambda-rie
RUN mkdir -p hub/checkpoints/
RUN cd hub/checkpoints/ && wget https://download.pytorch.org/models/resnet18-f37072fd.pth
# RUN cd /pose_estimation/trt_pose
RUN cd /pose_estimation/trt_pose && python3 setup.py install
# RUN python3 setup.py install

# WORKDIR ./
# RUN cd ./pose_estimation/torch2trt
# CMD ["python3", "setup.py", "install", "--plugins"]

# RUN cd ./pose_estimation/trt_pose
# CMD ["python3", "setup.py", "install"]


ENTRYPOINT [ "/entry_script.sh" ]
CMD ["app.lambda_handler"]
