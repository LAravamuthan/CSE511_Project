package phase1

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{SaveMode, SparkSession}

object SparkSQLExample {

  Logger.getLogger("org.spark_project").setLevel(Level.ERROR)
  Logger.getLogger("org.apache").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)
  Logger.getLogger("com").setLevel(Level.ERROR)

  def main(args: Array[String]) {
    val argss = Array(
      "src/examples/output",
      "rangequery",
      "src/resources/arealm10000.csv",
      "-93.63173,33.0183,-93.359203,33.219456",
      "rangejoinquery",
      "src/resources/arealm10000.csv",
      "src/resources/zcta10000.csv",
      "distancequery",
      "src/resources/arealm10000.csv",
      "-88.331492,32.324142",
      "1",
      "distancejoinquery",
      "src/resources/arealm10000.csv",
      "src/resources/arealm10000.csv",
      "0.1"
    )

    val spark = SparkSession
      .builder()
      .appName("cse511-phase1")
      .config("spark.some.config.option", "some-value")
      .master("local[*]")
      .getOrCreate()

    paramsParser(spark, args)

    spark.stop()
  }

  private def paramsParser(spark: SparkSession, args: Array[String]): Unit = {
    var paramOffset = 1
    var currentQueryParams = ""
    var currentQueryName = ""
    var currentQueryIdx = -1

    while (paramOffset <= args.length) {
      if (paramOffset == args.length || args(paramOffset).toLowerCase.contains(
        "query"
      )) {
        // Turn in the previous query
        if (currentQueryIdx != -1)
          queryLoader(
            spark,
            currentQueryName,
            currentQueryParams,
            args(0) + currentQueryIdx
          )

        // Start a new query call
        if (paramOffset == args.length) return

        currentQueryName = args(paramOffset)
        currentQueryParams = ""
        currentQueryIdx = currentQueryIdx + 1
      } else {
        // Keep appending query parameters
        currentQueryParams = currentQueryParams + args(paramOffset) + " "
      }

      paramOffset = paramOffset + 1
    }
  }

  private def queryLoader(
                           spark: SparkSession,
                           queryName: String,
                           queryParams: String,
                           outputPath: String
                         ): Unit = {

    println(queryName, queryParams, outputPath)
    var queryResult: Long = -1
    val queryParam = queryParams.split(" ")

    if (queryName.equalsIgnoreCase("RangeQuery")) {
      if (queryParam.length != 2)
        throw new ArrayIndexOutOfBoundsException(
          "[CSE511] Query " + queryName + " needs 2 parameters but you entered " + queryParam.length
        )
      queryResult =
        SpatialQuery.runRangeQuery(spark, queryParam(0), queryParam(1))
    } else if (queryName.equalsIgnoreCase("RangeJoinQuery")) {
      if (queryParam.length != 2)
        throw new ArrayIndexOutOfBoundsException(
          "[CSE511] Query " + queryName + " needs 2 parameters but you entered " + queryParam.length
        )
      queryResult =
        SpatialQuery.runRangeJoinQuery(spark, queryParam(0), queryParam(1))
    } else if (queryName.equalsIgnoreCase("DistanceQuery")) {
      if (queryParam.length != 3)
        throw new ArrayIndexOutOfBoundsException(
          "[CSE511] Query " + queryName + " needs 3 parameters but you entered " + queryParam.length
        )
      queryResult = SpatialQuery.runDistanceQuery(
        spark,
        queryParam(0),
        queryParam(1),
        queryParam(2)
      )
    } else if (queryName.equalsIgnoreCase("DistanceJoinQuery")) {
      if (queryParam.length != 3)
        throw new ArrayIndexOutOfBoundsException(
          "[CSE511] Query " + queryName + " needs 3 parameters but you entered " + queryParam.length
        )
      queryResult = SpatialQuery.runDistanceJoinQuery(
        spark,
        queryParam(0),
        queryParam(1),
        queryParam(2)
      )
    } else {
      throw new NoSuchElementException(
        "[CSE511] The given query name " + queryName + " is wrong. Please check your input."
      )
    }

    import spark.implicits._
    val resultDf = Seq(queryName, queryResult.toString).toDF()
    resultDf.write.mode(SaveMode.Overwrite).csv(outputPath)
  }

}


