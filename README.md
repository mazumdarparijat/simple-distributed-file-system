# Simple Distributed File System (SDFS)
SDFS is a simplified version of HDFS (Hadoop Distributed File System)
and is scalable as the number of servers increases. Data stored in
SDFS is tolerant to two machine failures at a time. After failure(s) we
ensure that data is re-replicated quickly so that another failure that happens soon
after is tolerated. SDFS files are immutable - once inserted, they are never changed, although they
may be deleted. SDFS is a flat file system, i.e., it has no concept of directories,
although filenames are allowed to contain slashes.

The allowed file ops include: put localfilename sdfsfilename
(from local dir), get sdfsfilename localfilename (fetches to local dir),
delete sdfsfilename, ls (list files present in SDFS) and list sdfsfilename (lists the VM's having replica of the file).

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
2. Type ```git clone https://gitlab-beta.engr.illinois.edu/cs425-agupta80-pmazmdr2/mp3-sdfs.git```
3. cd into the project root directory
4. run ```mvn package```. You should see build success.

### Step 2 - Run SDFSProxy
Default scripts assume SDFSProxy to run at VM-1
1. ssh into the vm machine 1 : ```ssh <NETID>@fa15-cs425-gNN-01.cs.illinois.edu```
2. cd into the project root directory
3. run ```scripts/run_sdfsproxy.sh > ~/log.txt``` 

### Step 3 - Run SDFSFileServer
You can run any number of Fileservers you want in the group
1. ssh into the vm machine: ```ssh <NETID>@fa15-cs425-gNN-XX.cs.illinois.edu```
2. cd into the project root directory
3. run ```scripts/run_sdfsserver.sh > ~/log.txt```

### Step 4 - Run SDFSClient
You can run any number of clients you want in the group and at any machine
1. ssh into the vm machine: ```ssh <NETID>@fa15-cs425-gNN-XX.cs.illinois.edu```
2. cd into the project root directory
3. run ```scripts/run_sdfsclient.sh > ```
4. Input fileops commands into client