# DJUNGELSKOG

DJUNGELSKOG is a STUN server that was developed as a solution to an optional project in the course IDATT2104 - Network Programming.
The server is written in Java, and has the following functionality:
  * Attributes:
    * Mapped-Address
        * This gives us backwards compatibility with RFC 3489
    * XOR-Mapped-Address
        * This is how the request's public ip and port is returned to them
    * Unknown attributes
        * If the client sends a comprehension-required attribute that we are not able to comprehend, then this attribute
        is returned back to the client listing the attributes our server can't comprehend, together with an error code
    * Error codes
        * If the client sends a malformed request then the server returns an error code attribute with error code 400
        * If the client sends attributes which the server is not able to handle then the server returns an error code 
      with error code 420
  * What exactly defines a malformed request?
    * When our server checks whether a message is a valid stun-request it checks the following, in order:
        1. Are the first two bits zero?
        2. Is the message a request and not a response?
        3. Is message a binding method?
        4. Is the message at least 20 bytes
        5. Is the message at most 255 bytes (arbitrarily chosen upper limit)
        6. Does the header include a magic cookie in the correct location
    * If all these checks pass, then our server checks whether the message is a request or an indication. 
    If it is a request then a reply is sent, if it is an indication then the message is silently discarded as indications
      warrant no response
  * Possible connections
    * Our application supports both udp-packets and tcp-connections. We have chosen a worker-thread model to handle 
  these connections, described in detail in the below section
      
#### Continuous deployment:
Our latest CI/CD can be found on the page:
https://github.com/ikhovind/jstun/actions

The actual process of setting up our CD is described in detail in the dependencies section
## Thread model
Our primary instance of the server is running on a free aws VM. This VM currently only has two threads, this is quite
few and even though we are not using a thread per client, even with worker threads this can become insufficient. 

Our current thread model makes use of the java-library ExecutorService. This allows us to create a fixed thread pool 
without having to implement worker threads ourselves. 

Even though our aws VM only has two threads, our current thread pool consists of three threads, we chose this in order
to take advantage of context switching on our VM. It also allows to have dedicated threads to listen for TCP connections
and UDP packets, while the last threads formulates and sends the replies to these packets and connections.

Because we use two out of three threads to listen for connections, any other tasks handled by the server are handled in 
an event loop.
## Further Work
An important step before any further development takes place would be to further develop our automated tests. 

A large challenge to our current development is that we are unable to test much functionality in a realistic environment
without writing and entirely new client. So writing larger tests would be one of the first steps for continued development.


If the program was to for example implement verifications methods such as usernames or passwords the server would have 
to test these. We would have to write a client that would be able to send a request which uses a username and password 
and that would verify that the response of the server was correct.

The attributes that are able to be implemented without implementing a credential mechanism would be username (?), 
fingerprint, message-integrity (?), Nonce, software and alternate server.

The attributes we would be able to implement with a credential mechanism would be Realm

####Weaknesses
The largest weakness that currently affect our server the most is the lack of being able to handle any attributes sent 
by the client. We have implemented the necessary error handling to inform the client of this, but it would be better 
to be able to actually handle for example requests pertaining to long term or short term credentials


## Dependencies
Our three dependencies are Log4j, Junit and Maven

The licences for these two dependencies are 
the [Eclipse public license - v 2.0 for Junit](https://github.com/junit-team/junit5/blob/main/LICENSE.md), 
the [Apache license, Version 2.0 for log4j](https://logging.apache.org/log4j/2.x/license.html) 
and the [Apache Software License 2.0 for Maven](https://maven.apache.org/ref/3.0/license.html). 
All of these are open source licenses, allowing us to use them free of charge in our own projects.

####Junit
Junit is used in our project to write our unit tests. They give us access to methods such as assert(), while allowing 
us to tag our tests, which simplify automatically running them in our CI-pipeline
####Log4j
Log4j is used to log the state of the program to both the command line and to write the logs to a file called all.txt
in the logs directory. 
####Maven
Maven is used to handle our dependencies and to help in automating our integration and deployment

It is also used in order to compile our project into a jar which is then automatically run in our
continuous deployment
####Documentation for our two dependencies can be found here:
https://junit.org/junit4/javadoc/latest/index.html for junit4

####And here:

https://logging.apache.org/log4j/2.x/javadoc.html for log4j
## Installation
There is currently an instance of the server running on stun:13.48.195.80:3478, this can be tested for all functionality,
both with a UDP and a TCP connection

If you want to host an instance of the server yourself then you only have clone the repo, then run the maven project 

1. If you don't have git, maven and java installed, please install them, this project is written in java 11, your 
   installation needs to be at least java 11 as well
   * [Follow the steps here to install java for your operating system](https://java.com/en/download/help/download_options.html#solaris)
   * [Follow this tutorial to install maven for your operating system](https://www.baeldung.com/install-maven-on-windows-linux-mac)
    * [Follow the steps here to install git for your operating system](https://github.com/git-guides/install-git)
2. Git clone our repo:
    * git clone https://github.com/ikhovind/jstun.git
3. cd into the cloned repo:
    * cd jstun
4. Compile and run the project using maven, remember to specify that Stun is the main class:
    * mvn compile exec:java -Dexec.mainClass="Stun"

There should now be an instance of the server running locally and you should see the following output from the server in 
your command line:
```
09:14:58.815 [Stun.main()] INFO  Stun - listening to port: 3478
09:14:58.819 [pool-2-thread-1] INFO  Stun - waiting for udp-packet
09:14:58.836 [pool-2-thread-2] INFO  Stun - waiting for tcp-connection
```
Make sure that your port 3478 is open and available before you run 
## Unit tests
Running the unit tests are quite simple in a maven project, simply run:
* mvn test

In a cloned repo like the one described under installation. You should see an output similar to this:

````
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running ResponseTest

*long logging output between these two*

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.429 sec

Results :

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0

````

##Pipeline
Setting up the pipeline for our continous deployment was likely the most time-consuming part of this project, with many
attempted solutions which ended up being just a waste of time.

Our attempts at CD were as following:
1. Our first attempt was to set up an azure web-app, the CD here worked quite well as azure was able to set it up 
automatically, but we were unable to find any output of the app, and were therefore unable to both debug and confirm 
   that our server was actually receiving packets
   

2. Our second attempt was to set up a virtual machine on Azure, but since we had a bad experience with the web-app, we 
did not put much effort into this VM and quickly migrated to AWS
   

3. On AWS we first created a vm and managed to get the application running there, however, our CD didn't work, and we had 
to update the app manually. This did however allow us to confirm that our packets were formulated correctly and that our
   server worked when hosted online
   

4. As we struggled to get our VM working, we attempted to set up an elastic beanstalk with automatic integration from our
Github repo. We never actually got this working, and it was excessively complicated for the simple functionality we were
   looking for, we therefore went back to the aforementioned VM and followed the instructions in our lesson on CD more 
   closely
   

5. After trying and failing to copy the repo into the VM with scp, we finally got it working with a rsync-template for 
Github actions, this allowed us to keep the VM's project updated, and from there setting up systemd in order to compile
   and run the project was smooth sailing. 

Our pipeline currently consists of automatically formatting our code according to the Google Java standard, 
automatically building and running unit tests as well as automatically deploying to our aws VM

Our VM runs Ubuntu and uses Systemd in order to continuously run the server

The deployment is performed by using rsync to copy the entire project into the VM. We then use ssh to package
the project into a jar, which is then run by the systemd-configuration

Our .yml-file can be found here:
https://github.com/ikhovind/jstun/blob/main/.github/workflows/maven.yml

A list of our workflows can be found here:
https://github.com/ikhovind/jstun/actions
