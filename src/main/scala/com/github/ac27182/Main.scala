package com.github.ac27182

import cats.effect.{ExitCode, IO, IOApp}
// import com.github.ac27182.Typeclasses1.Employee
import cats.implicits._
import cats._
import java.util.concurrent.Future
import com.github.ac27182.Typeclasses1.Record
import com.github.ac27182.Typeclasses1.Employee

object Typeclasses2 {
  case class Employee(name: String, id: Int, manager: Boolean)
  case class IceCream(name: String, numCherries: Int, cone: Boolean)

  trait CsvEncoder[A] {
    def encode(value: A): List[String]
  }

  object CsvEncoder {

    object instanes {
      implicit val employeeEncoder: CsvEncoder[Employee] = employee =>
        List(
          employee.name,
          employee.manager.toString,
          if (employee.manager) "y" else "n"
        )
    }

    object ops {
      implicit class CsvEncoderOps[A](values: List[A]) {
        def writeCsv(implicit encoder: CsvEncoder[A]): String =
          values
            .map(value => encoder.encode(value).mkString(","))
            .mkString("\n")
      }

    }

    def apply[A](implicit enc: CsvEncoder[A]): CsvEncoder[A] = enc
  }

}

object Typeclasses1 {

  case class Employee(name: String, id: Int, manager: Boolean)
  case class IceCream(name: String, numCherries: Int, cone: Boolean)
  case class Calendar(owner: Employee, events: List[Record])
  case class Record(day: String, month: String, notes: String)

  // type classes consist of three components
  // - the type class, which takes at least one generic parameter
  // - instances of the type class we want to extend
  // - interface methods to expose to users of your api

  // this is the typeclass, a small algebra describing what we want
  trait CsvEncoder[A] {
    def encode(value: A): List[String]
  }

  // this object contains a series of instances for which we want to encode
  object CsvEncoderInstances {
    implicit val employeeEncoder: CsvEncoder[Employee] =
      new CsvEncoder[Employee] {
        def encode(e: Employee): List[String] =
          List(e.name, e.manager.toString, if (e.manager) "y" else "n")
      }
  }

  // the interface methods / implicit interface methods used to expose our api to the end user
  object CsvEncoderSyntax {

    // when we define our interface methods as an implicit class, we 'latch' on to all values with a typeclass instance
    implicit class CsvEncoderOps[A](values: List[A]) {
      def writeCsv(implicit encoder: CsvEncoder[A]): String =
        values
          .map(value => encoder.encode(value).mkString(","))
          .mkString("\n")
    }

    // more generally, we can just apply a normal function to our value
    // implicit def writeCsv[A](
    //     values: List[A]
    // )(implicit encoder: CsvEncoder[A]): String =
    //   values
    //     .map(value => encoder encode value mkString (","))
    //     .mkString("\n")
  }

}
object Main extends IOApp {

  import Typeclasses1.CsvEncoderInstances._
  import Typeclasses1.CsvEncoderSyntax.{CsvEncoderOps}
  // import Typeclasses2._
  val employees = List(
    Employee("alex", 7, false),
    Employee("huw", 69, false),
    Employee("michal", 7, false)
  )

  val message: String =
    employees.writeCsv

  // val message1: String =
  //   writeCsv(employees)

  def run(args: List[String]): IO[ExitCode] =
    program(message)

  def program[A](a: A): IO[ExitCode] =
    for {
      _ <- IO(println(s"\n> program operational\n"))
      _ <- IO(println(a))
    } yield (ExitCode.Success)

}
