FROM public.ecr.aws/docker/library/python:buster

RUN apt update

RUN apt install -y ffmpeg

COPY lambda_utility/requirements.txt ./

# RUN pip3 install ultralytics
RUN pip3 install -r requirements.txt
RUN pip3 install torch torchvision torchaudio --extra-index-url https://download.pytorch.org/whl/cpu
RUN pip3 install awslambdaric

COPY lambda_utility/tracking ./tracking
COPY lambda_utility/app.py ./app.py
ENV base_path=./

# ENTRYPOINT [ "/usr/local/bin/python3", "-m", "awslambdaric"]
# CMD ["app.lambda_handler"]

COPY ./entry_script.sh /entry_script.sh
RUN chmod -R 777 /tracking/
RUN chmod 777 /entry_script.sh
RUN chmod 777 /app.py
# RUN chmod -R 777 ./tracking/yolov8s/weights/yolov8s.pt
# RUN chmod -R 777 ./tracking/monodepth/Monodepth2/models/mono_1024x320/
ADD aws-lambda-rie /usr/local/bin/aws-lambda-rie

ENTRYPOINT [ "/entry_script.sh" ]
CMD ["app.lambda_handler"]

