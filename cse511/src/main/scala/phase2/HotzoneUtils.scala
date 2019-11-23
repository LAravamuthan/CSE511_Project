package phase2

object HotzoneUtils {

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

}
