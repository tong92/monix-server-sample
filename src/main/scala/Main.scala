import monix.eval.{Callback, Task}
import monix.nio.tcp._
import monix.reactive.Observable

object Main extends App {

  implicit val ctx = monix.execution.Scheduler.Implicits.global

  def response =
    """HTTP/1.1 200 OK
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

  def forkTask(server: TaskServerSocketChannel): Task[_] = {
    for {
      socket <- server.accept()
      _ <- setServer(socket)
    } yield {
//      new Thread(() => {
//        forkTask(server).runAsync
//      }).start()
      Task.unit
    }
  }

  def setServer(socket: TaskSocketChannel): Task[_] = {
    for {
      conn <- {
        Task.now(readWriteAsync(socket))
      }
      reader <- {
        conn.tcpObservable
      }
      writer <- conn.tcpConsumer
      _ <- reader
        .doOnTerminateEval(_ => conn.stopWriting())
        .map(_ => response.toArray.map(_.toByte))
        .take(1)
        .consumeWith(writer)
    } yield {
      writer
    }
  }

  val serverProgramT = for {
    server <- asyncServer(java.net.InetAddress.getByName(null).getHostName, 9002)
    finish <- {
     val obs = Observable
        .fromIterable(1 to 10000)
        .mapTask(_ => server.accept())
        .mapParallelUnordered(4) { setServer }


      val futrue = Task {
        obs.doOnTerminateEval(_ => server.close())
          .publish
          .connect()
      }.runAsync
Task.fromFuture(futrue)
    }

//    _ <- forkTask(server)
  } yield {
    finish
  }

//  serverProgramT.runAsync(new Callback[String] {
//    override def onSuccess(value: String): Unit = println(s"Echoed $value bytes.")
//    override def onError(ex: Throwable): Unit = println(ex)
//  })
  serverProgramT.runAsync

//  def runServer(): Unit = serverProgramT.runAsync(new Callback[String] {
//    override def onSuccess(value: String): Unit = {
//      println(s"Echoed $value bytes.")
//      runServer()
//    }
//    override def onError(ex: Throwable): Unit = println(ex)
//  })
//  runServer()
}
