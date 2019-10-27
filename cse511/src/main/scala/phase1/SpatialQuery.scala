package phase1

import org.apache.spark.sql.SparkSession

object SpatialQuery extends App {

  def ST_Contains(queryRectangle: String, pointString: String): Boolean = {
    val rect = queryRectangle.split(",")
    val coordinates = rect.map(_.trim.toDouble)

    val x0 = math.min(coordinates(0), coordinates(2))
    val x1 = math.max(coordinates(0), coordinates(2))

    val y0 = math.min(coordinates(1), coordinates(3))
    val y1 = math.max(coordinates(1), coordinates(3))

    val points = pointString.split(",")
    val x = points(0).trim.toDouble
    val y = points(1).trim.toDouble

    if (x < x0 || x > x1 || y < y0 || y > y1)
      return false
    else
      return true
  }

  def ST_Within(
                 pointString1: String,
                 pointString2: String,
                 distance: Double
               ): Boolean = {
    val point1 = pointString1.split(",")
    val x = point1.map(_.trim.toDouble)

    val point2 = pointString2.split(",")
    val y = point2.map(_.trim.toDouble)

    var d = Math.sqrt(Math.pow((x(0) - y(0)), 2) + Math.pow((x(1) - y(1)), 2))

    if (d <= distance)
      return true
    else
      return false
  }

  // arg1: path to CSV of points P
  // arg2: rectangle R
  def runRangeQuery(spark: SparkSession, arg1: String, arg2: String): Long = {

    // Load contents of CSV as dataframe
    val pointDf = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg1);

    // Load dataframe into point view
    pointDf.createOrReplaceTempView("point")

    // YOU NEED TO FILL IN THIS USER DEFINED FUNCTION
    spark.udf.register(
      "ST_Contains",
      (queryRectangle: String, pointString: String) =>
        ((ST_Contains(queryRectangle, pointString)))
    )

    // All rows from point view which lie within the rectangle defined by arg2
    val resultDf = spark.sql(
      "select * from point where ST_Contains('" + arg2 + "',point._c0)"
    )
    resultDf.show()
    println("count1", resultDf.count())

    return resultDf.count()
  }

  // arg1: path to CSV of points P
  // arg2: path to CSV of rectangles R
  def runRangeJoinQuery(
                         spark: SparkSession,
                         arg1: String,
                         arg2: String
                       ): Long = {

    // Load contents of CSV as dataframe
    val pointDf = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg1);

    // Load dataframe into point view
    pointDf.createOrReplaceTempView("point")

    // Load contents of CSV as dataframe
    val rectangleDf = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg2);

    // Load dataframe into rectangle view
    rectangleDf.createOrReplaceTempView("rectangle")

    // YOU NEED TO FILL IN THIS USER DEFINED FUNCTION
    spark.udf.register(
      "ST_Contains",
      (queryRectangle: String, pointString: String) =>
        ((ST_Contains(queryRectangle, pointString)))
    )

    // Select all rectangle, point pairs where point lies within rectangle
    val resultDf = spark.sql(
      "select * from rectangle, point where ST_Contains(rectangle._c0, point._c0)"
    )
    resultDf.show()
    println("count2", resultDf.count())

    return resultDf.count()
  }

  // arg1: path to CSV of points P
  // arg2: center point O
  // arg3: distance D
  def runDistanceQuery(
                        spark: SparkSession,
                        arg1: String,
                        arg2: String,
                        arg3: String
                      ): Long = {

    // Load contents of CSV as dataframe
    val pointDf = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg1);

    // Load dataframe into point view
    pointDf.createOrReplaceTempView("point")

    // YOU NEED TO FILL IN THIS USER DEFINED FUNCTION
    spark.udf.register(
      "ST_Within",
      (pointString1: String, pointString2: String, distance: Double) => ((ST_Within(pointString1, pointString2, distance)))
    )

    // Select all points that lie at distance D from point O
    val resultDf = spark.sql(
      "select * from point where ST_Within(point._c0,'" + arg2 + "'," + arg3 + ")"
    )
    resultDf.show()
    println("count3", resultDf.count())

    return resultDf.count()
  }

  // arg1: path to CSV of points S1
  // arg2: path to CSV of points S2
  // arg3: distance D
  def runDistanceJoinQuery(
                            spark: SparkSession,
                            arg1: String,
                            arg2: String,
                            arg3: String
                          ): Long = {

    // Load contents of CSV as dataframe
    val pointDf = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg1);

    // Load dataframe into point1 view
    pointDf.createOrReplaceTempView("point1")

    // Load contents of CSV as dataframe
    val pointDf2 = spark.read
      .format("com.databricks.spark.csv")
      .option("delimiter", "\t")
      .option("header", "false")
      .load(arg2);

    // Load dataframe into point2 view
    pointDf2.createOrReplaceTempView("point2")

    // YOU NEED TO FILL IN THIS USER DEFINED FUNCTION
    spark.udf.register(
      "ST_Within",
      (pointString1: String, pointString2: String, distance: Double) => ((ST_Within(pointString1, pointString2, distance)))
    )

    // Select all pair of points that lie at distance D apart
    val resultDf = spark.sql(
      "select * from point1 p1, point2 p2 where ST_Within(p1._c0, p2._c0, " + arg3 + ")"
    )
    resultDf.show()
    println("count4", resultDf.count())

    return resultDf.count()
  }
}

