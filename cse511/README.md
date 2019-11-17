##CSE511_PHASE1_GROUP18 instructions on how to run the main class.
## How to submit the code to Spark

1. Go to project root folder
2. Do not change/remove plugin.sbt and build.sbt. These 2 files are needed for ```sbt assembly``` command.
3. Run ```sbt assembly```. You may need to install sbt in order to run this command.
4. Find the packaged jar in "./target/scala-2.11/CSE511_PHASE1_GROUP18-0.1.0.jar"
5. Submit the jar to Spark using Spark command "./bin/spark-submit"
		Eg: ```spark-submit CSE511_PHASE1_GROUP18-0.1.0.jar result/output rangequery src/resources/arealm10000.csv -93.63173,33.0183,-93.359203,33.219456 rangejoinquery src/resources/arealm10000.csv src/resources/zcta10000.csv distancequery src/resources/arealm10000.csv -88.331492,32.324142 1 distancejoinquery src/resources/arealm10000.csv src/resources/arealm10000.csv 0.1```
