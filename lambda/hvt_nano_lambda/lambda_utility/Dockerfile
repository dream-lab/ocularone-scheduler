FROM public.ecr.aws/docker/library/python:buster

RUN apt update

RUN apt install -y ffmpeg

RUN useradd -ms /bin/bash ocularone

USER ocularone

WORKDIR /home/ocularone


COPY lambda_utility/requirements.txt ./

#RUN pip3 install ultralytics
RUN pip3 install -r requirements.txt
#RUN pip install ultralytics
RUN pip3 install torch torchvision torchaudio --extra-index-url https://download.pytorch.org/whl/cpu
COPY lambda_utility/tracking ./tracking
COPY lambda_utility/app.py ./
ENV base_path=./

CMD ["app.lambda_handler"]
