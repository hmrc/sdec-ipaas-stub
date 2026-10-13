/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sdecipaasstub.hcp.stubs.repo

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import uk.gov.hmrc.sdecipaasstub.dto.Staff

class StaffDataRepositorySpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with ScalaFutures
    with OptionValues {
  private val jdbcUrl =
    "jdbc:h2:mem:staff_repository_test;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;USER=sa;PASSWORD="

  private def migrateSchema(): Unit =
    Flyway
      .configure()
      .dataSource(jdbcUrl, "sa", "")
      .locations("filesystem:conf/db/migration")
      .load()
      .migrate()

  private val xa: Transactor[IO] =
    Transactor.fromDriverManager[IO](
      driver = "org.h2.Driver",
      url = jdbcUrl,
      logHandler = None
    )

  private val repository = new StaffDataRepository[IO](xa)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    migrateSchema()
  }

  "StaffDataRepository" should {

    "insert a staff member and return the generated ID" in {
      val staff = Staff(
        pid = "PID001",
        name = "John Smith",
        email = "john.smith@test.com",
        srs = "sdec_vat_user"
      )

      val result = repository.insert(staff).unsafeToFuture()
      whenReady(result) { staffId =>
        staffId should be > 0L
      }
    }

    "find a staff member by PID when the staff member exists" in {
      val staff = Staff(
        pid = "PID002",
        name = "Jane Smith",
        email = "jane.smith@test.com",
        srs = "sdec_vat_user"
      )

      val result =
        for
          _      <- repository.insert(staff)
          actual <- repository.findByPID(staff.pid)
        yield actual

      val actualResult = result.unsafeToFuture()

      whenReady(actualResult) { actual =>
        actual                      should not be empty
        actual.map(_.pid)         shouldBe Some("PID002")
        actual.map(_.name)        shouldBe Some("Jane Smith")
        actual.map(_.email).value shouldBe Some("jane.smith@test.com")
      }
    }

    "return None when the PID does not exist" in {
      val result =
        repository.findByPID("UNKNOWN").unsafeRunSync()

      result shouldBe None
    }

    "persist the inserted staff member so it can be retrieved" in {
      val staff = Staff(
        pid = "PID003",
        name = "Alex Brown",
        email = "alex.brown@test.com",
        srs = "sdec_vat_user"
      )

      val futureResult =
        for
          generatedId <- repository.insert(staff)
          actual      <- repository.findByPID(staff.pid)
        yield (generatedId, actual)

      val result = futureResult.unsafeToFuture()
      whenReady(result) { (generatedId, actual) =>
        actual.map(_.id)          shouldBe Some(generatedId)
        actual.map(_.pid)         shouldBe Some(staff.pid)
        actual.map(_.name)        shouldBe Some(staff.name)
        actual.map(_.email).value shouldBe Some(staff.email)
      }
    }
  }
}
