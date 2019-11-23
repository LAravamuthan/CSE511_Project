package cse512

import java.util.logging.{Level, Logger}

import org.apache.log4j.Level
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {
  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame =
{
  // Load the original data from a data source
  var pickupInfo = spark.read.format("com.databricks.spark.csv").option("delimiter",";").option("header","false").load(pointPath);
  pickupInfo.createOrReplaceTempView("nyctaxitrips")
  pickupInfo.show()

  // Assign cell coordinates based on pickup points
  spark.udf.register("CalculateX",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 0)
    )))
  spark.udf.register("CalculateY",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 1)
    )))
  spark.udf.register("CalculateZ",(pickupTime: String)=>((
    HotcellUtils.CalculateCoordinate(pickupTime, 2)
    )))
  pickupInfo = spark.sql("select CalculateX(nyctaxitrips._c5),CalculateY(nyctaxitrips._c5), CalculateZ(nyctaxitrips._c1) from nyctaxitrips")
  var newCoordinateName = Seq("x", "y", "z")
  pickupInfo = pickupInfo.toDF(newCoordinateName:_*)
  pickupInfo.show()

  // Define the min and max of x, y, z
  val minX = -74.50/HotcellUtils.coordinateStep
  val maxX = -73.70/HotcellUtils.coordinateStep
  val minY = 40.50/HotcellUtils.coordinateStep
  val maxY = 40.90/HotcellUtils.coordinateStep
  val minZ = 1
  val maxZ = 31
  val numCells = (maxX - minX + 1)*(maxY - minY + 1)*(maxZ - minZ + 1)

  // YOU NEED TO CHANGE THIS PART

  val pDF = spark.sql("select x,y,z from pickupInfo where x>=" + minX + " and x<= " + maxX + " and y>= " + minY + " and y<= " + maxY + " and z>= " + minZ + " and z<= " + maxZ + " order by z,y,x").persist(); //persist() here creates a Data frame in the memory as select would only use map and hence has no action related to it 
  pDF.createOrReplaceTempView("Df0") //created the Data frame as Df0
  
  val pointsDf = spark.sql("select x,y,z,count(*) as points from Df0 group by z,y,x order by z,y,x").persist();
  pointsDf.createOrReplaceTempView("Df1")

  spark.udf.register("square", (inputVal: Int) => ((HotcellUtils.squaredValue(inputVal))))

  val pointSum = spark.sql("select sum(points) as sumvalue, sum(square(points)) as sumsquare from Df1");
  pointSum.createOrReplaceTempView("pointSum")

  val ptsSum = pointSum.first().getDouble(0)
  val sqSum = pointSum.first().getDouble(1)
  val average = (ptsSum.toDouble/numCells.toDouble)
  val standardDeviation = math.sqrt(((sqSum.toDouble/numCells.toDouble).toDouble - (average.toDouble * average.toDouble).toDouble).toDouble).toDouble

  spark.udf.register("Neighbours", (minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, inputX: Int, inputY: Int, inputZ: Int)
      => ((HotcellUtils.Neighbours(minX, minY, minZ, maxX, maxY, maxZ, inputX, inputY, inputZ))))

  val neighbours = spark.sql("select Neighbours("+minX + "," + minY + "," + minZ + "," + maxX + "," + maxY + "," + maxZ + "," + "temp1.x,temp1.y,temp1.z) as nCount, temp1.x as x, temp1.y as y, temp1.z as z, sum(temp2.points) as totalsum from Df1 as temp1, Df1 as temp2 where (temp2.x = temp1.x+1 or temp2.x = temp1.x or temp2.x = temp1.x-1) and (temp2.y = temp1.y+1 or temp2.y = temp1.y or temp2.y =temp1.y-1) and (temp2.z = temp1.z+1 or temp2.z = temp1.z or temp2.z =temp1.z-1) group by temp1.z,temp1.y,temp1.x order by temp1.z,temp1.y,temp1.x").persist()

  neighbours.createOrReplaceTempView("Df2");
  spark.udf.register("GScore", (x: Int, y: Int, z: Int, mean:Double, sd: Double, countn: Int, sumn: Int, numcells: Int) => ((
      HotcellUtils.gStatistic(x, y, z, mean, sd, countn, sumn, numcells))))
      
  val neighbours1 = spark.sql("select GScore(x,y,z,"+average+","+standardDeviation+",ncount,totalsum,"+numCells+") as gtstat, x, y, z from Df2 order by gtstat desc");
  neighbours1.createOrReplaceTempView("Df3")
      
  val finalresult = spark.sql("select x,y,z from Df3")
  finalresult.createOrReplaceTempView("Df4")


  return pickupInfo // YOU NEED TO CHANGE THIS PART
}
}
