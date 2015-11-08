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
There are 4 different services in each SDFS server : the failure detector, the election layer, the file server layer and the master layer. The entire system consists of many identical SDFS servers and one SDFS proxy server. The SDFS proxy server is assumed to be the endpoint for this SDFS service. So all clients communicate with this SDFS proxy server to get the current master among the SDFS servers. The SDFS proxy server acts as the introducer in group membership protocol and does not store any file as part of its service. If the SDFS proxy fails we assume that no new node can join the SDFS service cluster and no client requests can be handled until the SDFS proxy is back again. The failure detector uses SWIM algorithm that we implemented in MP2. The election layer uses a simplified bully algorithm. The algorithm is as follows : If a node detects failure of master using the failure detector layer, it calls for election and checks if it is next in line to be master. If not, it waits for coordinator message (and times out if not received). If it is the next in line to be master, it multicasts coordinator messages to all members in the group which send back okay messages if they agree. The check if it is potential master is possible by using group membership list. Finally the SDFS proxy server is let known who the master is. 
The master stores a list of which files are replicated by which nodes and periodically checks the replication of all files. if replication drops to less than 3 (due to some node failure), it asks a randomly chosen alive server to replicate the file. The master also handles requests from clients. During put, the client requests 3 servers from the master to put data in and then the client writes to as many of these 3 as it can. So we are doing active replication. This ensures that despite 2 failures at a time, we still have one copy of the file still left. For get request, the client gets list of servers with file from master and then gets the file from server. For delete, the client sends the request to the master, the master then relays the request itself to the individual servers containing the file.
 
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
3. run ```scripts/run_sdfsclient.sh ```
4. Input fileops commands into client