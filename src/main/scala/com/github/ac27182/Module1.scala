package com.github.ac27182

import cats.effect.IO
import cats._
import cats.implicits._
import com.github.ac27182.TaglessFinal2.Domain
import java.nio.file.{Files, Path, Paths}
import cats.effect.Resource
import scala.io.Source
import io.circe.Encoder
import io.circe.Decoder
import io.circe.generic.auto._
import cats.effect.Sync
import org.http4s.dsl.Http4sDsl
import com.github.ac27182.TaglessFinal2.Domain.UserRepository
import com.github.ac27182.TaglessFinal2.Domain.BookRepository
import org.http4s.HttpRoutes
import com.github.ac27182.TaglessFinal2.Domain.Book
import com.github.ac27182.TaglessFinal2.Domain.User
import com.github.ac27182.TaglessFinal2.Domain.UserId
import cats.effect.ContextShift
import com.github.ac27182.TaglessFinal2.Domain.FileService
import org.http4s.HttpApp
import cats.effect.IOApp
import cats.effect.ExitCode
import org.http4s.server.blaze.BlazeServerBuilder
import scala.io.StdIn

object TagglessFinal0 extends App {

  // defining the algebra
  trait Algebra[F[_]] {
    def getN(n: String): F[String]
    def getD(m: String): F[String]
  }

  // define an interpreter for the algebra
  class InterpreterIO extends Algebra[IO] {
    def getN(n: String): IO[String] = IO.pure(s"build $n has build number 69")
    def getD(m: String): IO[String] = IO.pure(s"build $m created on 01/01/2020")
  }

  // define another interpreter for the algebra
  class InterpreterList extends Algebra[List] {
    def getN(n: String): List[String] = List(s"build $n", "build number 420")
    def getD(m: String): List[String] =
      List(s"build $m", "build date 1997/07/07")
  }

  // defining an algebra bridge / (compiler bridge)
  trait AlgebraBridge[B] {
    def apply[F[_]](implicit A: Algebra[F]): F[B]
  }

  // build complex expressions from your algebra ...
  def getBuildNumber(build: String) = new AlgebraBridge[String] {
    def apply[F[_]](implicit A: Algebra[F]): F[String] = A.getN(build)
  }

  def getBuildDate(build: String) = new AlgebraBridge[String] {
    def apply[F[_]](implicit A: Algebra[F]): F[String] = A.getD(build)
  }

  val interpreter0 = new InterpreterIO
  val interpreter1 = new InterpreterList

  println(getBuildNumber("zzzz").apply(interpreter0).unsafeRunSync)
  println(getBuildNumber("zzzz").apply(interpreter1))
}

object TagglessFinal1 extends App {

  // bringing in our original algebra
  import TagglessFinal0.{Algebra, getBuildDate, getBuildNumber}

  // adding an extra axiom to our algebra
  trait ExtendedAlgebra[F[_]] extends Algebra[F] {
    def getM(m: String): F[String]
  }

  // creating a new bridge to apply this
  trait ExtendedAlgebraBridge[B] {
    def apply[F[_]](implicit B: ExtendedAlgebra[F]): F[B]
  }

  // createing complex expressions which include getM
  def getManufacturerLocation(build: String) =
    new ExtendedAlgebraBridge[String] {
      def apply[F[_]](implicit B: ExtendedAlgebra[F]): F[String] = B.getM(build)
    }

  // create new interpreter for extended algebra
  class InterpreterIO1 extends ExtendedAlgebra[IO] {
    def getN(n: String): IO[String] = ???
    def getD(m: String): IO[String] = ???
    def getM(m: String): IO[String] = ???
  }
  val interpreter2 = new InterpreterIO1

  println(getBuildNumber("zzzz").apply(interpreter2).unsafeRunSync)
  println(getBuildDate("zzzz").apply(interpreter2).unsafeRunSync)
  println(getManufacturerLocation("zzzz").apply(interpreter2).unsafeRunSync)
  // upon inspection the tagless final encoding pattern has 4 distinct parts
  // 1. the definition of an Algebra inside a trait
  // 2. the definition of a bridge trait, which compiles our 'complex expressions' in to the core algebras
  // 3. the definition of complex expressions, made up of core algebras
  // 4. the definition of n numbers of interpreters, to implement the algebra

  // NB try doing it without the compiler bridge?

}

object TaglessFinal2 extends App {

  // here we are defining the domain model for the reading list application
  // this is not a perfect model, however it is simple and demonstrative
  // this application allows users to add and remove books from their reading list

  object Domain {
    // describes a user id
    final case class UserId(
        id: String
    ) extends AnyVal

    // describes a book id
    final case class BookId(
        id: String
    ) extends AnyVal

    // describes a reading list assigned to a user
    final case class ReadingList(
        user: User,
        books: List[Book]
    )

    // describes a user
    final case class User(
        id: Option[UserId],
        firstName: String,
        lastName: String,
        books: List[BookId]
    )

    // decribes a book
    final case class Book(
        id: Option[BookId],
        title: String,
        author: String
    )

    // userrepository defines an algebra for manipulating users
    trait UserRepository[F[_]] {
      def getUser(id: UserId): F[Option[User]]
      def addUser(user: User): F[Unit]
      def updateUser(user: User): F[Unit]
    }

    // book repository defines an algebra for manipulating books
    trait BookRepository[F[_]] {
      def listBooks(): F[List[Book]]
      def getBook(id: BookId): F[Option[Book]]
      def addBook(id: Book): F[Unit]
    }

    // another way to think of the previous two algebras,
    // is as a low level DSL

    // reading list service defines the core domain of operations in our application
    // which will be combinations of the core algebreas we have defined above
    trait ReadingListService[F[_]] {
      def getReadingList(user: UserId): F[ReadingList]
      def addToReadingList(userId: UserId, bookId: BookId): F[Unit]
      def removeFromReadingList(userId: UserId, bookId: BookId): F[Unit]
    }

    // file service abstracts over reading our loacal filesystem
    trait FileService[F[_]] {
      def exists(path: Path): F[Boolean]
      def listDir(path: Path): F[List[Path]]
      def getFileData(path: Path): F[String]
      def writeFileData(path: Path, data: String): F[Unit]
    }

    // another wat to think of the the previious two services
    // is a high level DSL
  }

  object Interpreters {
    import Domain._
    import cats.implicits._
    import cats.Parallel._
    import cats.MonadError

    type Throwing[F[_]] = MonadError[F, Throwable]
    def Throwing[F[_]](implicit t: Throwing[F]): Throwing[F] = t

    // describes an exception when no user can be found
    case class NoSuchUserException(id: UserId) extends Exception

    // defines an implementation of ReadingListService in terms of user repository and book repository
    class ReadingListServiceInterpreter[F[_]: Throwing: Parallel](
        users: UserRepository[F],
        books: BookRepository[F]
    ) extends ReadingListService[F] {

      def getReadingList(userId: UserId): F[ReadingList] =
        for {
          userOpt <- users.getUser(userId)
          list <- userOpt match {
            case Some(user: User) =>
              user.books
                .traverse(bookId => books.getBook(bookId))
                .map(books => ReadingList(user, books.flatten))
            case None =>
              Throwing[F].raiseError[ReadingList](NoSuchUserException(userId))
          }
        } yield (list)

      def addToReadingList(
          userId: UserId,
          bookId: BookId
      ): F[Unit] =
        for {
          list <- getReadingList(userId)
          books = bookId :: list.user.books
          _ <- users.updateUser(list.user.copy(books = books))
        } yield ()

      def removeFromReadingList(
          userId: UserId,
          bookId: BookId
      ): F[Unit] =
        for {
          listCurrent <- getReadingList(userId)
          listNew = listCurrent.user.books.filter(_ != bookId)
          _ <- users.updateUser(listCurrent.user.copy(books = listNew))
        } yield ()
    }
  }

  object ImplementationFs {
    import Domain._
    import Interpreters._

    // implements the file service in a purely functional way, using the IO monad to suspend out side effects
    class FileServiceIO extends FileService[IO] {
      def exists(path: Path): IO[Boolean] =
        IO.delay { path.toFile.exists }
      def listDir(path: Path) =
        IO { path.toFile.listFiles.toList.map(_.toPath) }
      def getFileData(path: Path): IO[String] = {
        val aquire =
          IO.delay { Source.fromFile(path.toFile) }
        val use: Source => IO[String] =
          source => IO.delay { source.getLines.mkString }
        Resource
          .fromAutoCloseable(aquire)
          .use(use)
      }
      def writeFileData(path: Path, data: String): IO[Unit] =
        IO.delay {
          if (!path.getParent.toFile.exists) path.getParent.toFile.mkdirs
          Files.write(path, data.getBytes)
        }

    }

    // complex expressions defined in terms of our algebras and or services
    def save[F[_]: Monad, T: Encoder](files: FileService[F])(
        path: Path,
        data: T
    ): F[Unit] = {
      val encoded = Encoder[T].apply(data).toString
      files
        .writeFileData(path, encoded)
    }

    def read[F[_]: Throwing, T: Decoder](
        files: FileService[F]
    )(path: Path): F[Option[T]] = {
      for {
        exists <- files.exists(path)
        result <- if (exists) {
          files
            .getFileData(path)
            .flatMap(string => io.circe.parser.parse(string).liftTo[F])
            .flatMap(json => Decoder[T].decodeJson(json).liftTo[F])
            .map(_.some)
        } else {
          none[T].pure[F]
        }
      } yield result
    }

    // an implementation of the UserRepository, backed by our local filesystem
    class FileBackedUserRepository[F[_]: Throwing](
        root: Path,
        files: FileService[F]
    ) extends UserRepository[F] {

      def getUser(id: UserId): F[Option[User]] =
        read(files)(pathFor(id))

      def addUser(user: User): F[Unit] =
        save(files)(pathFor(user.id.get), user)

      def updateUser(user: User): F[Unit] =
        save(files)(pathFor(user.id.get), user)

      private def pathFor(key: UserId): Path =
        Paths.get(root.toString, key.id)

    }

    import java.util.UUID

    // an implementation of the bookRepository backed by our local file system
    class FileBackedBookRepository[F[_]: Throwing](
        root: Path,
        files: FileService[F]
    ) extends BookRepository[F] {
      def listBooks(): F[List[Book]] =
        for {
          bookFiles <- files.listDir(root)
          values    <- bookFiles.traverse(read[F, Book](files))
        } yield values.flatten

      def getBook(id: BookId): F[Option[Book]] =
        read(files)(pathFor(id))

      def addBook(book: Book): F[Unit] = {
        val id = book.id.getOrElse(BookId(UUID.randomUUID().toString))
        save(files)(pathFor(id), book.copy(id = Some(id)))
      }

      private def pathFor(key: BookId): Path =
        Paths.get(root.toString, key.id)

    }
  }

  object ImplementationHttp {
    import Interpreters._
    import io.circe.generic.auto._
    import io.circe.syntax._
    import org.http4s.circe._
    import ImplementationFs._

    trait Module {
      def dataDir: Path
      implicit def contextShift: ContextShift[IO]
      lazy val fileService: FileService[IO]                 = ???
      lazy val bookRepository: FileBackedBookRepository[IO] = ???
      lazy val userRepository: FileBackedUserRepository[IO] = ???
      lazy val readlingListServiceCompiler: ReadingListServiceInterpreter[IO] =
        ???
      lazy val readingListHttpService: ReadingListHttpService[IO] = ???
      lazy val httpApp: HttpApp[IO]                               = ???
    }

    object Server extends IOApp {

      val module =
        new Module {
          def dataDir: Path                  = ???
          def contextShift: ContextShift[IO] = ???
        }

      val fiber: IO[Either[Throwable, ExitCode]] =
        BlazeServerBuilder[IO]
          .bindHttp(8080, "localhost")
          .withHttpApp(module.httpApp)
          .resource
          .use(_ => IO(StdIn.readLine))
          .as(ExitCode.Success)
          .attempt

      def run(args: List[String]): IO[ExitCode] =
        fiber.unsafeRunSync match {
          case Left(error) =>
            IO {
              println("> program terminated")
              println(error.getMessage)
              ExitCode.Error
            }
          case Right(value) =>
            IO {
              println("program safely shutting down")
              value
            }
        }
    }

    // here we define the http endpoints avilible in the rest api, for the reading list application
    class ReadingListHttpService[F[_]: Sync](
        userRepository: UserRepository[F],
        bookRepository: BookRepository[F],
        readingListService: ReadingListServiceInterpreter[F]
    ) extends Http4sDsl[F] {
      val service: HttpRoutes[F] =
        HttpRoutes.of[F] {
          case GET -> Root / "book" =>
            for {
              books    <- bookRepository.listBooks
              response <- Ok(books.asJson)
            } yield response

          case req @ POST -> Root / "book" =>
            for {
              book     <- req.decodeJson[Book]
              _        <- bookRepository.addBook(book)
              response <- Ok(book.asJson)
            } yield response

          case req @ POST -> Root / "user" =>
            for {
              user     <- req.decodeJson[User]
              _        <- userRepository.addUser(user)
              response <- Ok(user.asJson)
            } yield response

          case GET -> Root / "reading-list" / userId =>
            for {
              list     <- readingListService.getReadingList(UserId(userId))
              response <- Ok(list.asJson)
            } yield response
        }
    }

  }

  // in summary the tagless final can be reduced in to three key parts:
  // 1. abstractly defined algebras and services
  // 2. refined interpreters build on top of the algebras and services
  // 3. implementations for our interpreters

}
