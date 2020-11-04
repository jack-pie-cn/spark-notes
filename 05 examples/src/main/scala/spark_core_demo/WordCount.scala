package spark_core_demo
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object WordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setAppName("WordCount")
    val sc = new SparkContext(sparkConf)

    val inputFile = "hdfs://master01:9000/mydata/in/words.txt"
    val file: RDD[String] = sc.textFile(inputFile,2)

    val res: RDD[(Int, String)] = file.flatMap(line => line.split(" "))
      .filter(word => word.nonEmpty)
      .map(word => (word,1))
      .reduceByKey((a,b) => a+b)
      .map(x => (x._2,x._1))
      .sortByKey(ascending = false)  // 按照单词出现的次数，降序排列

    res.saveAsTextFile("hdfs://master01:9000/mydata/out/wc.txt")
    res.foreach(println)
    sc.stop()

  }

}
