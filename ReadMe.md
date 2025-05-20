# Ocularone Scheduler
Ocularone Scheduler offers heuristics for scheduling of inferencing tasks over real-time drone videos for helping navigate Visually Impaired Persons (VIPs). It's real-time deadline-aware schedulier operates across edge accelerators and cloud. The heuristic algorithms try to maximize the Quality of Service (QoS) and Quality of Experience (QoE). These algorithms are validated using a containerized emulation-based testbed based upon [UltraViolet](https://github.com/dream-lab/UltraViolet). We also perform practical validation of these scheduling strategies on real hardware using edge accelerators and DJI Tello nano-drones and compare them against state-of-the-art baselines.


# Attribution
**If you use the Ocularone Scheduler, please cite our work!**

"Adaptive Heuristics for Scheduling DNN Inferencing on Edge and Cloud for Personalized UAV Fleets", Suman Raj and Radhika Mittal and Harshil Gupta and Yogesh Simmhan, *Future Generation Computer Systems*, 2025, [10.1016/j.future.2025.107874](https://doi.org/10.1016/j.future.2025.107874).

```
@article{RAJ2025107874,
  title = {Adaptive heuristics for scheduling DNN inferencing on edge and cloud for personalized UAV fleets},
  journal = {Future Generation Computer Systems},
  pages = {107874},
  year = {2025},
  issn = {0167-739X},
  doi = {https://doi.org/10.1016/j.future.2025.107874},
  url = {https://www.sciencedirect.com/science/article/pii/S0167739X25001694},
  author = {Suman Raj and Radhika Mittal and Harshil Gupta and Yogesh Simmhan},
  keywords = {Edge and cloud computing, Unmanned aerial vehicles, Intelligent offloading, Deadline-aware scheduling, DNN inferencing}
}
```

---
---
## System Requirements

To run experiments, you would need a machine with atleast 30 vCPUs, as each edge emulation container gets pinned to CPU cores. We use a host machine with an ``` Intel Xeon 6208U CPU@2.9 GHz with 30 vCPUs and 128 GB RAM, running Ubuntu v18.04```.  

Other requirements include (version tested on):  
- openjdk (version 11.0.19)
- Apache Maven (version 11.0.19 from https://maven.apache.org/install.html)
- Python 3.6.9
- tmux (to help with terminal and session management)
<!-- ultraviolet dependencies () -->
- [UltraViolet](https://github.com/dream-lab/UltraViolet)
  

---
---
## Environment Setup 

### UltraViolet Setup

- Set up Ultraviolet as per instructions in https://github.com/dream-lab/UltraViolet/blob/master/README.md. You could do either automatic installation or manual installation from Step 1-6. Once it's installed, please follow the steps below: 
- Create three new terminal windows: one each for the frontend server, controller and worker
  - In the first terminal window, start the frontend server.
  ```
  sudo MaxiNetFrontendServer
  ```
  - In the second terminal window, start the controller.
  ```
  cd <Path to Your UltraViolet directory>/pox
  python3 pox.py forwarding.l2_learning
  ```
  - In the third terminal window, start the worker.
  ```
  sudo MaxiNetWorker
  ```
  Enter choice 1 for each IP choice when prompted.

These can be left running across experiment runs and need not be shut down or restarted each time.

### Drone Containers Setup

- Ensure you have a functional docker setup on your host machine. (We use ```Docker Version 20.10.8, build 3967b7d```)
- Pull the drone docker image from dockerhub using
```
sudo docker pull radmittal3/buddy_drone:warmup
```
- Ensure that docker image ```radmittal3/buddy_drone:warmup``` is present on your host machine with the command
```
sudo docker images
```
```radmittal3/buddy_drone:warmup``` must be one of the entries.
- Tag the docker image with
```
sudo docker tag radmittal3/buddy_drone:warmup buddy_drone:warmup
```
as the Ultraviolet setup look for a docker image referenced as ```buddy_drone:warmup```.

### Edge Containers Setup
- Build the edge base docker image using the ``` <path to ocularone scheduler>/edge_files/Dockerfile_edge ```. Name and tag the image as ```edge_device:1.0```.
- Ensure that docker image ```edge_device:1.0``` is present on your host machine with the command as the bringup files look for a docker image referenced as ```edge_device:1.0```.
- Create an edge container with the docker image and access its CLI using 
```
sudo docker run -it edge_device:1.0 bash
```
- In the edge container's CLI, copy the required contents such as the inferencing script, post processing scripts etc from the host machine with ```scp```.
```
scp <host machine's username>@<host machine's IP address>:<path to ocularone scheduler>/edge_files/* .
``` 
- In the edge container, ensure that ```executor.sh``` points to the files copied in the above step. If not, edit the paths. Check and edit the contents with
```
vi executor.sh
```
You may optionally use VS code's Dev Containers extension (https://code.visualstudio.com/docs/devcontainers/attach-container) to attach to and develop inside the container.
- Once all changes within the edge container are complete, CHANGES MUST BE SAVED to your edge docker image by running
```
sudo docker commit <edge container ID> edge_device:1.0
```
``` sudo docker ps``` is useful to find the ID of running docker containers. ```docker ps -a``` may be used in case the container has been stopped.

### AWS Lambda (Cloud) Functions Setup

- Use the folders in ```<path to ocularone scheduler>/lambda``` for each DNN model. We provide the models for ``` BODY_POSE_ESTIMATION, CROWD_DENSITY & MASK_DETECTION``` DNN models, which may be used for testing the scheduler.
- Use AWS SAM CLI to deploy the models as AWS functions. (Check out https://aws.amazon.com/blogs/compute/using-container-image-support-for-aws-lambda-with-aws-sam/)
- Create function URLs for each function from the AWS Lambda Console and tune parameters including memory allocated as per requirements (described in the experimental setup section of https://arxiv.org/abs/2412.20860v1).
- Add the function URLs to ```lines 261-266``` in ```<path to ocularone scheduler>/src/main/java/org/dreamlab/ApplicationDetails/Application.java``` and ```mock_edge.py``` in ```edge_files```.


### Ocularone Scheduler Setup
- Clone this repository into your host machine using
```
https://github.com/dream-lab/ocularone-scheduler
```
- Build the jar file from the repository root.
```
cd <path to ocularone scheduler>
sudo mvn clean compile assembly:single
```

- ```<Path to Your UltraViolet directory>/ocularone-test-pipeline/deploy_nwvar_*.py``` are the deployment files. Modify paths in these files to point to your home directory in the docker volumes.
- Ensure that the set paths in ```<Path to Your UltraViolet directory>/ocularone-test-pipeline/deploy_nwvar_og.py``` point to the dataset in ```<path to ocularone scheduler>/video_dataset```.
- These will be mounted on the edge containers during experiments. 


---
---
## Running Experiments
- Open a new terminal window. To run an experiment with, say 2 drones per edge, run
```
cd <Your UltraViolet directory>/ocularone-test-pipeline\d20-config
cp switches7X2.json switches.json
cp infra_config7X2.json infra_config.json
```
- Choose the algorithm and load for the experiment from the below list.

| Load    | # of DNN Models |
| :--------: | :-------: |
| LIGHT  | 2    |
| MEDIUM  | 4    |
| HEAVY  | 6    |

| Algo_number    | Algorithm |
| :--------: | :-------: |
| 1  | FIFO    |
| 3  | EDF    |
| 4  | CLD    |
| 5  | SJF    |
| 7  | HBF    |
| 9  | HUF    |
| 52  | DEM    |
| 62  | E+C    |
| 31  | DEMS    |
| 35  | GEMS    |
| 91  | DEMS-A    |
| 1113    | D3+Kalmia Variant Baseline SOTA 1 |
| 1112 | DEDAS SOTA 2    |

- In another terminal window, run
```
cd <Your UltraViolet directory>/ocularone-test-pipeline
python3 deploy_nwvar_og.py <Algo_number > <load>
```
- For latency variability experiments, run 
```
python3 deploy_nwvar_og_artif_lat.py <Algo_number > <load>
```
- For bandwidth variability experiments, run 
```
python3 deploy_nwvar_bw_var.py <Algo_number > <load>
```

### Check Logs
- The logs are written to the locations specified in the ```<Your UltraViolet directory>/ocularone-test-pipeline\deploy_nwvar_*.py``` file being used for the experiment. The primary run logs to track the experiment for each edge are present in ```<path to log directory>/algo_<Algo_number>_load_<load>/Edge_<edgeid>/Edge<edgeid>_algo_<Algo_number>_load_<load>_java_client.log```.

---
---
## References for External DNN models
BODY_POSE_ESTIMATION - https://github.com/NVIDIA-AI-IOT/trt_pose  
CROWD_DENSITY - https://docs.ultralytics.com/models/yolov8/  
MASK_DETECTION - https://github.com/AIZOOTech/FaceMaskDetection

# CREDITS
- Suman Saj, PhD Candidate, IISc (sumanraj@iisc.ac.in)
- [Yogesh Simmhan](https://cds.iisc.ac.in/faculty/simmhan/), IISc (simmhan@iisc.ac.in)


Copyright 2005 DREAM:Lab, Indian Institute of Science
