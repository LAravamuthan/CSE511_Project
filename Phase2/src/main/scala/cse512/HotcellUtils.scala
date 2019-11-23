package cse512

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

object HotcellUtils {
  val coordinateStep = 0.01

  def CalculateCoordinate(inputString: String, coordinateOffset: Int): Int =
  {
    // Configuration variable:
    // Coordinate step is the size of each cell on x and y
    var result = 0
    coordinateOffset match
    {
      case 0 => result = Math.floor((inputString.split(",")(0).replace("(","").toDouble/coordinateStep)).toInt
      case 1 => result = Math.floor(inputString.split(",")(1).replace(")","").toDouble/coordinateStep).toInt
      // We only consider the data from 2009 to 2012 inclusively, 4 years in total. Week 0 Day 0 is 2009-01-01
      case 2 => {
        val timestamp = HotcellUtils.timestampParser(inputString)
        result = HotcellUtils.dayOfMonth(timestamp) // Assume every month has 31 days
      }
    }
    return result
  }

  def timestampParser (timestampString: String): Timestamp =
  {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val parsedDate = dateFormat.parse(timestampString)
    val timeStamp = new Timestamp(parsedDate.getTime)
    return timeStamp
  }

  def dayOfYear (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_YEAR)
  }

  def dayOfMonth (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_MONTH)
  }

  def squaredValue (x: Int): Double = 
  {
    return (x*x).toDouble
  } 

   def gStatistic(x: Int, y: Int, z: Int, avg:Double, stdDev: Double, n: Int, sumN: Int, numOfCells: Int): Double =
  {
    val num = (sumN.toDouble - (avg*n.toDouble))
    val value1 = ((numOfCells.toDouble*n.toDouble) -(n.toDouble*n.toDouble)).toDouble
    val value2 = (numOfCells.toDouble-1.0).toDouble
    val den = stdDev*math.sqrt((value1/value2)).toDouble
    val result = (num/den).toDouble
    return result
  }

  def Neighbours(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int, inputX: Int, inputY: Int, inputZ: Int): Int = 
  {
    var count = 0;
    
      
    if (inputX == minX || inputX == maxX) {
      count += 1;
    }

    if (inputY == minY || inputY == maxY) {
      count += 1;
    }

    if (inputZ == minZ || inputZ == maxZ) {
      count += 1;
    }

    if (count == 1) {
      // Here only one of the cordinates is at its extremes, hence the point lies on the faces of the cube
      val k = 17
      return k;
     
    } 
    else if (count == 2)
    {
      // Here two of the cordinates are at their extremes, hence the point lies on the edges of the cube
      val k = 11
      return k;
      
    }
    else if (count == 3)
    {
      // the top right most corner of the lat,long,time CUBE
      val k = 7
      return k;
    } 
    else
    {
      //Here the point is inside the body of the cube
       val k = 26
      return k;
      
    }
  }

  // YOU NEED TO CHANGE THIS PART
}
