package com.cuappdev.backend

import org.apache.spark.sql.{SQLContext, _}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.execution.datasources.hbase._


object sparkML {
  case class Record(col0: Int, col1: Int, col2: Boolean)
  val catalog =
    s"""{
        |"table":{"namespace":"default", "name":"table1"},
        |"rowkey":"key",
        |"columns":{
        |"col0":{"cf":"rowkey", "col":"key", "type":"int"},
        |"col1":{"cf":"cf1", "col":"col1", "type":"int"},
        |"col2":{"cf":"cf2", "col":"col2", "type":"boolean"}
        |}
        |}""".stripMargin

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("HBaseTest")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    def withCatalog(cat: String): DataFrame = {
      sqlContext
        .read
        .options(Map(HBaseTableCatalog.tableCatalog->catalog))
        .format("org.apache.spark.sql.execution.datasources.hbase")
        .load()
    }
    val data = (0 to 100).map(number => Record(number, number, number % 2 == 0))

    sc.parallelize(data).toDF.write.options(
        Map(HBaseTableCatalog.tableCatalog -> catalog, HBaseTableCatalog.newTable -> "3"))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .save()

    val df = withCatalog(catalog)
    df.show
    sc.stop()
  }
}

