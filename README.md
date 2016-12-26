# Setting up Apache Ambari on EC2

This guide describes to to manually setup Ambari on EC2 for the purpose of setting up a custom Hadoop environment using HDP (Hortonworks' build packages)

Used this video for walkthrough: [here](https://www.youtube.com/watch?v=6-RY4Ll6ABU)

Steps:

## Create 1 m4.large instance for Ambari Head
* Redhat enterprise
* Protect against accidental termination
* Add 100GB magnetic storage (delete on termination)
* Create a new security group with all in/out ports open from any IP
* Create or use appdev pemfile

## Setup base image
* SSH as ec2-user to public ip address (ssh -i ec2-user@54.....)
* Format 100GB drive (sudo mkfs -t ext4 /dev/xvdb)
* Mount the drive (sudo mkdir /grid && sudo mount /dev/xvdb /grid)
* Add info to /etc/fstab to be pernament mount
  * (/dev/xvdb /grid /ext4 defaults.nofail 0 2)
* Reboot and reconnect
* Disable SELinux (set SELINUX in /etc/sysconfig/selinux to disabled)
* Disable firewall (I DID NOT DO THIS FOR CUAPPDEV CLUSTER)
  * sudo systemctl disable firewalld
  * sudo service firewalld stop
* Install ntp (sudo yum install ntp)
* Start ntpd (sudo systemtctl enable ntpd && sudo systemctl start ntpd)
* Generate ssh key for connecting between machines (ssh-keygen -t rsa)
* Add new key to authorized_keys (cat .ssh/id_rsa.pub >> .ssh/authorized_keys)
  * This is for connecting between the clusters easily
* Go to AWS dashboard and create image based on this instance

## Spinning up other machines
* Go into AWS Console and create new instances
  * Same m4.large size as ambari instance
* Under configure instance Add the following lines:
  * sudo mkdir /grid
  * sudo mkfs -t ext4 /dev/xdvb
  * sudo mount /dev/xdvb /grid
* Once created, add the list of hosts to all machines (/etc/hosts)

## Install Ambari
* Connect to the Ambari Machine
* Install wget (sudo yum install wget)
* Add the hdfp ambari repo ()
* Update yum (sudo yum repolist)
* Install ambari server (sudo yum install ambari-server)
* Setup ambari-server (sudo ambari-server setup)
  * Use defaults (No custom user account, java8, accept agreement, no advanced db config)
* Start ambari-server (sudo ambari-server start)

## Create Cluster
* Connect through browser to ambari at :8080
* Name the cluster
* Select HDP2.4 (HDP version 2.4)
  * Under Advanced Options unselect all repos that arent rhel7
* For target hosts, copy the privateDNS addresses for all machines
* For SHH key, paste in the id_rsa file that was generated
* User is ec2-user

# Setting up a DC/OS cluster

This guide describes how to manually setup a DC/OS cluster on AWS machines.

## System Requirements

Setting up DC/OS requires the following ec2 servers
* A bootstrap node
  * This node performs the initial setup of DC/OS on the rest of the nodes in your cluster, installing any necessary packages.
  * This node is temporary and can be relatively low power (t2.micro will do)
* 1, 3, or 5 master nodes running Red Hat or CentOS
  * Master nodes receive jobs and distribute those jobs to agent nodes. DC/OS recommends at least 3 master nodes for fault tolerance, but you can get away with just a single master node.
  * Recommended m3.xlarge instances, but m3.medium and up will suffice.
* Any number of agent nodes running Red Hat or CentOS
  * Agent nodes receive jobs from master nodes.
  * Recommended m3.xlarge instances, but m3.medium and up will suffice.

## Setting up Servers

Deploy the servers described above on Amazon EC2 through the GUI or the CLI. All the nodes should
be deployed in the same availability zone (ex. 2c).

### Security Group

For your bootstrap node's security group, set HTTP (80) and SSH (22) ports to available anywhere, and also
set port 9000 to available anywhere. You will be shutting down the bootstrap node right after the install
process so security isn't a huge deal.

For the cluster nodes (masters and agents) set SSH and HTTP to available anywhere, and additionally,
add custom TCP rules for ports 1024-65535 for inbound traffic from this security group and the
bootstrap node's security group. The image below is what your resulting security group should look like.

![alt text](images/security-group.png "security group")

### Updating Software

ssh into each of your master and agent servers, and run `sudo yum update -y`.

## Configuring DC/OS cluster

ssh into your bootstrap node and download the DC/OS installer.

```bash
curl -O https://downloads.dcos.io/dcos/stable/dcos_generate_config.sh
```

and start the DC/OS GUI installer.

```bash
sudo bash dcos_generate_config.sh --web
```

Next, point your browser to `http://bootstrap-node-ip:9000. You should be met by the following landing page.
click Begin Installation.

![alt text](images/dcos-gui-install.png "landing page")

Specify your deployment settings on the following page.

![alt text](images/dcos-gui-preflight.png "deployment settings")

### Settings

#### Master Private IP List

Comma separated list of all the private ip's of your masters.

#### Agent private IP List

Comma separated list of all the private ip's of your private agents.

#### Agent Public IP List

Comma separated list of all the private ip's of your public agents.

#### Master Public IP

The public IP for one of your master nodes.

##### SSH username

The username when you SSH into any of the nodes in your cluster (eg. ec2-user).

#### SSH Listening Port

Should be 22.

##### Private SSH Key

The SSH key you created when you setup your cluster. Copy and paste the contents of the .pem file into here.

### Deployment

Hit run pre-flight, followed by deploy, followed by run post-flight. After the entire setup is complete, you
will be redirected to login to your DC/OS cluster GUI interface. At this time, you can shutdown the bootstrap
node.
