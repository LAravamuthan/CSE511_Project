package phase2

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {
  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

  def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame = {
    // Load the original data from a data source
    var pickupInfo = spark.read.format("com.databricks.spark.csv").option("delimiter", ";").option("header", "false").load(pointPath);
    pickupInfo.createOrReplaceTempView("nyctaxitrips")
    pickupInfo.show()

    // Assign cell coordinates based on pickup points
    spark.udf.register("CalculateX", (pickupPoint: String) => ((
      HotcellUtils.CalculateCoordinate(pickupPoint, 0)
      )))
    spark.udf.register("CalculateY", (pickupPoint: String) => ((
      HotcellUtils.CalculateCoordinate(pickupPoint, 1)
      )))
    spark.udf.register("CalculateZ", (pickupTime: String) => ((
      HotcellUtils.CalculateCoordinate(pickupTime, 2)
      )))
    pickupInfo = spark.sql("select CalculateX(nyctaxitrips._c5),CalculateY(nyctaxitrips._c5), CalculateZ(nyctaxitrips._c1) from nyctaxitrips")
    var newCoordinateName = Seq("x", "y", "z")
    pickupInfo = pickupInfo.toDF(newCoordinateName: _*)
    pickupInfo.createOrReplaceTempView("pickupInfoView")
    pickupInfo.show()

    // Define the min and max of x, y, z
    val minX = -74.50 / HotcellUtils.coordinateStep
    val maxX = -73.70 / HotcellUtils.coordinateStep
    val minY = 40.50 / HotcellUtils.coordinateStep
    val maxY = 40.90 / HotcellUtils.coordinateStep
    val minZ = 1
    val maxZ = 31
    val numCells = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1)
    print(numCells)

    // first select all x , y and z pairs and store it in one table the criteria her is that minX <= x <= maxX and so on for y and z ordered by  day of the month followed by
    // X or Y coordinate

    pickupInfo = spark.sql("SELECT x as X,y as Y,z as Z  FROM pickupInfoView WHERE x>= "
      + minX + " AND x<= " + maxX + " AND y>= " + minY + " AND y<= " + maxY + " AND  z>= " + minZ + " AND z<= "
      + maxZ + " ORDER BY  z, y, x");
    pickupInfo.createOrReplaceTempView("CELLSOFINTEREST")
    //pickupInfo.show()

    // make list of unique pairs if X, Y, Z and their frequency of occurrence in a table  ordered by day of month , y, x
    pickupInfo = spark.sql("SELECT X, Y, Z, count(*) AS FREQUENCY FROM CELLSOFINTEREST GROUP by X, Y, Z ORDER BY Z, Y, X")
    pickupInfo.createOrReplaceTempView("CELLFREQUENCYVIEW")
    //pickupInfo.show()

    // get the Total sum of occurences of all cells in order to get the Mean to be used later.
    val sumTotalOfFrequencies = spark.sql("SELECT  SUM(FREQUENCY) AS TOTAL FROM CELLFREQUENCYVIEW")
    sumTotalOfFrequencies.createOrReplaceTempView("SUMTOTALOFFREQUENCIES")
    //sumTotalOfFrequencies.show()

    val mean = (sumTotalOfFrequencies.first().getLong(0).toDouble / numCells.toDouble)
    print("the mean is ")
    println(mean)


    /* Now we go on to find the second Moment in order to get the standard deviation, VAR(X) = E(X^2) - E(X)^2 , standardDeviation = squart(VAR(X))
    *  For getting the second moment of all entries we create a Table for all squared values using a User defined function as below
    * */

    spark.udf.register("SQUARED", (inputX: Int) => (((inputX * inputX).toDouble)))

    val totalOfSquaredFrequencies = spark.sql("SELECT SUM(SQUARED(FREQUENCY)) AS TOTALOFSQUAREDVALUES FROM CELLFREQUENCYVIEW")
    totalOfSquaredFrequencies.createOrReplaceTempView("SUMSQUAREDTOTALOFFREQUENCIES")
    //totalOfSquaredFrequencies.show()


    val standardDeviation = scala.math.sqrt(((totalOfSquaredFrequencies.first().getDouble(0) / numCells.toDouble) -
      (mean.toDouble * mean.toDouble)))
    print("the standard deviation is ")
    println(standardDeviation)


    spark.udf.register("SurroundingCells", (X: Int, Y: Int, Z: Int, minX: Int, maxX: Int, minY: Int, maxY: Int,
                                            minZ: Int, maxZ: Int) =>
      ((HotcellUtils.calculateSurroundingCellsToConsider(X, Y, Z, minX, minY, minZ, maxX, maxY, maxZ))))

    /*
    * Now we create a view which will have three columns no. of surrounding cells, X, Y and Z of the cell in question and sums of frequency contribution
    * of its the cell in question's surrounding, we self join on CELLFREQUENCYVIEW on values x , y and z varying from n-1 to n+ 1 this table will help us get the
    * z score which require surrounding counts and their frequencies calculating the same is prime objective in creating below view
    * */

    val surroundingCellsResults = spark.sql("SELECT SurroundingCells(CFV1.X, CFV1.Y, CFV1.Z, " + minX + "," + maxX + "," + minY + "," + maxY + "," + minZ + "," + maxZ + ") "
      + "AS neighbour_count, CFV1.X AS X, CFV1.Y AS Y, CFV1.Z AS Z, SUM(CFV2.FREQUENCY) AS tot_surr_fre FROM CELLFREQUENCYVIEW AS CFV1, CELLFREQUENCYVIEW AS CFV2 "
      + "WHERE (CFV2.X = CFV1.X+1 or CFV2.X = CFV1.X or CFV2.X = CFV1.X-1) AND (CFV2.Y = CFV1.Y+1 OR CFV2.Y = CFV1.Y OR CFV2.Y = CFV1.Y-1) "
      + "AND (CFV2.Z = CFV1.Z+1 OR CFV2.Z = CFV1.Z OR CFV2.Z = CFV1.Z-1) GROUP BY CFV1.Z, CFV1.Y, CFV1.X ORDER BY CFV1.Z, CFV1.Y, CFV1.X")
    surroundingCellsResults.createOrReplaceTempView("surroundingCellsView")
    //surroundingCellsResults.show()

    /*now we form the zSCore view using zScore user defined function for each cell*/

    spark.udf.register("ZScore", (surroundingCells: Int, totalFrequencies: Int, numCells: Int, x: Int, y: Int, z: Int, mean: Double, standardDeviation: Double) =>
      ((HotcellUtils.calculateZScore(surroundingCells, totalFrequencies, numCells, x, y, z, mean, standardDeviation))))

    pickupInfo = spark.sql("SELECT ZScore(neighbour_count, tot_surr_fre, " + numCells + ", x, y, z,"
      + mean + ", " + standardDeviation + ") as Gi, X, Y, Z FROM surroundingCellsView ORDER BY Gi DESC");
    pickupInfo.createOrReplaceTempView("zScoresView")
    pickupInfo.show()

    pickupInfo = spark.sql("SELECT X, Y, Z FROM zScoresView")
    pickupInfo.createOrReplaceTempView("finalResultsView")
    pickupInfo.show()

    return pickupInfo
  }
}
