# Failure Detector
This project implements faliure detector based on SWIM which provides complete and weakly consistent membership list
guarantees in a scalable fashion.

## Design
In this MP we implemented Failure detector module as a set of daemon threads along with the main application thread. 
We spawned three threads in total apart from the main application. Two of those threads were responsible for sending and receiving failure detector module messages over UDP, and one final thread for TCP connection only used to get membership list from introducer when a process joins the group.

We used a special introducer node which was responsible for making new processes join the group. Although we used a special introducer node in our implementation it is fault tolerant in the way that, even if introducer fails rest of the processes can continue failure detector module without any problem, and introducer failure only limits joining new processes in the group. Our application is fast (detects failure within 3 seconds and disseminates within 6 sec), complete and consistent with very low false positive rate. We took into account various cases involving introducer failure, high network latencies and scalability while implementing the MP.

 
## Package Dependencies
- Java 7
- Maven

## Instructions
### Step 1 - Set up code in VM's
The following are the steps to get code and compile in application in a VM:
1. ssh into the vm machine : Eg - ```ssh <NETID>@fa15-cs425-gNN-XX.cs.illinois.edu```
2. Type ```git clone https://gitlab-beta.engr.illinois.edu/cs425-agupta80-pmazmdr2/mp2-failure-detector.git```
3. cd into the project root directory
4. run ```mvn package```. You should see build success.

### Step 2 - Run Introducer
Default scripts assume introducer to run at VM-1
1. ssh into the vm machine 1 : ```ssh <NETID>@fa15-cs425-gNN-01.cs.illinois.edu```
2. cd into the project root directory
3. run ```scripts/run_introducer.sh > ~/log.txt``` 

### Step 3 - Run Normal processes
You can any any number of processes you want in the group
1. ssh into the vm machine: ```ssh <NETID>@fa15-cs425-gNN-XX.cs.illinois.edu```
2. cd into the project root directory
3. run ```scripts/run_normal.sh > ~/log.txt``` 

