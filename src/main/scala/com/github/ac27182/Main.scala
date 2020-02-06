package com.github.ac27182

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats._
import java.util.concurrent.Future
// import com.github.ac27182.Typeclasses1.Record
import com.github.ac27182.Typeclasses1.Employee
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto._
import eu.timepit.refined.string.Regex
import eu.timepit.refined.boolean.Not

object RefinedTypes {
  // https://github.com/fthomas/refined

  // this is the essence of domain driven design
  // using a strong, expressive type system, to reduce human error and make our code do exactly what we want

  type Age =
    Int Refined Interval.Open[18, 99]

  type PosInt =
    Int Refined Positive

  type EventType =
    String Refined MatchesRegex["DriverEvents220|VehicleEvents114"]

  type PostCode =
    String Refined MatchesRegex["^\\w[a-zA-Z0-9]{5}$"]

  type Name =
    String Refined MatchesRegex["^\\w{1,10}$"]

  val age: PosInt = 5
  val data: EventType = "DriverEvents220"
  val name: Name = "alex"
  val postcode: PostCode = "BL52SW"

}

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

import java.util.{UUID, Date}
import java.time.Instant
object Typeclasses1 {

  case class Employee(name: String, id: Int, manager: Boolean)
  case class IceCream(name: String, numCherries: Int, cone: Boolean)
  case class ThermostatRecord(
      timestamp: Instant,
      temprature: Long,
      userId: UUID
  )

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

  val employees = List(
    Employee("alex", 7, false),
    Employee("huw", 69, false),
    Employee("michal", 7, false)
  )

  val message: String =
    employees.writeCsv

  def run(args: List[String]): IO[ExitCode] =
    program(message)

  def program[A](a: A): IO[ExitCode] =
    for {
      _ <- IO(println(s"\n> program operational\n"))
      _ <- IO(println(a))
    } yield (ExitCode.Success)

}
