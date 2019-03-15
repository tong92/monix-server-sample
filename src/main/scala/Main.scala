import monix.eval.{Callback, Task}
import monix.nio.tcp._

object Main extends App {

  implicit val ctx = monix.execution.Scheduler.Implicits.global

  def response = """HTTP/1.1 200 OK
                                       |Date: Mon, 27 Jul 2009 12:28:53 GMT
                                       |Server: Apache/2.2.14 (Win32)
                                       |Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT
                                       |Content-Length: 88
                                       |Content-Type: text/html
                                       |Connection: Closed
                                       |
                                       |<html>
                                       |<body>
                                       |<h1>Hello, World!</h1>
                                       |</body>
                                       |</html>""".stripMargin

  def forkTask(server: TaskServerSocketChannel): Task[String] = {
    for {
      socket <- server.accept()
      conn <- {
        println("test")
        Task.now(readWriteAsync(socket))
      }
      reader <- {
        println("test2")
        conn.tcpObservable
      }
      writer <- conn.tcpConsumer
      echoedLen <- reader.doOnTerminateEval(_ => conn.stopWriting()).map(_ => response.toArray.map(_.toByte)).take(1).consumeWith(writer)
      _ <- conn.close()
      _ <- forkTask(server)
    } yield {
      ""
    }
  }


  val serverProgramT = for {
    server <- asyncServer(java.net.InetAddress.getByName(null).getHostName, 9002)
    _ <- forkTask(server)
  } yield {
    ""
  }

  serverProgramT.runAsync(new Callback[String] {
    override def onSuccess(value: String): Unit = println(s"Echoed $value bytes.")
    override def onError(ex: Throwable): Unit = println(ex)
  })
}
